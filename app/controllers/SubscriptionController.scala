package controllers

import javax.inject.{Inject, Singleton}

import models.JsonFormatters._
import models._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.SubscriptionService

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SubscriptionController  @Inject()(cc: ControllerComponents,
                                        subscriptionService: SubscriptionService) extends AbstractController(cc) with CommonControllers {

  def getSubscriptions(id: String) = Action.async { implicit request =>
    subscriptionService.getSubscriptions(id) map { apis => Ok(Json.toJson(apis))
    } recover {
      case _: ApplicationNotFoundException => ApplicationNotFound("id", id).toHttpResponse
    }
  }

  def subscribe(id: String, context: String, version: String) = Action.async { implicit request =>
    subscriptionService.subscribe(id, APIIdentifier(context, version)) map { _ => NoContent } recover {
      case _: ApplicationNotFoundException => ApplicationNotFound("id", id).toHttpResponse
    }
  }

  def unsubscribe(id: String, context: String, version: String) = Action.async { implicit request =>
    subscriptionService.unsubscribe(id, APIIdentifier(context, version)) map { _ => NoContent } recover {
      case _: ApplicationNotFoundException => ApplicationNotFound("id", id).toHttpResponse
    }
  }

}
