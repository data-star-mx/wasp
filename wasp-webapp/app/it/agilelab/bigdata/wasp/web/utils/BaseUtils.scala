package it.agilelab.bigdata.wasp.web.utils

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import play.api.mvc.Results.Redirect
import play.api.mvc._
import java.util.SimpleTimeZone
import scala.util.Random

object Utils extends BaseUtils {
  def toTimeAgo(t : Timestamp) = new TimestampExt(t).toTimeAgo
  def toTimeAgoFull(t : Timestamp) = new TimestampExt(t).toTimeAgoFull
}

trait BaseUtils {

  def generatePassword(passwordLength : Int = 8) = "a" + Random.nextLong.abs.toString.take(passwordLength).toSeq.mkString

  def now = new Timestamp(System.currentTimeMillis)

  private def eraseDaytTime(cal : Calendar) = {

    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    cal
  }

  def ceilDate(ts : Timestamp = now) = {

    var cal = Calendar.getInstance();
    cal.setTimeInMillis(ts.getTime())
    cal.add(Calendar.DATE, 1);
    cal = eraseDaytTime(cal)
    new Timestamp(cal.getTime.getTime)
  }

  def floorDate(ts : Timestamp = now) = {

    var cal = Calendar.getInstance();
    cal.setTimeInMillis(ts.getTime())
    cal = eraseDaytTime(cal)
    new Timestamp(cal.getTime.getTime)
  }

  def italianDateFormat : String = "dd/MM/yyyy"
  def italianDateTimeFormat : String = "dd/MM/yyyy HH:mm:ss"

  def gmtDateTimeFormat2 = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
    sdf.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
    sdf
  }

  def gmtDateFormatter = {
    val sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z", Locale.US);
    sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
    sdf.applyPattern("dd MMM yyyy HH:mm:ss z");
    sdf
  }

  class TimestampExt(val ts : Timestamp) {

    def toDateStr(format : String = italianDateFormat) = {
      var format1 = new SimpleDateFormat(format)
      format1.format(ts.getTime)
    }

    def toDateTimeStr(format : String = italianDateTimeFormat) = {
      var format1 = new SimpleDateFormat(format)
      format1.format(ts.getTime)
    }

    def toTimeAgo = {
      var resString = "";
      val diffMills = now.getTime() - ts.getTime()

      val seconds = diffMills / 1000
      val minutes = seconds / 60
      val hours = minutes / 60
      val days = hours / 24

      if (days > 0) {
        resString = toDateStr()
      }
      else if (hours > 0) {
        resString = hours + " ore fa"
      }
      else if (minutes > 0) {
        resString = minutes + " minuti fa"
      }
      else {
        resString = "Pochi secondi fa"
      }

      resString
    }

    def toTimeAgoFull = {
      def conv(value : Long, next : Seq[(Int, Seq[String])]) : Option[String] =
        next.headOption.flatMap(v => {
          val l = value / v._1
          conv(l, next.tail).orElse(if (l > 0) {
            if (l > 1 && v._2.length > 1) Some(v._2.tail.head format l)
            else Some(v._2.head format l)
          }
          else None)
        })

      val defs = (1000, List("pochi secondi fa")) :: (60, List("%d minuti fa")) :: (60, List("un'ora fa", "%d ore fa")) :: (24, List("un giorno fa", "%d giorni fa")) :: (30, List("pi&ugrave; di un mese fa", "pi&ugrave; di %d mesi fa")) :: (12, List("pi&ugrave; di un anno fa")) :: Nil

      conv(now.getTime - ts.getTime, defs)
    }

    def toTimeStr(format : String = "HH:mm") = {
      var format1 = new SimpleDateFormat(format)
      format1.format(ts.getTime)
    }

    def ceil() = {
      ceilDate(ts)
    }

    def floor() = {
      floorDate(ts)
    }
  }

  class DateStrParser(val str : String) {

    def parseDate(format : String = italianDateFormat) = {
      new SimpleDateFormat(format, Locale.ITALIAN).parse(str);
    }

    def parseTimestamp(format : String = italianDateFormat) = {
      new Timestamp(parseDate(format).getTime)
    }

  }

  implicit def tsToTsExt(ts : Timestamp) = new TimestampExt(ts)
  implicit def strToDateParser(str : String) = new DateStrParser(str)
  implicit def strToCMStrUtils(str : String) = new CMStrUtils(str)

}

class CMStrUtils(val str : String) {

  def toDefinedLenghtString(maxlenght : Int, popoverTitle : String) = if (str.length < maxlenght) str else str.substring(0, maxlenght - 3) + " <a href='javascript:;' class='popovers' data-html='true' data-toggle='popover' data-placement='left' data-container='body' data-content='" + str.replace('\'', '\u0022').replace('"', '\"') + "' data-original-title='" + popoverTitle + "'>...</a>"

}