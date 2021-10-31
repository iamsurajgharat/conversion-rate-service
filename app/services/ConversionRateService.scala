package com.surajgharat.conversionrates.services
import org.joda.time.DateTime
import com.surajgharat.conversionrates.models._
import com.surajgharat.conversionrates.repositories.Repository
import com.surajgharat.conversionrates.repositories.Repository._
import zio.Task
import com.google.inject.Inject
import com.google.inject.ImplementedBy
import com.google.inject

@ImplementedBy(classOf[ConversionRateService])
trait ConversionRateServiceSpec{
    def saveRates(rates:List[ConversionRate]): Task[List[SavedConversionRate]]
    def getAllRates(): Task[List[SavedConversionRate]]
}

@inject.Singleton
class ConversionRateService @Inject() (private val repository: Repository) extends ConversionRateServiceSpec{
    def saveRates(rates: List[ConversionRate]): Task[List[SavedConversionRate]] = {
        Repository.saveRates(rates).provide(repository.rateRepository)
    }
        
    

    def getAllRates(): Task[List[SavedConversionRate]] = 
        Repository.getAllRates().provide(repository.rateRepository)
}