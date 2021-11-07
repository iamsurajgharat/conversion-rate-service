package com.surajgharat.conversionrates.controllers
package tests

import akka.http.scaladsl.model.HttpHeader
import akka.stream.Materializer
import com.surajgharat.conversionrates.models.ConversionRate
import com.surajgharat.conversionrates.repositories.Repository
import com.surajgharat.conversionrates.services.ConversionRateService
import org.joda.time.DateTime
import org.mockito.ArgumentMatchersSugar
import org.mockito.MockSettings
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.JsArray
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.Headers
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers
import zio.ZIO

class ConversionRateControllerSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar{

    import Helpers._
    import Repository._
    "getAllRates" must{
        val rateServiceMock = mock[ConversionRateService]
        val subject = new ConversionRateController(rateServiceMock, Helpers.stubControllerComponents())
        "return list of saved rates" in {
            // mock result data
            when(rateServiceMock.getAllRates()).
                thenReturn(
                    ZIO.succeed(List(getSampleSavedRate()))
                )

            // act
            val result = subject.getAllRates().apply(FakeRequest())

            // assure
            status(result) mustBe OK
            val jsValue = Json.parse(contentAsString(result))
            jsValue.validate[List[SavedConversionRate]] match {
                case JsSuccess(data, _) =>
                    data must have size 1
                case _:JsError => 
                    fail("Invalid json in result ")
            }
            
        }

        "return internalServerError if service fails" in {
            // mock failure
            when(rateServiceMock.getAllRates()).
                thenReturn(
                    ZIO.fail(new Exception("unreachable database"))
                )
            
            // act
            val result = subject.getAllRates().apply(FakeRequest())

            // assure
            status(result) mustBe INTERNAL_SERVER_ERROR
            contentAsString(result) mustBe "unreachable database"
        }
    }

    "saveRates" must{
        val rateServiceMock = mock[ConversionRateService]
        val subject = new ConversionRateController(rateServiceMock, Helpers.stubControllerComponents())

        "return savedRates for provided rates" in {
            // arrange
            val rates = List(getSampleRate())
            var json = Json.toJson(rates)
            val request = FakeRequest(GET, "/rates/save", Headers.create(), json)

            // mock result data
            when(rateServiceMock.saveRates(any[List[ConversionRate]])).
                thenReturn(
                    ZIO.succeed(List(SavedConversionRate(1, rates.head)))
                )

            // act
            val result = subject.saveRates().apply(request)

            // assure
            status(result) mustBe OK
            val jsValue = Json.parse(contentAsString(result))
            jsValue.validate[List[SavedConversionRate]] match {
                case JsSuccess(data, _) =>
                    data.head.rate.source mustBe rates.head.source
                case _:JsError => 
                    fail("Invalid json in result ")
            }

            // reset mock
            reset(rateServiceMock)
        }

        "return bad-request for incorrect input" in {
            // arrange
            // in place of list of rates, we would pass one rate in request
            // which would fail in request parsing since it expects a list not an object
            val rate = getSampleRate()
            var json = Json.toJson(rate)
            val request = FakeRequest(GET, "/rates/save", Headers.create(), json)

            // act
            val result = subject.saveRates().apply(request)

            // assure
            status(result) mustBe BAD_REQUEST
            verify(rateServiceMock, times(0)).saveRates(any[List[ConversionRate]])

            // reset mock
            reset(rateServiceMock)
        }

        "return internalServerError when service fals" in {
            // arrange
            val rates = List(getSampleRate())
            var json = Json.toJson(rates)
            val request = FakeRequest(GET, "/rates/save", Headers.create(), json)

            // mock result data
            when(rateServiceMock.saveRates(any[List[ConversionRate]])).
                thenReturn(
                    ZIO.fail(new Exception("database is busy"))
                )

            // act
            val result = subject.saveRates().apply(request)

            // assure
            status(result) mustBe INTERNAL_SERVER_ERROR
            contentAsString(result) mustBe "database is busy"

            // reset mock
            reset(rateServiceMock)
        }
    }

    private def getSampleSavedRate() : SavedConversionRate = {
        SavedConversionRate(1, getSampleRate())
    }

    private def getSampleRate() : ConversionRate = {
        ConversionRate("Kg", "Gm", new DateTime(), new DateTime(), 1000, None)
    }
}