package models

case class CreateApplicationRequest(name: String,
                                    description: String,
                                    applicationUrls: ApplicationUrls,
                                    collaborators: Set[Collaborator]) {

  val normalisedCollaborators = collaborators.map(c => c.copy(emailAddress = c.emailAddress.toLowerCase))

  require(name.nonEmpty, s"name is required")
  require(collaborators.exists(_.role == Role.ADMINISTRATOR), s"at least one ADMINISTRATOR collaborator is required")
  require(collaborators.size == collaborators.map(_.emailAddress.toLowerCase).size, "duplicate email in collaborator")
  require(applicationUrls.redirectUris.size <= 5, "maximum number of redirect URIs exceeded")
}