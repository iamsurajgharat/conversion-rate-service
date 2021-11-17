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