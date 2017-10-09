package repository

import java.util.UUID

import models._
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import utils.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionRepositorySpec extends UnitSpec with BeforeAndAfterEach {

  lazy val fakeApplication = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> "mongodb://localhost:27017/tapi-subscription-test")
    .build()

  lazy val underTest = fakeApplication.injector.instanceOf[SubscriptionRepository]

  val applicationId = UUID.randomUUID().toString
  val applicationId2 = UUID.randomUUID().toString
  val apiIdentifier = APIIdentifier("aContext", "aVersion")
  val apiIdentifier2 = APIIdentifier("aContext", "aVersion2")

  override def afterEach {
    await(underTest.repository).drop(failIfNotFound = false)
  }

  "isSubscribed" should {
    "return true when the API is subscribed" in {
      await(underTest.subscribe(applicationId, apiIdentifier))

      await(underTest.isSubscribed(applicationId, apiIdentifier)) shouldBe true
    }
    "return false when the API is not subscribed" in {
      await(underTest.isSubscribed(applicationId, apiIdentifier)) shouldBe false
    }
  }

  "subscribe" should {
    "create only one subscription record" in {
      await(underTest.subscribe(applicationId, apiIdentifier))
      await(underTest.subscribe(applicationId, apiIdentifier))
      await(underTest.unsubscribe(applicationId, apiIdentifier))

      await(underTest.isSubscribed(applicationId, apiIdentifier)) shouldBe false
    }
  }

  "unsubscribe" should {
    "unsubscribe only the selected version to the selected application" in {
      await(underTest.subscribe(applicationId, apiIdentifier))
      await(underTest.subscribe(applicationId2, apiIdentifier))
      await(underTest.subscribe(applicationId, apiIdentifier2))

      await(underTest.unsubscribe(applicationId, apiIdentifier))

      await(underTest.isSubscribed(applicationId, apiIdentifier)) shouldBe false
      await(underTest.isSubscribed(applicationId, apiIdentifier2)) shouldBe true
      await(underTest.isSubscribed(applicationId2, apiIdentifier)) shouldBe true
      await(underTest.isSubscribed(applicationId2, apiIdentifier2)) shouldBe false
    }
  }

  "getSubscriptions" should {
    "return the subscribed APIs" in {
      await(underTest.subscribe(applicationId, apiIdentifier))

      await(underTest.getSubscriptions(applicationId)) shouldBe Seq(apiIdentifier)
    }

    "return an empty list when there are no API subscribed for the application" in {
      await(underTest.subscribe(applicationId2, apiIdentifier))

      await(underTest.getSubscriptions(applicationId)) shouldBe Seq.empty
    }

  }
}