package tapi

import models.JsonFormatters._
import models._
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.mvc.Http.HeaderNames.CONTENT_TYPE

import scalaj.http.Http

class SubscriptionSpec extends BaseFeatureSpec {

  val redirectUris = Seq("http://redirecturi")
  val createAppRequest = CreateApplicationRequest("app name", "app description", redirectUris,
    Set(Collaborator("admin@app.com", Role.ADMINISTRATOR)))

  feature("subscribe and unsubscribe an application to an API") {

    scenario("subscribe to API") {

      Given("An application")
      val application = createApplication()

      When("I subscribe the application to an API")
      val createSubscriptionResponse = Http(s"$serviceUrl/application/${application.id}/subscription?context=hello&version=1.0")
        .headers(Seq(CONTENT_TYPE -> "application/json")).postForm.asString

      Then("I receive a 204 (NoContent)")
      createSubscriptionResponse.code shouldBe Status.NO_CONTENT

      And("I can retrieve the subscription")
      val fetchSubscriptionResponse = Http(s"$serviceUrl/application/${application.id}/subscription").asString
      fetchSubscriptionResponse.code shouldBe OK
      Json.parse(fetchSubscriptionResponse.body) shouldBe Json.toJson(Seq(APIIdentifier("hello", "1.0")))
    }

    scenario("unsubscribe to API") {
      Given("An application")
      val application = createApplication()

      And("The application is subscribe to an API")
      val createSubscriptionResponse = Http(s"$serviceUrl/application/${application.id}/subscription?context=hello&version=1.0")
        .headers(Seq(CONTENT_TYPE -> "application/json")).postForm.asString

      When("I unsubscribe the application to an API")
      val removeSubscriptionResponse = Http(s"$serviceUrl/application/${application.id}/subscription?context=hello&version=1.0")
        .headers(Seq(CONTENT_TYPE -> "application/json")).method("DELETE").asString

      Then("I receive a 204 (NoContent)")
      removeSubscriptionResponse.code shouldBe Status.NO_CONTENT

      And("The application is no longer subscribed to the API")
      val fetchSubscriptionResponse = Http(s"$serviceUrl/application/${application.id}/subscription").asString
      fetchSubscriptionResponse.code shouldBe OK
      Json.parse(fetchSubscriptionResponse.body) shouldBe Json.toJson(Seq[APIIdentifier]())

    }
  }

  private def createApplication(): Application = {
    val createdResponse = Http(s"$serviceUrl/application")
      .headers(Seq(CONTENT_TYPE -> "application/json"))
      .postData(Json.toJson(createAppRequest).toString()).asString
    Json.parse(createdResponse.body).as[Application]
  }
}