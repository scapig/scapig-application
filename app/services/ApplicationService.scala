package services

import javax.inject.{Inject, Singleton}

import models.{Application, Environment, AuthenticateRequest, EnvironmentApplication}
import repository.ApplicationRepository

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ApplicationService @Inject()(applicationRepository: ApplicationRepository) {

  def fetch(id: String): Future[Option[Application]] = {
    applicationRepository.fetch(id)
  }

  def fetchByClientId(clientId: String): Future[Option[EnvironmentApplication]] = {
    applicationRepository.fetchByClientId(clientId) map (_ map (app => EnvironmentApplication(clientId, app)))
  }

  def fetchByServerToken(serverToken: String): Future[EnvironmentApplication] = {
    applicationRepository.fetchByServerToken(serverToken) map { app =>
      val clientId = if (app.credentials.production.serverToken == serverToken) app.credentials.production.clientId else app.credentials.sandbox.clientId
      EnvironmentApplication(clientId, app)
    }
  }

  def createOrUpdate(application: Application): Future[Application] = applicationRepository.save(application)

  def authenticate(authenticateRequest: AuthenticateRequest): Future[Option[EnvironmentApplication]] = {
    applicationRepository.fetchByClientId(authenticateRequest.clientId) map (_.flatMap { app =>
      Seq(app.credentials.production, app.credentials.sandbox)
        .find(t => t.clientId == authenticateRequest.clientId && t.clientSecrets.exists(_.secret == authenticateRequest.clientSecret)
        ).map(_ => EnvironmentApplication(authenticateRequest.clientId, app))
    })
  }
}
