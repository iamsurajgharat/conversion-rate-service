package com.surajgharat.conversionrates.services
package tests

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.mockito.MockitoSugar
import play.api.test.Helpers

class ConversionRateServiceSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar{
    import Helpers._
    "saveRates" must {
        "pass valid input to database layer" in {
            
        }
    }
}