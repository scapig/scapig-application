package controllers

import javax.inject.{Inject, Singleton}

import models.JsonFormatters._
import models._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.ApplicationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

@Singleton
class ApplicationController  @Inject()(cc: ControllerComponents,
                                       applicationService: ApplicationService) extends AbstractController(cc) with CommonControllers {

  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[CreateApplicationRequest] { createAppRequest: CreateApplicationRequest =>
      applicationService.create(createAppRequest) map { application => Created(Json.toJson(application))}
    }
  }

  def update(id: String) = Action.async(parse.json) { implicit request =>
    withJsonBody[UpdateApplicationRequest] { updateApplicationRequest: UpdateApplicationRequest =>
      applicationService.update(id, updateApplicationRequest) map { application => Ok(Json.toJson(application))}
    }
  }

  def fetch(id: String) = Action.async { implicit request =>
    applicationService.fetch(id) map { application => Ok(Json.toJson(application))
    } recover {
      case _: ApplicationNotFoundException => ApplicationNotFound("id", id).toHttpResponse
    }
  }

  def queryDispatcher() = Action.async { implicit request =>
    request.queryString.keys.headOption match {
      case Some("clientId") => fetchByClientId(request.queryString("clientId").head)
      case Some("serverToken") =>  fetchByServerToken(request.queryString("serverToken").head)
      case _ => successful(ErrorInvalidRequest("clientId or serverToken is required").toHttpResponse)
    }
  }

  def fetchAllByCollaboratorEmail(collaboratorEmail: String) = Action.async { implicit request =>
    applicationService.fetchAllByCollaboratorEmail(collaboratorEmail) map { applications =>Ok(Json.toJson(applications))}
  }

  private def fetchByClientId(clientId: String) = {
    applicationService.fetchByClientId(clientId) map {
      case Some(application) => Ok(Json.toJson(application))
      case None => ApplicationNotFound("clientId", clientId).toHttpResponse
    }
  }

  private def fetchByServerToken(serverToken: String) = {
    applicationService.fetchByServerToken(serverToken) map { application =>
      Ok(Json.toJson(application))
    } recover {
      case _: ApplicationNotFoundException => ApplicationNotFound("serverToken", serverToken).toHttpResponse
    }
  }

  def authenticate() = Action.async(parse.json) { implicit request =>
    withJsonBody[AuthenticateRequest] { authenticateRequest: AuthenticateRequest =>
      applicationService.authenticate(authenticateRequest) map {
        case Some(environmentApplication) => Ok(Json.toJson(environmentApplication))
        case None => Unauthorized
      }
    }
  }

}
