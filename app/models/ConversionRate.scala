package com.surajgharat.conversionrates.models
//import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import org.joda.time.DateTime
object ConversionRateRequest {
  implicit val dateTimeWrites = new Writes[DateTime] {
    def writes(date: DateTime) = {
        println("I can see this!!!")
      JsString(
        date.year().get().toString() + "-" +
          date.monthOfYear().get().toString() + "-" +
          date.dayOfMonth().get().toString()
      )
    }
  }

  implicit val dateTimeReads: Reads[DateTime] = new Reads[DateTime] {
    def reads(jsObj: JsValue): JsResult[DateTime] = {
      jsObj match {
        case JsString(str) =>
          val parts = str.split("-")
          if (parts.length != 3) JsError("Not supported date format :" + str)
          else
            JsSuccess(
              new DateTime(parts(0).toInt, parts(1).toInt, parts(2).toInt, 0, 0)
            )
        case _ => JsError("Not supported date format")
      }
    }
  }
  implicit val format = Json.format[ConversionRateRequest]
}

case class ConversionRateRequest(
    source: String,
    target: String,
    date: Option[DateTime]
)

object ConversionRateResponse {
  implicit val dateTimeWrites = new Writes[DateTime] {
    def writes(date: DateTime) = {
        println("I can see this!!!")
      JsString(
        date.year().get().toString() + "-" +
          date.monthOfYear().get().toString() + "-" +
          date.dayOfMonth().get().toString()
      )
    }
  }

  implicit val dateTimeReads: Reads[DateTime] = new Reads[DateTime] {
    def reads(jsObj: JsValue): JsResult[DateTime] = {
      jsObj match {
        case JsString(str) =>
          val parts = str.split("-")
          JsSuccess(
            new DateTime(parts(0).toInt, parts(1).toInt, parts(2).toInt, 0, 0)
          )
        case _ => JsError("Not supported date format")
      }
    }
  }
  implicit val responsFormat = Json.format[ConversionRateResponse]
}

case class ConversionRateResponse(
    source: String,
    target: String,
    date: DateTime,
    rate: Float
)
