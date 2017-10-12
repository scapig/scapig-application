package services

import java.util.UUID

import models._
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import repository.{ApplicationRepository, SubscriptionRepository}
import utils.UnitSpec

import scala.concurrent.Future.{failed, successful}

class SubscriptionServiceSpec extends UnitSpec with MockitoSugar {

  val appId = UUID.randomUUID().toString
  val api = APIIdentifier("context", "version")
  val applicationUrls = ApplicationUrls(Seq("http://redirecturi"), "http://conditionUrl", "http://privacyUrl")
  val application = Application("app name", "app description", Set(Collaborator("admin@app.com", Role.ADMINISTRATOR)), applicationUrls)

  trait Setup {
    val applicationRepository = mock[ApplicationRepository]
    val subscriptionRepository = mock[SubscriptionRepository]

    val underTest = new SubscriptionService(applicationRepository, subscriptionRepository)
  }

  "subscribe" should {

    "fail with ApplicationNotFound when the application does not exist" in new Setup {
      given(applicationRepository.fetch(appId)).willReturn(failed(ApplicationNotFoundException()))

      intercept[ApplicationNotFoundException]{await(underTest.subscribe(appId, api))}
    }

    "subscribe to the API" in new Setup {
      given(applicationRepository.fetch(appId)).willReturn(successful(application))
      given(subscriptionRepository.subscribe(appId, api)).willReturn(successful(HasSucceeded))

      val result = await(underTest.subscribe(appId, api))

      result shouldBe HasSucceeded
      verify(subscriptionRepository).subscribe(appId, api)
    }

    "fail when the subscriptionService fails" in new Setup {
      given(applicationRepository.fetch(appId)).willReturn(successful(application))
      given(subscriptionRepository.subscribe(appId, api)).willReturn(failed(new RuntimeException()))

      intercept[RuntimeException]{await(underTest.subscribe(appId, api))}
    }
  }

  "unsubscribe" should {

    "usubscribe to the API" in new Setup {
      given(subscriptionRepository.unsubscribe(appId, api)).willReturn(successful(HasSucceeded))

      val result = await(underTest.unsubscribe(appId, api))

      result shouldBe HasSucceeded
      verify(subscriptionRepository).unsubscribe(appId, api)
    }

    "fail when the subscriptionService fails" in new Setup {
      given(subscriptionRepository.unsubscribe(appId, api)).willReturn(failed(new RuntimeException()))

      intercept[RuntimeException]{await(underTest.unsubscribe(appId, api))}
    }
  }

  "getSubscriptions" should {

    "return the APIs the application is subscribed to" in new Setup {
      given(applicationRepository.fetch(appId)).willReturn(successful(application))
      given(subscriptionRepository.getSubscriptions(appId)).willReturn(successful(Seq(api)))

      val result = await(underTest.getSubscriptions(appId))

      result shouldBe Seq(api)
    }

    "fail with ApplicationNotFound when the application does not exist" in new Setup {
      given(applicationRepository.fetch(appId)).willReturn(failed(ApplicationNotFoundException()))

      intercept[ApplicationNotFoundException]{await(underTest.getSubscriptions(appId))}
    }

    "fail when the subscriptionService fails" in new Setup {
      given(subscriptionRepository.getSubscriptions(appId)).willReturn(failed(new RuntimeException()))

      intercept[RuntimeException]{await(underTest.getSubscriptions(appId))}
    }
  }

  "isSubscribed" should {

    "return true when the application is subscribed to the API" in new Setup {
      given(subscriptionRepository.isSubscribed(appId, api)).willReturn(successful(true))

      val result = await(underTest.isSubscribed(appId, api.context, api.version))

      result shouldBe true
    }

    "return false when the application is not subscribed to the API" in new Setup {
      given(subscriptionRepository.isSubscribed(appId, api)).willReturn(successful(false))

      val result = await(underTest.isSubscribed(appId, api.context, api.version))

      result shouldBe false
    }
  }

}
