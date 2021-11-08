package com.surajgharat.conversionrates
package repositories

import com.surajgharat.conversionrates.models.ConversionRate
import zio.Task
import zio.ZIO
import zio.RIO
import zio.ULayer
import play.api.libs.json.Json
import com.google.inject.ImplementedBy
import com.google.inject
import zio.Has
import zio.ZLayer
import play.api.db.Database
import javax.inject.Inject
import com.surajgharat.conversionrates.repositories.rateRepositoryLive._
import org.joda.time.DateTime

@ImplementedBy(classOf[SlickRateRepository])
trait Repository {
    import Repository._
    def getAllRates():Task[List[SavedConversionRate]]
    def getRates(source: Set[String], target:Set[String]): Task[List[SavedConversionRate]]
    def getRatesBySource(source:String):Task[List[SavedConversionRate]]
    def getRatesByTarget(target:String):Task[List[SavedConversionRate]]
    def saveRates(rates:List[ConversionRate]):Task[List[SavedConversionRate]]
    def deleteRates(ids:Set[Int]):Task[Unit]
}

object Repository{
    import com.surajgharat.conversionrates.models.ConversionRate._
    case class SavedConversionRate(
        id:Option[Int],
        source:String,
        target:String,
        fromDate:DateTime,
        toDate:DateTime,
        value:Float
    )

    implicit val savedRateFormat = Json.format[SavedConversionRate]

    def getAllRates():RIO[Has[Repository], List[SavedConversionRate]] = 
        ZIO.serviceWith(_.getAllRates())

    def getRates(source: Set[String], target:Set[String]):RIO[Has[Repository],List[SavedConversionRate]] = 
        ZIO.serviceWith(_.getRates(source, target))

    def getRatesBySource(source:String):RIO[Has[Repository],List[SavedConversionRate]] = 
        ZIO.serviceWith(_.getRatesBySource(source))

    def getRatesByTarget(target:String):RIO[Has[Repository],List[SavedConversionRate]] = 
        ZIO.serviceWith(_.getRatesByTarget(target))

    def saveRates(rates:List[ConversionRate]):RIO[Has[Repository], List[SavedConversionRate]] =
        ZIO.serviceWith(_.saveRates(rates))

    def deleteRates(ids:Set[Int]):RIO[Has[Repository], Unit] =
        ZIO.serviceWith(_.deleteRates(ids))
}

@inject.Singleton
case class TestRateRepository() extends Repository{
    import Repository._
    private var savedRates = Map.empty[Int,SavedConversionRate]
    private var nextId : Int = 0
    
    def getAllRates(): Task[List[SavedConversionRate]] = 
        Task.succeed(savedRates.view.map(_._2).toList.sortBy(_.id))

    def getRatesBySource(source: String): Task[List[SavedConversionRate]] = {
        Task.succeed(savedRates.withFilter(_._2.source == source).map(_._2).toList)
    }

    def getRates(source: Set[String], target:Set[String]): Task[List[SavedConversionRate]] = {
        Task.succeed(savedRates.withFilter(r => 
            source.contains(r._2.source) && 
            target.contains(r._2.target)).map(_._2).toList)
    }

    def getRatesByTarget(target: String): Task[List[SavedConversionRate]] = {
        Task.succeed(savedRates.withFilter(_._2.target == target).map(_._2).toList)
    }

    def deleteRates(ids: Set[Int]): Task[Unit] = {    
        savedRates = savedRates.removedAll(ids)
        Task.succeed(())
    }

    def saveRates(rates: List[ConversionRate]): Task[List[SavedConversionRate]] = {
        if(rates.isEmpty) Task.succeed(List.empty[SavedConversionRate])
        else{
            val (first :: rest) = rates
            val savedRate = saveRate(first)
            savedRates = savedRates + (savedRate.id.get -> savedRate)
            saveRates(rest).map(savedRate :: _)
        }
    }

    private def saveRate(rate:ConversionRate):SavedConversionRate = {
        rate.id match {
            case None => 
                createSavedRate(getNextId(), rate)
            case Some(id) => 
                createSavedRate(id, rate)
        }
    }

    private def createSavedRate(id:Int, rate: ConversionRate) = 
        SavedConversionRate(Some(id), rate.source, rate.target, rate.startDate, rate.endDate, rate.rate)

    private def getNextId():Int = {
        nextId = nextId + 1;
        println("New generated id :"+nextId)
        nextId
    }
}

// object TestRateRepository {
//     val layer: ULayer[Has[Repository]] = ZLayer.succeed(TestRateRepository())
// }