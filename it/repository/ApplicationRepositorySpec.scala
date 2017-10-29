package repository

import java.util.UUID

import models._
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import utils.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationRepositorySpec extends UnitSpec with BeforeAndAfterEach {

  val user = "admin@app.com"
  val redirectUris = Seq("http://redirecturi")
  val application = Application("app name", "app description", Set(Collaborator(user, Role.ADMINISTRATOR)), redirectUris)

  lazy val fakeApplication = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> "mongodb://localhost:27017/tapi-application-test")
    .build()

  lazy val underTest = fakeApplication.injector.instanceOf[ApplicationRepository]

  override def afterEach {
    await(await(underTest.repository).drop(failIfNotFound = false))
  }

  "createOrUpdate" should {
    "create a new application" in {
      await(underTest.save(application))

      await(underTest.fetch(application.id.toString())) shouldBe application
    }

    "update an existing application" in {
      val updatedApplication = application.copy(name = "updated name")
      await(underTest.save(application))

      await(underTest.save(updatedApplication))

      await(underTest.fetch(application.id.toString())) shouldBe updatedApplication
    }

  }

  "fetchByClientId" should {
    "return the application if the sandbox clientId matches" in {
      await(underTest.save(application))

      await(underTest.fetchByClientId(application.credentials.sandbox.clientId)) shouldBe Some(application)
    }

    "return the application if the production clientId matches" in {
      await(underTest.save(application))

      await(underTest.fetchByClientId(application.credentials.production.clientId)) shouldBe Some(application)
    }

    "return None when no clientId matches" in {
      await(underTest.save(application))

      await(underTest.fetchByClientId("anotherClientId")) shouldBe None
    }

  }

  "fetchAllByCollaboratorEmail" should {
    val application2 = application.copy(id = UUID.randomUUID())
    val otherUserApplication = application.copy(id = UUID.randomUUID(), collaborators = Set(Collaborator("another@gmail.com", Role.ADMINISTRATOR)))

    "return all the user applications" in {
      await(underTest.save(application))
      await(underTest.save(application2))
      await(underTest.save(otherUserApplication))

      await(underTest.fetchAllByCollaboratorEmail(user)) shouldBe Seq(application, application2)
    }

    "return an empty list when no application match" in {
      await(underTest.fetchAllByCollaboratorEmail(user)) shouldBe Seq()
    }
  }

  "fetchByServerToken" should {
    "return the application if the sandbox serverToken matches" in {
      await(underTest.save(application))

      await(underTest.fetchByServerToken(application.credentials.sandbox.serverToken)) shouldBe application
    }

    "return the application if the production serverToken matches" in {
      await(underTest.save(application))

      await(underTest.fetchByServerToken(application.credentials.production.serverToken)) shouldBe application
    }

    "fail with ApplicationNotFoundException if no serverToken matches" in {
      await(underTest.save(application))

      intercept[ApplicationNotFoundException]{await(underTest.fetchByServerToken("anotherServerToken"))}
    }

  }

}