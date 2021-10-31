package com.surajgharat.conversionrates.models
//import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import org.joda.time.DateTime
import com.surajgharat.conversionrates.repositories.Repository
object ConversionRate {
  implicit val dateTimeWrites = new Writes[DateTime] {
    def writes(date: DateTime) = {
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
  implicit val requestFormat = Json.format[ConversionRateRequest]
  implicit val responseFormat = Json.format[ConversionRateResponse]
  implicit val rateFormat = Json.format[ConversionRate]
}

case class ConversionRateRequest(
    source: String,
    target: String,
    date: Option[DateTime]
)

case class ConversionRateResponse(
    source: String,
    target: String,
    date: DateTime,
    rate: Float
)


case class ConversionRate(source: String,
    target: String,
    startDate: DateTime,
    endDate: DateTime,
    rate: Float,
    id:Option[Long]
    )