package repository

import models._
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import utils.UnitSpec
import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationRepositorySpec extends UnitSpec with BeforeAndAfterEach {

  val applicationUrls = ApplicationUrls(Seq("http://redirecturi"), "http://conditionUrl", "http://privacyUrl")
  val application = Application("app name", "app description", Set(Collaborator("admin@app.com", Role.ADMINISTRATOR)), applicationUrls)

  lazy val fakeApplication = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> "mongodb://localhost:27017/tapi-delegated-authority-test")
    .build()

  lazy val underTest = fakeApplication.injector.instanceOf[ApplicationRepository]

  override def afterEach {
    await(underTest.repository).drop(failIfNotFound = false)
  }

  "createOrUpdate" should {
    "create a new delegated authority" in {
      await(underTest.save(application))

      await(underTest.fetch(application.id.toString())) shouldBe Some(application)
    }

    "update an existing delegated authority" in {
      val updatedApplication = application.copy(name = "updated name")
      await(underTest.save(application))

      await(underTest.save(updatedApplication))

      await(underTest.fetch(application.id.toString())) shouldBe Some(updatedApplication)
    }

  }

}
