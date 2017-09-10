package controllers

import models.{ErrorInvalidRequest, ValidationException}
import play.api.libs.json._
import play.api.mvc.{Request, Result}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait CommonControllers {

  protected def withJsonBody[T]
  (f: (T) => Future[Result])(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]): Future[Result] = {
    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs)) =>
        Future.successful(ErrorInvalidRequest(s"${fieldName(errs)} is required").toHttpResponse)
      case Failure(e) if e.isInstanceOf[ValidationException] =>
        Future.successful(ErrorInvalidRequest(e.getMessage).toHttpResponse)
      case Failure(_) =>
        Future.successful(ErrorInvalidRequest("Unable to process request").toHttpResponse)
    }
  }

  private def fieldName[T](errs: Seq[(JsPath, Seq[JsonValidationError])]) = {
    errs.head._1.toString().substring(1)
  }
}
