package com.surajgharat.conversionrates.models
//import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import org.joda.time.DateTime
import com.surajgharat.conversionrates.repositories.Repository

sealed trait BaseResponse{
  val s1 = 10
}

object BaseResponse {
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
  implicit val baseResponseFormat = Json.format[BaseResponse]

  //def toJsValue[T <: ConversionRateRequest](value:T) : JsValue = Json.toJson(value)
}

case class ConversionRateRequest(
    source: String,
    target: String,
    date: Option[DateTime]
) extends BaseResponse

case class ConversionRateResponse(
    source: String,
    target: String,
    date: DateTime,
    rate: Float
) extends BaseResponse


case class ConversionRate(source: String,
    target: String,
    startDate: Option[DateTime],
    endDate: Option[DateTime],
    rate: Float,
    id:Option[Int]
    ) extends BaseResponse{
      def toSavedRate(sd:DateTime, ed:DateTime):Repository.SavedConversionRate = 
        Repository.SavedConversionRate(id, source, target, startDate.getOrElse(sd), endDate.getOrElse(ed), rate)
    }

object ConversionRate{
  def apply(savedRate:Repository.SavedConversionRate) : ConversionRate = 
    ConversionRate(savedRate.source, savedRate.target, Some(savedRate.fromDate), Some(savedRate.toDate), savedRate.value, savedRate.id)
}