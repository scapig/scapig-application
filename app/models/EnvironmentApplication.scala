package models

import java.util.UUID

import models.Environment.Environment


case class EnvironmentApplication(id: UUID,
                                  name: String,
                                  environment: Environment,
                                  description: String,
                                  applicationUrls: ApplicationUrls)

object EnvironmentApplication {
  def apply(clientId: String, application: Application): EnvironmentApplication = {
    val environment = if(application.credentials.production.clientId == clientId) Environment.PRODUCTION else Environment.SANDBOX
    EnvironmentApplication(application.id, application.name, environment, application.description, application.applicationUrls)
  }
}

case class AuthenticateRequest(clientId: String, clientSecret: String)
