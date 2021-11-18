package com.surajgharat.conversionrates
package helpers
import zio.UIO
import zio.Task
import scala.util.Success
import scala.util.Failure
import scala.util.Try

object ZIOHelper{
    //def interpret[Result](effect: UIO[Result]):Result = zio.Runtime.default.unsafeRunTask(effect)
    def interpret[Result](effect: Task[Result]):Result = zio.Runtime.default.unsafeRunTask(effect)
    def interpret2[Result](effect: Task[Result]):Either[Throwable,Result] = Try(zio.Runtime.default.unsafeRunTask(effect)) match {
        case Success(value) => Right(value)
        case Failure(error) => Left(error)
    }
}