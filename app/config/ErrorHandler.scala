package config

import javax.inject.Singleton

import models.{ErrorInternalServerError, ErrorInvalidRequest}
import play.api.Logger
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

@Singleton
class ErrorHandler extends DefaultHttpErrorHandler {
  override def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(ErrorInvalidRequest(message).toHttpResponse)
  }

  override def onServerError(request: RequestHeader, exception: Throwable) = {
    Logger.error("An unexpected error occurred", exception)
    Future.successful(ErrorInternalServerError(exception.getMessage).toHttpResponse)
  }
}