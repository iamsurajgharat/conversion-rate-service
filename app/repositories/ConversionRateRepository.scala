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
    def getRatesByTarget(units:Set[String]):Task[Seq[SavedConversionRate]]
    def saveRates(rates:List[SavedConversionRate]):Task[List[SavedConversionRate]]
    def deleteRates(ids:Set[Int]):Task[Unit]
}

object Repository{
    import com.surajgharat.conversionrates.models.BaseResponse._
    case class SavedConversionRate(
        id:Option[Int],
        source:String,
        target:String,
        fromDate:DateTime,
        toDate:DateTime,
        value:Float
    ){
        def overlap(that:SavedConversionRate):Boolean = {
            import helpers.MyDate._
            if (this.fromDate <= that.fromDate) that.fromDate >= this.fromDate && that.fromDate <= this.toDate
            else this.fromDate >= that.fromDate && this.fromDate <= that.toDate
        }
    }

    implicit val savedRateFormat = Json.format[SavedConversionRate]

    def getAllRates():RIO[Has[Repository], List[SavedConversionRate]] = 
        ZIO.serviceWith(_.getAllRates())

    def getRatesByTarget(units:Set[String]):RIO[Has[Repository],Seq[SavedConversionRate]] = 
        ZIO.serviceWith(_.getRatesByTarget(units))

    def saveRates(rates:List[SavedConversionRate]):RIO[Has[Repository], List[SavedConversionRate]] =
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

    def getRatesByTarget(units:Set[String]):Task[Seq[SavedConversionRate]] = ???

    def deleteRates(ids: Set[Int]): Task[Unit] = {    
        savedRates = savedRates.removedAll(ids)
        Task.succeed(())
    }

    def saveRates(rates: List[SavedConversionRate]): Task[List[SavedConversionRate]] = {
        if(rates.isEmpty) Task.succeed(List.empty[SavedConversionRate])
        else{
            val (first :: rest) = rates
            val savedRate = saveRate(first)
            savedRates = savedRates + (savedRate.id.get -> savedRate)
            saveRates(rest).map(savedRate :: _)
        }
    }

    private def saveRate(rate:SavedConversionRate):SavedConversionRate = {
        rate.id match {
            case None => 
                createSavedRate(getNextId(), rate)
            case Some(id) => 
                createSavedRate(id, rate)
        }
    }

    private def createSavedRate(id:Int, rate: SavedConversionRate) = 
        SavedConversionRate(Some(id), rate.source, rate.target, rate.fromDate, rate.toDate, rate.value)

    private def getNextId():Int = {
        nextId = nextId + 1;
        println("New generated id :"+nextId)
        nextId
    }
}

// object TestRateRepository {
//     val layer: ULayer[Has[Repository]] = ZLayer.succeed(TestRateRepository())
// }