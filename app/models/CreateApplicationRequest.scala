package models

case class CreateApplicationRequest(name: String,
                                    description: String,
                                    redirectUris: Seq[String],
                                    collaborators: Set[Collaborator]) {

  val normalisedCollaborators = collaborators.map(c => c.copy(emailAddress = c.emailAddress.toLowerCase))

  require(name.nonEmpty, s"name is required")
  require(collaborators.exists(_.role == Role.ADMINISTRATOR), s"at least one ADMINISTRATOR collaborator is required")
  require(collaborators.size == collaborators.map(_.emailAddress.toLowerCase).size, "duplicate email in collaborator")
  require(redirectUris.size <= 5, "maximum number of redirect URIs exceeded")
}

case class UpdateApplicationRequest(name: String,
                                    description: String,
                                    redirectUris: Seq[String],
                                    rateLimitTier: RateLimitTier.Value)
