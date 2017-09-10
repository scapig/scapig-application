package models

import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import models.JsonFormatters._

sealed abstract class ErrorResponse(
                                     val httpStatusCode: Int,
                                     val errorCode: String,
                                     val message: String) {

  def toHttpResponse: Result = Results.Status(httpStatusCode)(Json.toJson(this))
}

case class ErrorInvalidRequest(errorMessage: String) extends ErrorResponse(BAD_REQUEST, "INVALID_REQUEST", errorMessage)

class ValidationException(message: String) extends RuntimeException(message)
