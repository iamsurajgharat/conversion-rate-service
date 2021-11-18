package com.surajgharat.conversionrates.services
package tests

import org.mockito.MockitoSugar
import play.api.test.Helpers
import com.surajgharat.conversionrates.repositories.Repository
import com.surajgharat.conversionrates.services.ConversionRateService
import zio.ZIO
import zio.test.DefaultRunnableSpec
import zio.test.Assertion._
import zio.test._
import zio.test.environment._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.mockito.ArgumentMatchersSugar
import org.scalatestplus.play.PlaySpec
import org.joda.time.DateTime
import com.surajgharat.conversionrates.helpers._

object ConversionRateServiceImplSpec extends DefaultRunnableSpec with MockitoSugar{
    import Helpers._
    val rateRepositoryMock = mock[Repository]
    val subject = new ConversionRateServiceImpl(rateRepositoryMock)
    
    def spec = suite("ConversionRateServiceImpl"){
        suite("getAllRates"){
            test("should return all rates"){
                for{
                    result <- ZIO.succeed("Yess")
                } yield assert(result)(equalTo("Yess"))
            }
        }
    }
}

class ConversionRateServiceImplSpec2 extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar{
    import Helpers._
    import ZIOHelper._
    val rateRepositoryMock = mock[Repository]
    val subject = new ConversionRateServiceImpl(rateRepositoryMock)
    "getAllRates" must {
        "return effect that gives all rates as provided by repository" in {
            // setup mock results
            when(rateRepositoryMock.getAllRates()).thenReturn(ZIO.succeed(List(getSampleSavedRate())))

            // act
            val result = interpret{
                subject.getAllRates()
            }

            // assure
            result must have length 1
            verify(rateRepositoryMock).getAllRates()
        }

        "return effect that fails as repository faild to fetch" in {
            // setup mock results
            when(rateRepositoryMock.getAllRates()).thenReturn(ZIO.fail(new Exception("Database is unreachable")))

            // act
            //interpret(subject.getAllRates()) must throwA("Database is unreachable")
            //(10 / 2) must throwA[Exception]

            // assure
            // result must have length 1
            // verify(rateRepositoryMock).getAllRates()
        }
    }

    private def getSampleSavedRate() : Repository.SavedConversionRate = {
        Repository.SavedConversionRate(Some(1), "Kg", "Gm", new DateTime(), new DateTime(), 1000)
    }
}