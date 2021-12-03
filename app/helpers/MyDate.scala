package com.surajgharat.conversionrates
package helpers

import org.joda.time.DateTime

class MyDate(dateTime:DateTime){
    private val date = dateTime.toDate()
    def <(that:MyDate):Boolean = this.date.before(that.date)
    def ==(that:MyDate):Boolean = this.date.equals(that.date)
    def <=(that:MyDate):Boolean = this < that || this == that
    def >(that:MyDate):Boolean = this.date.after(that.date)
    def >=(that:MyDate):Boolean = this > that || this == that
}

object MyDate {
    implicit def toMyDate(value:DateTime):MyDate = new MyDate(value)
}