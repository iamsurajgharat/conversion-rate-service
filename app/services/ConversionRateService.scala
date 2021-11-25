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
    def getRates(requests:List[ConversionRateRequest]):Task[List[ConversionRateResponse]]
}

object ConversionRateService{
    val defaultStartDate = new DateTime(1, 1, 1, 0, 0)
    val defaultEndDate = new DateTime(4000, 12, 31, 0, 0)
}

@inject.Singleton
class ConversionRateServiceImpl @Inject() (repository:Repository) extends ConversionRateService{
    val repoLayer: ULayer[Has[Repository]] = ZLayer.succeed(repository)
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

    def getRates(requests:List[ConversionRateRequest]):Task[List[ConversionRateResponse]] = {
        val allUnits = requests.flatMap(x => List(x.source, x.target)).toSet
        for{
            savedRates <- repository.getRatesByTarget(allUnits)
            rateGraph <- ZIO.attempt(RateGraph(savedRates))
            res <- ZIO.foreach(requests)(x => {
                val date = getRateDate(x.date)
                val edges = rateGraph.getEdgesBetween(x.source, x.target, date)
                val value = calculateValueFromEdges(edges)
                ZIO.succeed(ConversionRateResponse(x.source, x.target, date, value.getOrElse(-1)))
            })
        } yield res
    }

    private def createGraph(rates:List[SavedConversionRate]):RateGraph = ???

    private def validateRate(rates: List[ConversionRate], 
        savedRates:Seq[SavedConversionRate]):IO[ValidationException, List[SavedConversionRate]] = {
        val updatedSavedRates = getUpdateSavedRates(rates, savedRates)
        val savedRatesByTarget = updatedSavedRates.groupBy(_.target)
        val validInvalidRates = rates.map(_.toSavedRate(ConversionRateService.defaultStartDate, ConversionRateService.defaultEndDate))
            .zipWithIndex
            .partition(newRate => {
            savedRatesByTarget.get(newRate._1.target).getOrElse(Nil).view.exists(sr => (newRate._1.id == None ||(newRate._1.id.get != sr.id.get)) && sr.overlap(newRate._1))
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

    case class RateGraphEdge(fromDate:DateTime, toDate:DateTime, target:String, value:Float)
    
    case class RateGraphNode(unit:String, edges:Map[String,RateGraphEdge]){
        def addEdge(edge:RateGraphEdge):RateGraphNode = RateGraphNode(unit, edges + (edge.target -> edge))
    }

    class RateGraph(nodes:Map[String, RateGraphNode]){
        def addRate(rate:SavedConversionRate):RateGraph = {
            val node1 = nodes
            .getOrElse(rate.source, RateGraphNode(rate.source, Map.empty[String, RateGraphEdge]))
            .addEdge(RateGraphEdge(rate.fromDate, rate.toDate, rate.target, rate.value))
            
            val node2 = nodes
            .getOrElse(rate.target, RateGraphNode(rate.target, Map.empty[String, RateGraphEdge]))
            .addEdge(RateGraphEdge(rate.fromDate, rate.toDate, rate.source, 1/rate.value))
            new RateGraph(nodes ++ List((rate.source -> node1), (rate.target -> node2)))
        }

        def getEdgesBetween(start:String, end:String, date:DateTime):List[RateGraphEdge] = {
            if(!nodes.contains(start) || !nodes.contains(end)) Nil
            else{
                if(nodes(start).edges.contains(end)) List(nodes(start).edges(end))
                else List(
                    nodes(start).edges.head._2, 
                    nodes(nodes(start).edges.head._1).edges(end)
                )
            }
        }
    }

    object RateGraph{
        def apply(rates:Seq[SavedConversionRate]):RateGraph = RateGraph(new RateGraph(Map.empty[String,RateGraphNode]), rates)
        
        private def apply(graph:RateGraph, rates:Seq[SavedConversionRate]):RateGraph = {
            if(rates.isEmpty) graph
            else{
                val (head :: rest) = rates
                RateGraph(graph.addRate(head), rest)
            }
        }
    }
}

case class ValidationException(message:String) extends Exception(message)