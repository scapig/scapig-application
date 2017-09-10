package controllers

import javax.inject.{Inject, Singleton}

import models._
import models.JsonFormatters._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.ApplicationService
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ApplicationController  @Inject()(cc: ControllerComponents,
                                       applicationService: ApplicationService) extends AbstractController(cc) with CommonControllers {

  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[CreateApplicationRequest] { createAppRequest: CreateApplicationRequest =>
      applicationService.createOrUpdate(Application(createAppRequest)) map { application => Ok(Json.toJson(application))}
    }
  }

  def fetch(id: String) = Action.async { implicit request =>
    applicationService.fetch(id) map {
      case Some(application) => Ok(Json.toJson(application))
      case None => ApplicationNotFound("id", id).toHttpResponse
    }
  }

  def fetchByClientId(clientId: String) = Action.async { implicit request =>
    applicationService.fetchByClientId(clientId) map {
      case Some(application) => Ok(Json.toJson(application))
      case None => ApplicationNotFound("clientId", clientId).toHttpResponse
    }
  }
}
