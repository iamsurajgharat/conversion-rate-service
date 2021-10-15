package com.surajgharat.conversionrates.controllers
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import javax.inject._
import com.surajgharat.conversionrates.models._
import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import scala.concurrent.Future
import org.joda.time

@Singleton
class ConversionRateController @Inject() (
    val controllerComponents: ControllerComponents
    ) extends BaseController {
        
    import ConversionRateRequest._
    import ConversionRateResponse._

    def greet = Action{
        Ok("All is well")
    }

    def rates() = Action(parse.json) { request: Request[JsValue] =>
        def error(errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]) = 
        BadRequest(Json.obj("errors" -> JsError.toJson(errors)))

        def success(request:List[ConversionRateRequest]) : Result = {
            Ok(Json.toJson(request.map(r => ConversionRateResponse(r.source, r.target, r.date.getOrElse(time.DateTime.now()), 234))))
        }

        request.body.validate[List[ConversionRateRequest]].fold(error, success);
    }

    def action1(name:String) = TODO

    
}
