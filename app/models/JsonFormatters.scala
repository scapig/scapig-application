package models

import org.joda.time.DateTime
import play.api.libs.json.{JodaReads, JodaWrites, _}

object JsonFormatters {
  val datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  implicit val dateRead: Reads[DateTime] = JodaReads.jodaDateReads(datePattern)
  implicit val dateWrite: Writes[DateTime] = JodaWrites.jodaDateWrites(datePattern)
  implicit val dateFormat: Format[DateTime] = Format[DateTime](dateRead, dateWrite)

  implicit val formatRole = EnumJson.enumFormat(Role)
  implicit val formatEnvironment = EnumJson.enumFormat(Environment)
  implicit val formatRateLimitTier = EnumJson.enumFormat(RateLimitTier)
  implicit val formatClientSecret = Json.format[ClientSecret]
  implicit val formatEnvironmentToken = Json.format[EnvironmentToken]
  implicit val formatApplicationTokens = Json.format[ApplicationTokens]
  implicit val formatCollaborator = Json.format[Collaborator]
  implicit val formatApplicationUrls = Json.format[ApplicationUrls]
  implicit val formatCreateApplicationRequest = Json.format[CreateApplicationRequest]
  implicit val formatApplication = Json.format[Application]
  implicit val errorResponseWrites = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message)
  }

}

object EnumJson {

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException =>
            JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not contain '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }

}