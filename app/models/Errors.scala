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

case class ErrorInternalServerError(errorMessage: String) extends ErrorResponse(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", errorMessage)

case class ErrorInvalidRequest(errorMessage: String) extends ErrorResponse(BAD_REQUEST, "INVALID_REQUEST", errorMessage)
case class ApplicationNotFound(key: String, id: String) extends ErrorResponse(NOT_FOUND, "NOT_FOUND", s"no application found for $key $id")
case class ErrorNotFound() extends ErrorResponse(NOT_FOUND, "NOT_FOUND", "The resource could not be found.")

class ValidationException(message: String) extends RuntimeException(message)

case class ApplicationNotFoundException() extends RuntimeException()
