package com.surajgharat.conversionrates.services
package tests

import com.surajgharat.conversionrates.helpers._
import com.surajgharat.conversionrates.repositories.Repository
import com.surajgharat.conversionrates.services.ConversionRateService
import org.joda.time.DateTime
import org.mockito.ArgumentMatchersSugar
import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers
import zio.ZIO
import com.surajgharat.conversionrates.models.ConversionRate
import com.surajgharat.conversionrates.models.ConversionRateRequest

class ConversionRateServiceImplSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar{
    import Helpers._
    import ZIOHelper._
    import Repository._
    val rateRepositoryMock = mock[Repository]
    val subject = new ConversionRateServiceImpl(rateRepositoryMock)
    "getAllRates" must {
        "return an effect that gives all rates as provided by repository" in {
            // setup mock results
            reset(rateRepositoryMock)
            when(rateRepositoryMock.getAllRates()).thenReturn(ZIO.succeed(List(getSampleSavedRate())))

            // act
            val result = interpret{
                subject.getAllRates()
            }

            // assure
            result must have length 1
            verify(rateRepositoryMock).getAllRates()
        }

        "return an effect that fails as repository faild to fetch" in {
            // setup mock results
            reset(rateRepositoryMock)
            when(rateRepositoryMock.getAllRates()).thenReturn(ZIO.fail(new Exception("Database is unreachable")))

            // act and assure
            an [Exception] must be thrownBy interpret(subject.getAllRates())
        }
    }

    "saveRates" must {
        "return an effect that passes given rate to repo" in {
            // arrange 
            val inputRates = List(getSampleRate())

            // setup mock results
            reset(rateRepositoryMock)
            when(rateRepositoryMock.getRatesByTarget(argThat[Set[String]](_ => true))).thenReturn(ZIO.succeed(Seq.empty[Repository.SavedConversionRate]))
            when(rateRepositoryMock.saveRates(argThat[List[SavedConversionRate]](x => x.length == 1 && x.head.source == inputRates(0).source)))
                .thenAnswer(ZIO.succeed(List(getSampleSavedRate())))

            // act
            val effect = subject.saveRates(inputRates)
            val result = interpret(effect)

            // assure
            result must have size 1
            verify(rateRepositoryMock, times(1)).getRatesByTarget(Set(inputRates.head.target))
            verify(rateRepositoryMock, times(1)).saveRates(argThat[List[SavedConversionRate]](x => x.length == 1 && x.head.source == inputRates(0).source))
        }

        "return an effect that fails due to overlapping rates" in {
            // arrange 
            val inputRates = List(getSampleRate())
            val savedRates = Seq(getSampleSavedRate())

            // setup mock results
            reset(rateRepositoryMock)
            when(rateRepositoryMock.getRatesByTarget(argThat[Set[String]](_ => true))).thenReturn(ZIO.succeed(savedRates))
            when(rateRepositoryMock.saveRates(argThat[List[SavedConversionRate]](x => x.length == 1 && x.head.source == inputRates(0).source)))
                .thenAnswer(ZIO.succeed(List(getSampleSavedRate())))

            // act
            val effect = subject.saveRates(inputRates)
            val thrown = the [ValidationException] thrownBy interpret(effect)

            // assure
            thrown.message must include("Overlapping rates")
            verify(rateRepositoryMock, times(1)).getRatesByTarget(Set(inputRates.head.target))
            verify(rateRepositoryMock, times(0)).saveRates(argThat[List[SavedConversionRate]](x => true))
        }
    }

    "getRates" must {
        "return correct direct rate" in {
            // arrange
            val input = List(ConversionRateRequest("USD", "INR", Some(new DateTime(2021,1,15,0,0,0))))

            reset(rateRepositoryMock)
            when(rateRepositoryMock.getRatesByTarget(argThat[Set[String]](_ => true)))
                .thenReturn(
                    ZIO.succeed(
                        Seq(
                            SavedConversionRate(Some(1), "USD", "INR", new DateTime(2021,1,1,0,0,0), new DateTime(2021,1,31,0,0,0), 75)
                        )
                    )
                )

            // act
            val effect = subject.getRates((input))
            val result = interpret(effect)

            // assure
            result.head.rate mustBe 75

        }

        "return correct direct rate for given date" in {
            // arrange
            val input = List(ConversionRateRequest("USD", "INR", Some(new DateTime(2021,1,15,0,0,0))))

            reset(rateRepositoryMock)
            when(rateRepositoryMock.getRatesByTarget(argThat[Set[String]](_ => true)))
                .thenReturn(
                    ZIO.succeed(
                        Seq(
                            SavedConversionRate(Some(1), "USD", "INR", new DateTime(2021,1,1,0,0,0), new DateTime(2021,1,31,0,0,0), 75),
                            SavedConversionRate(Some(1), "USD", "INR", new DateTime(2021,2,1,0,0,0), new DateTime(2021,2,28,0,0,0), 80)
                        )
                    )
                )

            // act
            val effect = subject.getRates((input))
            val result = interpret(effect)

            // assure
            result.head.rate mustBe 75

        }

        "return undefined value when rate not configured" in {
            // arrange
            val input = List(ConversionRateRequest("USD", "INR", Some(new DateTime(2021,1,15,0,0,0))))

            reset(rateRepositoryMock)
            when(rateRepositoryMock.getRatesByTarget(argThat[Set[String]](_ => true)))
                .thenReturn(
                    ZIO.succeed(
                        Seq(
                        )
                    )
                )

            // act
            val effect = subject.getRates((input))
            val result = interpret(effect)

            // assure
            result.head.rate mustBe ConversionRateService.ValueForUndefinedRate

        }

        "return correct indirect rate" in {
            // arrange
            val input = List(
                ConversionRateRequest("EUR", "INR", Some(new DateTime(2021,1,15,0,0,0)))
            )

            reset(rateRepositoryMock)
            when(rateRepositoryMock.getRatesByTarget(argThat[Set[String]](_ => true)))
                .thenReturn(
                    ZIO.succeed(
                        Seq(
                            SavedConversionRate(Some(1), "USD", "INR", new DateTime(2021,1,1,0,0,0), new DateTime(2021,1,31,0,0,0), 75),
                            SavedConversionRate(Some(1), "USD", "EUR", new DateTime(2021,1,1,0,0,0), new DateTime(2021,1,31,0,0,0), 0.5f)
                        )
                    )
                )

            // act
            val effect = subject.getRates((input))
            val result = interpret(effect)

            // assure
            result.head.rate mustBe 150f
        }

        "return correct indirect rate for given date" in {
            // arrange
            val input = List(
                ConversionRateRequest("EUR", "INR", Some(new DateTime(2021,2,15,0,0,0)))
            )

            reset(rateRepositoryMock)
            when(rateRepositoryMock.getRatesByTarget(argThat[Set[String]](_ => true)))
                .thenReturn(
                    ZIO.succeed(
                        Seq(
                            SavedConversionRate(Some(1), "USD", "INR", new DateTime(2021,1,1,0,0,0), new DateTime(2021,1,31,0,0,0), 75),
                            SavedConversionRate(Some(1), "USD", "EUR", new DateTime(2021,1,1,0,0,0), new DateTime(2021,1,31,0,0,0), 0.5f),
                            SavedConversionRate(Some(1), "USD", "INR", new DateTime(2021,2,1,0,0,0), new DateTime(2021,2,28,0,0,0), 80),
                            SavedConversionRate(Some(1), "USD", "EUR", new DateTime(2021,2,1,0,0,0), new DateTime(2021,2,28,0,0,0), 0.2f)
                        )
                    )
                )

            // act
            val effect = subject.getRates((input))
            val result = interpret(effect)

            // assure
            result.head.rate mustBe 400f
        }
    }

    private def getSampleSavedRate() : Repository.SavedConversionRate = {
        Repository.SavedConversionRate(Some(1), "Kg", "Gm", ConversionRateService.defaultStartDate, ConversionRateService.defaultEndDate, 1000)
    }

    private def getSampleRate() : ConversionRate = 
        ConversionRate("Kg", "Gm", None, None, 1000, None)
    
}