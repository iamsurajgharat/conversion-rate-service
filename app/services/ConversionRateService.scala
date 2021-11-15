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
import zio.IO
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
    val defaultStartDate = new DateTime(1, 1, 1, 0, 0)
    val defaultEndDate = new DateTime(4000, 12, 31, 0, 0)
    def saveRates(rates: List[ConversionRate]): Task[List[SavedConversionRate]] = {
        // find overlaps if any
        val savedRatesEffect = for{
            oldSavedRates <- Repository.getRatesByTarget(rates.map(_.target).toSet)
            validatedRates <- validateRate(rates, oldSavedRates)
            savedRates <- Repository.saveRates(validatedRates)
        } yield savedRates
        
        savedRatesEffect.provideLayer(repoLayer)
    }
    
    def getAllRates(): Task[List[SavedConversionRate]] = 
        Repository.getAllRates().provideLayer(repoLayer)

    private def validateRate(rates: List[ConversionRate], 
        savedRates:Seq[SavedConversionRate]):IO[ValidationException, List[SavedConversionRate]] = {
        val savedRatesByTarget = savedRates.groupBy(_.target)
        val validInvalidRates = rates.map(_.toSavedRate(defaultStartDate, defaultEndDate))
            .zipWithIndex
            .partition(newRate => {
            savedRatesByTarget.get(newRate._1.target).getOrElse(Nil).exists(_.overlap(newRate._1))
        })
        if(validInvalidRates._1.isEmpty){
            val validRates = validInvalidRates._2.map(_._1)
            ZIO.succeed(validRates)
        }
        else{
            val invalidIndices = validInvalidRates._1.map(_._2)
            val message = s"Overlapping rates [${invalidIndices.mkString(",")}]"
            ZIO.fail(new ValidationException(message))
        }
    }
        
}

case class ValidationException(message:String) extends Exception(message)