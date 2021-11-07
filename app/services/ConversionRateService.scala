package com.surajgharat.conversionrates.services
import com.google.inject
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.surajgharat.conversionrates.models._
import com.surajgharat.conversionrates.repositories._
import org.joda.time.DateTime
import zio.Has
import zio.RIO
import zio.Task
import zio.ULayer
import zio.ZIO
import zio.ZLayer

import Repository._

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