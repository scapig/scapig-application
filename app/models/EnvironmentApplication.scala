package models

import java.util.UUID

import models.AuthType.AuthType


case class EnvironmentApplication(id: UUID,
                                  name: String,
                                  environment: AuthType,
                                  description: String,
                                  applicationUrls: ApplicationUrls)

object EnvironmentApplication {
  def apply(clientId: String, application: Application): EnvironmentApplication = {
    val authType = if(application.credentials.production.clientId == clientId) AuthType.PRODUCTION else AuthType.SANDBOX
    EnvironmentApplication(application.id, application.name, authType, application.description, application.applicationUrls)
  }
}

object AuthType extends Enumeration {
  type AuthType = Value
  val PRODUCTION, SANDBOX = Value
}

case class AuthenticateRequest(clientId: String, clientSecret: String)
