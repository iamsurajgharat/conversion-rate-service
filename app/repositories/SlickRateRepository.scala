package com.surajgharat.conversionrates
package repositories

import com.surajgharat.conversionrates.repositories.Repository
import javax.inject._
import zio.{ZIO,Task}
import com.surajgharat.conversionrates.models._
import org.joda.time.DateTime
import scala.concurrent.Future
import org.joda.time.DateTimeZone

object rateRepositoryLive {
    
    import slick.jdbc.PostgresProfile.api._
    import scala.concurrent.ExecutionContext.Implicits.global
    import com.github.tototoshi.slick.PostgresJodaSupport._
    import Repository.SavedConversionRate

    class SlickRateRepository() extends Repository {
        
        def getAllRates(): Task[List[Repository.SavedConversionRate]] = {
            ZIO.fromFuture(ec => {
                val query = rates.sortBy(_.id).result
                db.run(query).map(_.toList)
            });
        }
        
        def getRates(source: Set[String], target: Set[String]): Task[List[Repository.SavedConversionRate]] = ???
        
        def getRatesBySource(source: String): Task[List[Repository.SavedConversionRate]] = ???
        
        def getRatesByTarget(target: String): Task[List[Repository.SavedConversionRate]] = ???
        
        def saveRates(rates: List[ConversionRate]): Task[List[Repository.SavedConversionRate]] = ???
        
        def deleteRates(ids: Set[Int]): Task[Unit] = ???
    }

    class ConversionRates(tag: Tag) extends Table[SavedConversionRate](tag, "conversion_rates") {
        def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
        def source = column[String]("source")
        def target = column[String]("target")
        def fromDate = column[DateTime]("from_date")
        def toDate = column[DateTime]("to_date")
        def value = column[Float]("value")
        override def * = (id.?, source, target, fromDate, toDate, value) <> (SavedConversionRate.tupled, SavedConversionRate.unapply)
    }

    val rates = TableQuery[ConversionRates]
    val db = Database.forConfig("mydb")
}