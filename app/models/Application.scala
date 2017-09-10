package models

import java.util.UUID

import models.Generator.random
import models.RateLimitTier.BRONZE
import org.joda.time.DateTime

case class Application(name: String,
                       description: String,
                       collaborators: Set[Collaborator],
                       applicationUrls: ApplicationUrls,
                       tokens: ApplicationTokens = ApplicationTokens(),
                       createdOn: DateTime = DateTime.now,
                       rateLimitTier: Option[RateLimitTier.Value] = Some(BRONZE),
                       id: UUID = UUID.randomUUID()) {

  lazy val admins = collaborators.filter(_.role == Role.ADMINISTRATOR)
}

object Application {
  def apply(req: CreateApplicationRequest): Application = Application(req.name, req.description, req.normalisedCollaborators, req.applicationUrls)
}

case class Collaborator(emailAddress: String, role: Role.Value)

case class ApplicationTokens(production: EnvironmentToken = EnvironmentToken(),
                             sandbox: EnvironmentToken = EnvironmentToken()) {

  def environmentToken(environment: Environment.Value) = {
    environment match {
      case Environment.PRODUCTION => production
      case _ => sandbox
    }
  }
}

case class EnvironmentToken(clientId: String = random(),
                            serverToken: String = random(),
                            clientSecrets: Seq[ClientSecret] = Seq(ClientSecret()))

case class ClientSecret(secret: String = random(),
                        createdOn: DateTime = DateTime.now())

case class ApplicationUrls(redirectUris: Seq[String],
                           termsAndConditionsUrl: String,
                           privacyPolicyUrl: String)

object Role extends Enumeration {
  type Role = Value
  val DEVELOPER, ADMINISTRATOR = Value
}

object Environment extends Enumeration {
  type Environment = Value
  val PRODUCTION, SANDBOX = Value
}

object RateLimitTier extends Enumeration {
  type RateLimitTier = Value
  val GOLD, SILVER, BRONZE = Value
}

object Generator {
  def random(): String = UUID.randomUUID().toString.replaceAll("-", "")
}