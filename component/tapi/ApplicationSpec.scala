package tapi

import models._
import models.JsonFormatters._
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.mvc.Http.HeaderNames.CONTENT_TYPE

import scalaj.http.Http

class ApplicationSpec extends BaseFeatureSpec {

  val applicationUrls = ApplicationUrls(Seq("http://redirecturi"), "http://conditionUrl", "http://privacyUrl")
  val createAppRequest = CreateApplicationRequest("app name", "app description", applicationUrls,
    Set(Collaborator("admin@app.com", Role.ADMINISTRATOR)))

  feature("create and fetch api definition") {

    scenario("happy path") {

      When("An application create request is received")
      val createdResponse = Http(s"$serviceUrl/application")
        .headers(Seq(CONTENT_TYPE -> "application/json"))
        .postData(Json.toJson(createAppRequest).toString()).asString

      Then("I receive a 200 (Ok) with the new application")
      createdResponse.code shouldBe OK
      val newApplication = Json.parse(createdResponse.body).as[Application]

      And("The application can be retrieved")
      val fetchResponse = Http(s"$serviceUrl/application/${newApplication.id}").asString
      fetchResponse.code shouldBe OK
      Json.parse(fetchResponse.body) shouldBe Json.toJson(newApplication)
    }

    scenario("fetch by production clientId") {
      Given("An application")
      val application = createApplication()

      When("I fetch the application by production clientId")
      val fetchResponse = Http(s"$serviceUrl/application?clientId=${application.tokens.production.clientId}").asString

      Then("I receive a 200 (Ok) with the application")
      fetchResponse.code shouldBe OK
      Json.parse(fetchResponse.body) shouldBe Json.toJson(application)
    }

    scenario("fetch by sandbox clientId") {
      Given("An application")
      val application = createApplication()

      When("I fetch the application by sandbox clientId")
      val fetchResponse = Http(s"$serviceUrl/application?clientId=${application.tokens.sandbox.clientId}").asString

      Then("I receive a 200 (Ok) with the application")
      fetchResponse.code shouldBe OK
      Json.parse(fetchResponse.body) shouldBe Json.toJson(application)
    }
  }

  private def createApplication(): Application = {
    val createdResponse = Http(s"$serviceUrl/application")
      .headers(Seq(CONTENT_TYPE -> "application/json"))
      .postData(Json.toJson(createAppRequest).toString()).asString
    Json.parse(createdResponse.body).as[Application]
  }
}