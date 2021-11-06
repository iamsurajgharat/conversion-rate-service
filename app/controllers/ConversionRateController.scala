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
import zio.{Task,UIO,ZIO}

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

    def getAllRates() = zioAction { _ =>
        rateService.getAllRates().fold(handleInternalError, handleSuccess)
    }

    def saveRates() = zioActionWithBody { request => 
        def validateError(errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]) : UIO[Result] = {
            val validationError = ZIO.attempt(Json.obj("errors" -> JsError.toJson(errors))).
                orElse(ZIO.succeed(Json.obj("errors" -> "Error in parsing input")))
            validationError.map(BadRequest(_))
        }

        def validateSuccess(rates:List[ConversionRate]) : UIO[Result] = {
            rateService.saveRates(rates).fold(handleInternalError, handleSuccess)
        }

        request.body.validate[List[ConversionRate]].fold(validateError, validateSuccess)
    }

    def zioAction(actionFun : Request[AnyContent] => UIO[Result]):Action[AnyContent] = {
        Action { request =>
            ((interpret _) compose actionFun)(request)
        }
    }
    
    def zioActionWithBody(actionFun : Request[JsValue] => UIO[Result]):Action[JsValue] = {
        Action(parse.json) { request =>
            ((interpret _) compose actionFun)(request)
        }
    }
    
    private def interpret(effect: UIO[Result]):Result = zio.Runtime.default.unsafeRunTask(effect)
    private def handleInternalError(e:Throwable):Result = InternalServerError(e.getMessage())
    private def handleSuccess(data:List[SavedConversionRate]):Result = Ok(Json.toJson(data))
}
