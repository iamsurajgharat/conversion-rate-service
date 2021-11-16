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
        
        def getAllRates(): Task[List[SavedConversionRate]] = {
            ZIO.fromFuture(ec => {
                val query = ratesFromDB.sortBy(_.id).result
                db.run(query).map(_.toList)
            });
        }

        def getRatesByTarget(units:Set[String]):Task[Seq[SavedConversionRate]] = {
            ZIO.fromFuture(ec => {
                val query = ratesFromDB.filter(x => x.target.inSet(units))
                db.run(query.result)
            })
        }
        
        def saveRates(rates: List[SavedConversionRate]): Task[List[SavedConversionRate]] = {
            val (addOnes, updateOnes) = rates.partition(_.id == None)
            val insertWithIdedRateReturn = (ratesFromDB returning ratesFromDB.map(_.id) into ((rate, id) => rate.copy(id=Some(id))))
            val insertAct = insertWithIdedRateReturn ++= addOnes
            val updateAct = DBIO.sequence(updateOnes.map(row => ratesFromDB.filter(_.id === row.id).update(row)))
            println("update request :"+updateOnes)
            
            ZIO.fromFuture(ec => {
                for{
                    addedRate <- db.run(insertAct)
                    _ <- db.run(updateAct)
                } yield (addedRate.toList ++ updateOnes).sortBy(_.id.getOrElse(0))
            })
            
        }

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

    val ratesFromDB = TableQuery[ConversionRates]
    val db = Database.forConfig("mydb")
}