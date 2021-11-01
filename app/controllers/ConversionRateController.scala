package com.surajgharat.conversionrates.controllers
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import javax.inject._
import com.surajgharat.conversionrates.models._
import com.surajgharat.conversionrates.services._
import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import scala.concurrent.Future
import org.joda.time
import com.surajgharat.conversionrates.repositories._
import zio.Task

@Singleton
class ConversionRateController @Inject() (
        val rateService: ConversionRateServiceSpec,
        val controllerComponents: ControllerComponents
    ) extends BaseController {
        
    import ConversionRate._
    import Repository._
    private val logger = Logger(getClass)

    def greet = Action { request =>
        Ok("All is well")
    }
    
    def getRates() = Action(parse.json) { request: Request[JsValue] =>
        logger.trace("getRates request hit")
        def error(errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]) = 
            BadRequest(Json.obj("errors" -> JsError.toJson(errors)))

        def success(request:List[ConversionRateRequest]) : Result = {
            Ok(Json.toJson(request.map(r => ConversionRateResponse(r.source, r.target, r.date.getOrElse(time.DateTime.now()), 234))))
        }

        request.body.validate[List[ConversionRateRequest]].fold(error, success);
    }

    def saveRates2() = Action(parse.json) { request: Request[JsValue] =>
        def error(errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]) = 
            BadRequest(Json.obj("errors" -> JsError.toJson(errors)))

        def success(request:List[ConversionRate]) : Result = {
            //Ok(Json.toJson(request.map(r => ConversionRateResponse(r.source, r.target, r.date.getOrElse(time.DateTime.now()), 234))))
            Ok(Json.toJson(List.empty[SavedConversionRate]))
        }

        request.body.validate[List[ConversionRate]].fold(error, success);
    }

    def getAllRates() = action { () =>
        rateService.getAllRates()
    }

    def saveRates() = postAction { request => 
        def error(errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]) = 
            Task.fail(new Exception("Input parding issue"))

        def success(rates:List[ConversionRate]) : Task[List[SavedConversionRate]] = 
            rateService.saveRates(rates)

        request.body.validate[List[ConversionRate]].fold(error, success)
    }

    def postAction(actionFun : Request[JsValue] => Task[List[SavedConversionRate]]):Action[JsValue] = {
        def interpret(effect: Task[List[SavedConversionRate]]):Result = {
            Ok(Json.toJson(zio.Runtime.default.unsafeRunTask(effect)))
        }

        Action(parse.json){ request =>
            interpret(actionFun(request))
        }
    }

    def action(actionFun : () => Task[List[SavedConversionRate]]):Action[AnyContent] = {
        def interpret(effect: Task[List[SavedConversionRate]]):Result = {
            Ok(Json.toJson(zio.Runtime.default.unsafeRunTask(effect)))
        }

        Action { _ =>
            interpret(actionFun())
        }
    }
}
