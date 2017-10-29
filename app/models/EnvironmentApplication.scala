package models

import java.util.UUID

import models.Environment.Environment
import models.RateLimitTier.RateLimitTier


case class EnvironmentApplication(id: UUID,
                                  clientId: String,
                                  name: String,
                                  environment: Environment,
                                  description: String,
                                  rateLimitTier: RateLimitTier,
                                  redirectUris: Seq[String])

object EnvironmentApplication {
  def apply(clientId: String, application: Application): EnvironmentApplication = {
    val environment = if(application.credentials.production.clientId == clientId) Environment.PRODUCTION else Environment.SANDBOX
    EnvironmentApplication(application.id, clientId, application.name, environment, application.description, application.rateLimitTier, application.redirectUris)
  }
}

case class AuthenticateRequest(clientId: String, clientSecret: String)
