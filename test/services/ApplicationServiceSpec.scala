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
  val productionClientId = application.tokens.production.clientId

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
      given(mockApplicationRepository.fetch(application.id.toString)).willReturn(successful(Some(application)))

      val result = await(underTest.fetch(application.id.toString))

      result shouldBe Some(application)
    }

    "return None when the application does not exist" in new Setup {
      given(mockApplicationRepository.fetch(application.id.toString)).willReturn(successful(None))

      val result = await(underTest.fetch(application.id.toString))

      result shouldBe None
    }

    "fail when the repository fails" in new Setup {
      given(mockApplicationRepository.fetch(application.id.toString)).willReturn(failed(new RuntimeException("Error message")))

      intercept[RuntimeException] {
        await(underTest.fetch(application.id.toString))
      }
    }
  }

  "fetchByClientId" should {
    "return the application when it exists" in new Setup {
      given(mockApplicationRepository.fetchByClientId(productionClientId)).willReturn(successful(Some(application)))

      val result = await(underTest.fetchByClientId(productionClientId))

      result shouldBe Some(application)
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

}