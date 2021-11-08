package com.surajgharat.conversionrates
package repositories

import com.surajgharat.conversionrates.repositories.Repository
import javax.inject._
import zio.{ZIO,Task}
import com.surajgharat.conversionrates.models._
import org.joda.time.DateTime
import scala.concurrent.Future
import org.joda.time.DateTimeZone
//import slick.jdbc.PostgresProfile.api._

object rateRepositoryLive {
    
    import slick.jdbc.PostgresProfile.api._
    import scala.concurrent.ExecutionContext.Implicits.global
    import com.github.tototoshi.slick.PostgresJodaSupport._

    class SlickRateRepository() extends Repository {
        
        def getAllRates(): Task[List[Repository.SavedConversionRate]] = {
            ZIO.fromFuture(ec => {
                val query = rates.sortBy(_.id).result
                db.run(query).map(_.toList).map(_.map(convert))
            });
        }
        
        def getRates(source: Set[String], target: Set[String]): Task[List[Repository.SavedConversionRate]] = ???
        
        def getRatesBySource(source: String): Task[List[Repository.SavedConversionRate]] = ???
        
        def getRatesByTarget(target: String): Task[List[Repository.SavedConversionRate]] = ???
        
        def saveRates(rates: List[ConversionRate]): Task[List[Repository.SavedConversionRate]] = ???
        
        def deleteRates(ids: Set[Long]): Task[Unit] = ???

        private def convert(rate:SavedConversionRate2):Repository.SavedConversionRate = {
            Repository.SavedConversionRate(rate.id.get, 
                ConversionRate(rate.source, rate.target, new org.joda.time.DateTime(),
                new DateTime(), rate.value, Some(rate.id.get)))
        }
        
    }

    case class SavedConversionRate2(
        id:Option[Int],
        source:String,
        target:String,
        fromDate:Option[DateTime],
        toDate:Option[DateTime],
        value:Float
        )

    class ConversionRates(tag: Tag) extends Table[SavedConversionRate2](tag, "conversion_rates") {
        def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
        def source = column[String]("source")
        def target = column[String]("target")
        def fromDate = column[Option[DateTime]]("fromdate")
        def toDate = column[Option[DateTime]]("todate")
        def value = column[Float]("value")
        override def * = (id.?, source, target, fromDate, toDate, value) <> (SavedConversionRate2.tupled, SavedConversionRate2.unapply)
    }

    val rates = TableQuery[ConversionRates]
    val db = Database.forConfig("mydb")
}