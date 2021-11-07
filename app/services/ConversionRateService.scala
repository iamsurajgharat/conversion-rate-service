package com.surajgharat.conversionrates.services
import org.joda.time.DateTime
import com.surajgharat.conversionrates.models._
import com.surajgharat.conversionrates.repositories._
import Repository._
import zio.Task
import com.google.inject.Inject
import com.google.inject.ImplementedBy
import com.google.inject
import zio.ZLayer
import zio.ULayer
import zio.Has

@ImplementedBy(classOf[ConversionRateServiceImpl])
trait ConversionRateService{
    def saveRates(rates:List[ConversionRate]): Task[List[SavedConversionRate]]
    def getAllRates(): Task[List[SavedConversionRate]]
}

@inject.Singleton
class ConversionRateServiceImpl @Inject() (repository:Repository) extends ConversionRateService{
    val repoLayer: ULayer[Has[Repository]] = ZLayer.succeed(repository)
    def saveRates(rates: List[ConversionRate]): Task[List[SavedConversionRate]] = 
        Repository.saveRates(rates).provideLayer(repoLayer)
    

    def getAllRates(): Task[List[SavedConversionRate]] = 
        Repository.getAllRates().provideLayer(repoLayer)
}