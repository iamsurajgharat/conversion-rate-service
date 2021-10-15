package com.surajgharat.conversionrates.services
import org.joda.time.DateTime

trait ConversionRateServiceSpec{
    def saveRates(rates:List[ConversionRate]):Unit
}

case class ConversionRate(source: String,
    target: String,
    startDate: DateTime,
    endDate: DateTime,
    rate: Float)

class ConversionRateService extends ConversionRateServiceSpec{
    def saveRates(rates: List[ConversionRate]): Unit = ???
}