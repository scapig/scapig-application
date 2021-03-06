package models

import java.util.UUID

import models.Generator.random
import models.RateLimitTier.BRONZE
import org.joda.time.DateTime

case class Application(name: String,
                       description: String,
                       collaborators: Set[Collaborator],
                       redirectUris: Seq[String],
                       credentials: ApplicationCredentials = ApplicationCredentials(),
                       createdOn: DateTime = DateTime.now,
                       rateLimitTier: RateLimitTier.Value = BRONZE,
                       id: UUID = UUID.randomUUID()) {

  lazy val admins = collaborators.filter(_.role == Role.ADMINISTRATOR)
}

object Application {
  def apply(req: CreateApplicationRequest): Application = Application(req.name, req.description, req.normalisedCollaborators, req.redirectUris)
}

case class Collaborator(emailAddress: String, role: Role.Value)

case class ApplicationCredentials(production: EnvironmentCredentials = EnvironmentCredentials(),
                                  sandbox: EnvironmentCredentials = EnvironmentCredentials()) {

  def environmentToken(environment: Environment.Value) = {
    environment match {
      case Environment.PRODUCTION => production
      case _ => sandbox
    }
  }
}

case class EnvironmentCredentials(clientId: String = random(),
                                  serverToken: String = random(),
                                  clientSecrets: Seq[ClientSecret] = Seq(ClientSecret()))

case class ClientSecret(secret: String = random(),
                        createdOn: DateTime = DateTime.now())


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