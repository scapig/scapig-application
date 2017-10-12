package controllers

import java.util.UUID

import models.{APIIdentifier, ApplicationNotFoundException, HasSucceeded}
import org.mockito.BDDMockito.given
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import services.SubscriptionService
import utils.UnitSpec
import models.JsonFormatters._

import scala.concurrent.Future

class SubscriptionControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterAll {

  val applicationId = UUID.randomUUID().toString
  val api = APIIdentifier("context", "version")

  trait Setup {
    val subscriptionService = mock[SubscriptionService]
    val request = FakeRequest()

    val underTest = new SubscriptionController(Helpers.stubControllerComponents(), subscriptionService)
  }

  "getSubscriptions" should {
    "return the subscribed apis" in new Setup {
      given(subscriptionService.getSubscriptions(applicationId)).willReturn(Future.successful(Seq(api)))

      val result = await(underTest.getSubscriptions(applicationId)(request))

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe Json.toJson(Seq(api))
    }

    "return a 404 when the application does not exist" in new Setup {
      given(subscriptionService.getSubscriptions(applicationId)).willReturn(Future.failed(ApplicationNotFoundException()))

      val result = await(underTest.getSubscriptions(applicationId)(request))

      status(result) shouldBe Status.NOT_FOUND
      jsonBodyOf(result) shouldBe Json.obj("code" -> "NOT_FOUND", "message" -> s"no application found for id $applicationId")
    }
  }

  "subscribe" should {
    "subscribe the api to the application" in new Setup {
      given(subscriptionService.subscribe(applicationId, api)).willReturn(Future.successful(HasSucceeded))

      val result = await(underTest.subscribe(applicationId, api.context, api.version)(request))

      status(result) shouldBe Status.NO_CONTENT
    }

    "fail with a 404 when the application does not exist" in new Setup {
      given(subscriptionService.subscribe(applicationId, api)).willReturn(Future.failed(ApplicationNotFoundException()))

      val result = await(underTest.subscribe(applicationId, api.context, api.version)(request))

      status(result) shouldBe Status.NOT_FOUND
      jsonBodyOf(result) shouldBe Json.obj("code" -> "NOT_FOUND", "message" -> s"no application found for id $applicationId")
    }
  }

  "unsubscribe" should {
    "unsubscribe the api to the application" in new Setup {
      given(subscriptionService.unsubscribe(applicationId, api)).willReturn(Future.successful(HasSucceeded))

      val result = await(underTest.unsubscribe(applicationId, api.context, api.version)(request))

      status(result) shouldBe Status.NO_CONTENT
    }

    "fail with a 404 when the application does not exist" in new Setup {
      given(subscriptionService.unsubscribe(applicationId, api)).willReturn(Future.failed(ApplicationNotFoundException()))

      val result = await(underTest.unsubscribe(applicationId, api.context, api.version)(request))

      status(result) shouldBe Status.NOT_FOUND
      jsonBodyOf(result) shouldBe Json.obj("code" -> "NOT_FOUND", "message" -> s"no application found for id $applicationId")
    }

  }

  "validateSubscription" should {
    "return 204 (NoContent) when the application is subscribed to the API" in new Setup {
      given(subscriptionService.isSubscribed(applicationId, api.context, api.version)).willReturn(Future.successful(true))

      val result = await(underTest.validateSubscription(applicationId, api.context, api.version)(request))

      status(result) shouldBe Status.NO_CONTENT
    }

    "return 404 (NotFound) when the application is not subscribed to the API" in new Setup {
      given(subscriptionService.isSubscribed(applicationId, api.context, api.version)).willReturn(Future.successful(false))

      val result = await(underTest.validateSubscription(applicationId, api.context, api.version)(request))

      status(result) shouldBe Status.NOT_FOUND
    }

  }

}