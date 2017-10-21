package services

import models._
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import repository.ApplicationRepository
import utils.UnitSpec

import scala.concurrent.Future.{failed, successful}

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  val applicationUrls = ApplicationUrls(Seq("http://redirecturi"), "http://conditionUrl", "http://privacyUrl")
  val application = Application("app name", "app description", Set(Collaborator("admin@app.com", Role.ADMINISTRATOR)), applicationUrls)
  val productionClientId = application.credentials.production.clientId
  val sandboxClientId = application.credentials.sandbox.clientId
  val productionServerToken = application.credentials.production.serverToken
  val sandboxServerToken = application.credentials.sandbox.serverToken

  trait Setup {
    val mockApplicationRepository = mock[ApplicationRepository]
    val underTest = new ApplicationService(mockApplicationRepository)

    when(mockApplicationRepository.save(any())).thenAnswer(returnSame[Application])
  }

  "createOrUpdate" should {

    "save the application in the repository" in new Setup {
      val result = await(underTest.createOrUpdate(application))

      result shouldBe application
      verify(mockApplicationRepository).save(application)
    }

    "fail when the repository fails" in new Setup {

      given(mockApplicationRepository.save(application)).willReturn(failed(new RuntimeException("Error message")))

      intercept[RuntimeException] {
        await(underTest.createOrUpdate(application))
      }
    }
  }

  "fetch" should {
    "return the application when it exists" in new Setup {
      given(mockApplicationRepository.fetch(application.id.toString)).willReturn(successful(application))

      val result = await(underTest.fetch(application.id.toString))

      result shouldBe application
    }

    "propagate ApplicationNotFoundException when the application does not exist" in new Setup {
      given(mockApplicationRepository.fetch(application.id.toString)).willReturn(failed(ApplicationNotFoundException()))

      intercept[ApplicationNotFoundException]{await(underTest.fetch(application.id.toString))}
    }

    "fail when the repository fails" in new Setup {
      given(mockApplicationRepository.fetch(application.id.toString)).willReturn(failed(new RuntimeException("Error message")))

      intercept[RuntimeException] {
        await(underTest.fetch(application.id.toString))
      }
    }
  }

  "fetchByClientId" should {
    "return the environement application when it exists" in new Setup {
      val environmentApplication = EnvironmentApplication(application.id, productionClientId, application.name, Environment.PRODUCTION,
        application.description, application.rateLimitTier, application.applicationUrls)

      given(mockApplicationRepository.fetchByClientId(productionClientId)).willReturn(successful(Some(application)))

      val result = await(underTest.fetchByClientId(productionClientId))

      result shouldBe Some(environmentApplication)
    }

    "return None when the application does not exist" in new Setup {
      given(mockApplicationRepository.fetchByClientId(productionClientId)).willReturn(successful(None))

      val result = await(underTest.fetchByClientId(productionClientId))

      result shouldBe None
    }

    "fail when the repository fails" in new Setup {
      given(mockApplicationRepository.fetchByClientId(productionClientId)).willReturn(failed(new RuntimeException("Error message")))

      intercept[RuntimeException] {
        await(underTest.fetchByClientId(productionClientId))
      }
    }
  }

  "fetchByServerToken" should {
    "return the environment application when it exists" in new Setup {
      val environmentApplication = EnvironmentApplication(application.id, productionClientId, application.name, Environment.PRODUCTION,
        application.description, application.rateLimitTier, application.applicationUrls)

      given(mockApplicationRepository.fetchByServerToken(productionServerToken)).willReturn(successful(application))

      val result = await(underTest.fetchByServerToken(productionServerToken))

      result shouldBe environmentApplication
    }

    "propage ApplicationNotFoundException when the application does not exist" in new Setup {
      given(mockApplicationRepository.fetchByServerToken(productionServerToken)).willReturn(failed(ApplicationNotFoundException()))

      intercept[ApplicationNotFoundException]{await(underTest.fetchByServerToken(productionServerToken))}
    }

    "fail when the repository fails" in new Setup {
      given(mockApplicationRepository.fetchByServerToken(productionServerToken)).willReturn(failed(new RuntimeException("Error message")))

      intercept[RuntimeException] {
        await(underTest.fetchByServerToken(productionServerToken))
      }
    }
  }

  "fetchAllByCollaboratorEmail" should {
    "return the users applications" in new Setup {
      val collaboratorEmail = "collaborator@gmail.com"

      given(mockApplicationRepository.fetchAllByCollaboratorEmail(collaboratorEmail)).willReturn(successful(Seq(application)))

      val result = await(underTest.fetchAllByCollaboratorEmail(collaboratorEmail))

      result shouldBe Seq(application)
    }
  }

  "authenticate" should {
    "return the production application when the clientId and secret are correct" in new Setup {
      val productionApplication = EnvironmentApplication(application.id, productionClientId, application.name, Environment.PRODUCTION,
        application.description, application.rateLimitTier, application.applicationUrls)

      given(mockApplicationRepository.fetchByClientId(productionClientId)).willReturn(successful(Some(application)))

      val result = await(underTest.authenticate(
        AuthenticateRequest(application.credentials.production.clientId,  application.credentials.production.clientSecrets.head.secret)))

      result shouldBe Some(productionApplication)
    }

    "return the sandbox application when the clientId and secret are correct" in new Setup {
      val sandboxApplication = EnvironmentApplication(application.id, sandboxClientId, application.name, Environment.SANDBOX,
        application.description, application.rateLimitTier, application.applicationUrls)

      given(mockApplicationRepository.fetchByClientId(sandboxClientId)).willReturn(successful(Some(application)))

      val result = await(underTest.authenticate(
        AuthenticateRequest(application.credentials.sandbox.clientId,  application.credentials.sandbox.clientSecrets.head.secret)))

      result shouldBe Some(sandboxApplication)
    }

    "return None when the secret is correct" in new Setup {
      given(mockApplicationRepository.fetchByClientId(productionClientId)).willReturn(successful(Some(application)))

      val result = await(underTest.authenticate(
        AuthenticateRequest(application.credentials.production.clientId,  "invalidSecret")))

      result shouldBe None
    }

    "return None when the clientId is correct" in new Setup {
      given(mockApplicationRepository.fetchByClientId("invalidClientId")).willReturn(successful(None))

      val result = await(underTest.authenticate(
        AuthenticateRequest("invalidClientId",  application.credentials.production.clientSecrets.head.secret)))

      result shouldBe None
    }

  }
}