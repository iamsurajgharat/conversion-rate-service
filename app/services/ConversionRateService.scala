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
import akka.http.javadsl.model.headers.Date

@ImplementedBy(classOf[ConversionRateServiceImpl])
trait ConversionRateService{
    def saveRates(rates:List[ConversionRate]): Task[List[ConversionRate]]
    def getAllRates(): Task[List[ConversionRate]]
    def getRates(requests:List[ConversionRateRequest]):Task[List[ConversionRateResponse]]
}

object ConversionRateService{
    val defaultStartDate = new DateTime(1, 1, 1, 0, 0)
    val defaultEndDate = new DateTime(4000, 12, 31, 0, 0)
    val ValueForUndefinedRate = -1f;
}

@inject.Singleton
class ConversionRateServiceImpl @Inject() (repository:Repository) extends ConversionRateService{
    val repoLayer: ULayer[Has[Repository]] = ZLayer.succeed(repository)
    def saveRates(rates: List[ConversionRate]): Task[List[ConversionRate]] = {
        // find overlaps if any
        val savedRatesEffect = for{
            oldSavedRates <- Repository.getRatesByTarget(rates.map(_.target).toSet)
            validatedRates <- validateRate(rates, oldSavedRates)
            savedRates <- Repository.saveRates(validatedRates)
            rates <- mapToWebModel(savedRates)
        } yield rates
        
        savedRatesEffect.provideLayer(repoLayer)
    }
    
    def getAllRates(): Task[List[ConversionRate]] = 
        Repository.getAllRates().map(x => x.map(ConversionRate(_))).provideLayer(repoLayer)

    def getRates(requests:List[ConversionRateRequest]):Task[List[ConversionRateResponse]] = {
        val allUnits = requests.flatMap(x => List(x.source, x.target)).toSet
        for{
            savedRates <- repository.getRatesByTarget(allUnits)
            rateGraph <- ZIO.attempt(RateGraph(savedRates))
            res <- ZIO.foreach(requests)(x => {
                val date = getRateDate(x.date)
                val edges = rateGraph.getEdgesBetween(x.source, x.target, date)
                val value = calculateValueFromEdges(edges)
                ZIO.succeed(ConversionRateResponse(x.source, x.target, date, value.getOrElse(ConversionRateService.ValueForUndefinedRate)))
            })
        } yield res
    }

    private def createGraph(rates:List[SavedConversionRate]):RateGraph = ???

    private def mapToWebModel(savedRates:List[SavedConversionRate]) = ZIO.attempt{
        savedRates.map[ConversionRate](ConversionRate(_))
    }

    private def validateRate(rates: List[ConversionRate], 
        savedRates:Seq[SavedConversionRate]):IO[ValidationException, List[SavedConversionRate]] = {
        val updatedSavedRates = getUpdateSavedRates(rates, savedRates)
        val savedRatesByTarget = updatedSavedRates.groupBy(_.target)
        val validInvalidRates = rates.map(_.toSavedRate(ConversionRateService.defaultStartDate, ConversionRateService.defaultEndDate))
            .zipWithIndex
            .partition(newRate => {
            savedRatesByTarget.get(newRate._1.target).getOrElse(Nil).view.exists(sr => (newRate._1.id == None ||(newRate._1.id.get != sr.id.get)) && sr.overlap(newRate._1))
        })

        if (validInvalidRates._1.isEmpty) ZIO.succeed(validInvalidRates._2.map(_._1))        
        else {
            val invalidIndices = validInvalidRates._1.map(_._2)
            val message = s"Overlapping rates [${invalidIndices.mkString(",")}]"
            ZIO.fail(new ValidationException(message))
        }
    }

    private def getUpdateSavedRates(rates:List[ConversionRate], savedRates:Seq[SavedConversionRate]):List[SavedConversionRate] = {
        val rateById = rates.view.filter(_.id != None).foldLeft(Map.empty[Int,ConversionRate])((m,r) => m + (r.id.get -> r))
        var updatedSavedRates = List.empty[SavedConversionRate]
        for(savedRate <- savedRates){
            if(rateById.contains(savedRate.id.get)){
                updatedSavedRates = rateById(savedRate.id.get).toSavedRate(ConversionRateService.defaultStartDate, ConversionRateService.defaultEndDate) :: updatedSavedRates
            }
            else{
                updatedSavedRates = savedRate :: updatedSavedRates
            }
        }
        updatedSavedRates
    }

    private def calculateValueFromEdges(edges:List[RateGraphEdge]):Option[Float] = {
        if(edges.isEmpty) None
        else Some(edges.view.map(_.value).reduce((x,y) => x * y))
    }

    private def getRateDate(reqDate:Option[DateTime]):DateTime = reqDate.getOrElse(new DateTime())

    case class RateGraphEdge(fromDate:DateTime, toDate:DateTime, target:String, value:Float){
        import com.surajgharat.conversionrates.helpers.MyDate._
        def isApplicable(date:DateTime):Boolean = date >= fromDate && date <= toDate
        def reverse(newTarget:String):RateGraphEdge = RateGraphEdge(fromDate, toDate, newTarget, 1/value)
    }
    
    case class RateGraphNode(unit:String, edges:Map[String,List[RateGraphEdge]]){
        def addEdge(edge:RateGraphEdge):RateGraphNode = {
            val finalEdges = edge :: edges.getOrElse(edge.target, List.empty[RateGraphEdge])
            RateGraphNode(unit, edges + (edge.target -> finalEdges))
        }

        def getEdge(target:String, date:DateTime):Option[RateGraphEdge] = {
            if(!edges.contains(target)) None 
            else edges(target).find(_.isApplicable(date))
        }
    }

    class RateGraph(nodes:Map[String, RateGraphNode]){
        def addRate(rate:SavedConversionRate):RateGraph = {
            val node1 = nodes
            .getOrElse(rate.source, RateGraphNode(rate.source, Map.empty[String, List[RateGraphEdge]]))
            .addEdge(RateGraphEdge(rate.fromDate, rate.toDate, rate.target, rate.value))
            
            val node2 = nodes
            .getOrElse(rate.target, RateGraphNode(rate.target, Map.empty[String, List[RateGraphEdge]]))
            .addEdge(RateGraphEdge(rate.fromDate, rate.toDate, rate.source, 1/rate.value))
            new RateGraph(nodes ++ List((rate.source -> node1), (rate.target -> node2)))
        }

        def getEdgesBetween(start:String, end:String, date:DateTime):List[RateGraphEdge] = {
            if(!nodes.contains(start) || !nodes.contains(end)) Nil
            else{
                if(nodes(start).edges.contains(end)) 
                    nodes(start).getEdge(end, date).fold[List[RateGraphEdge]](Nil)(List(_))
                else {
                    val intNode = nodes(start).edges.head._1
                    (nodes(intNode).getEdge(start, date), nodes(intNode).getEdge(end, date)) match {
                        case (Some(e1), Some(e2)) => List(e1.reverse(intNode), e2)
                        case (_,_) => Nil
                    }
                }
            }
        }
    }

    object RateGraph{
        def apply(rates:Seq[SavedConversionRate]):RateGraph = RateGraph(new RateGraph(Map.empty[String,RateGraphNode]), rates)
        
        private def apply(graph:RateGraph, rates:Seq[SavedConversionRate]):RateGraph = {
            if(rates.isEmpty) graph
            else{
                val (head :: rest) = rates.toList
                this(graph.addRate(head), rest)
            }
        }
    }
}

case class ValidationException(message:String) extends Exception(message)