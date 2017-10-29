package tapi

import models._
import models.JsonFormatters._
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.mvc.Http.HeaderNames.CONTENT_TYPE

import scalaj.http.Http

class ApplicationSpec extends BaseFeatureSpec {

  val collaboratorEmail = "admin@app.com"
  val applicationUrls = Seq("http://redirecturi")
  val createAppRequest = CreateApplicationRequest("app name", "app description", applicationUrls,
    Set(Collaborator(collaboratorEmail, Role.ADMINISTRATOR)))

  feature("create and fetch application") {

    scenario("happy path") {

      When("An application create request is received")
      val createdResponse = Http(s"$serviceUrl/application")
        .headers(Seq(CONTENT_TYPE -> "application/json"))
        .postData(Json.toJson(createAppRequest).toString()).asString

      Then("I receive a 201 (Ok) with the new application")
      createdResponse.code shouldBe Status.CREATED
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
      val fetchResponse = Http(s"$serviceUrl/application?clientId=${application.credentials.production.clientId}").asString

      Then("I receive a 200 (Ok) with the environment application")
      fetchResponse.code shouldBe OK
      Json.parse(fetchResponse.body) shouldBe Json.toJson(EnvironmentApplication(application.id, application.credentials.production.clientId,
        application.name, Environment.PRODUCTION, application.description, application.rateLimitTier, application.redirectUris))
    }

    scenario("fetch by sandbox clientId") {
      Given("An application")
      val application = createApplication()

      When("I fetch the application by sandbox clientId")
      val fetchResponse = Http(s"$serviceUrl/application?clientId=${application.credentials.sandbox.clientId}").asString

      Then("I receive a 200 (Ok) with the environment application")
      fetchResponse.code shouldBe OK
      Json.parse(fetchResponse.body) shouldBe Json.toJson(EnvironmentApplication(application.id, application.credentials.sandbox.clientId,
        application.name, Environment.SANDBOX, application.description, application.rateLimitTier, application.redirectUris))
    }
  }

  feature("Authenticate") {
    scenario("production credentials") {

      Given("An application")
      val application = createApplication()

      When("I validate the production credentials")
      val authenticationRequest = AuthenticateRequest(application.credentials.production.clientId, application.credentials.production.clientSecrets.head.secret)
      val fetchResponse = Http(s"$serviceUrl/application/authenticate")
        .headers(Seq(CONTENT_TYPE -> "application/json"))
        .postData(Json.toJson(authenticationRequest).toString).asString

      Then("I receive a 200 (Ok) with the environment application")
      fetchResponse.code shouldBe OK
      Json.parse(fetchResponse.body) shouldBe Json.toJson(EnvironmentApplication(application.id, application.credentials.production.clientId,
        application.name, Environment.PRODUCTION, application.description, application.rateLimitTier, application.redirectUris))
    }

    scenario("sandbox credentials") {

      Given("An application")
      val application = createApplication()

      When("I validate the sandbox credentials")
      val authenticationRequest = AuthenticateRequest(application.credentials.sandbox.clientId, application.credentials.sandbox.clientSecrets.head.secret)
      val fetchResponse = Http(s"$serviceUrl/application/authenticate")
        .headers(Seq(CONTENT_TYPE -> "application/json"))
        .postData(Json.toJson(authenticationRequest).toString).asString

      Then("I receive a 200 (Ok) with the environment application")
      fetchResponse.code shouldBe OK
      Json.parse(fetchResponse.body) shouldBe Json.toJson(EnvironmentApplication(application.id, application.credentials.sandbox.clientId,
        application.name, Environment.SANDBOX, application.description, application.rateLimitTier, application.redirectUris))

    }

    scenario("invalid credentials") {

      Given("An application")
      val application = createApplication()

      When("I validate with invalid credentials")
      val authenticationRequest = AuthenticateRequest(application.credentials.sandbox.clientId, application.credentials.production.clientSecrets.head.secret)
      val fetchResponse = Http(s"$serviceUrl/application/authenticate")
        .headers(Seq(CONTENT_TYPE -> "application/json"))
        .postData(Json.toJson(authenticationRequest).toString).asString

      Then("I receive a 401 (Unauthorized)")
      fetchResponse.code shouldBe Status.UNAUTHORIZED
    }

  }

  feature("fetch by collaborator") {
    scenario("return the user's applications") {

      Given("An application where the user is a collaborator")
      val application = createApplication(createAppRequest)

      Given("An application where the user is not a collaborator")
      val anotherApp = createApplication(createAppRequest.copy(collaborators = Set(Collaborator("anotheruser@gmail.com", Role.ADMINISTRATOR))))

      When("I fetch the user's applications")
      val fetchResponse = Http(s"$serviceUrl/applications?collaboratorEmail=$collaboratorEmail").asString

      Then("I receive a 200 (OK) with the application for which the user is a collaborator")
      fetchResponse.code shouldBe Status.OK
      Json.parse(fetchResponse.body) shouldBe Json.toJson(Seq(application))
    }

  }

  private def createApplication(applicationRequest: CreateApplicationRequest = createAppRequest): Application = {
    val createdResponse = Http(s"$serviceUrl/application")
      .headers(Seq(CONTENT_TYPE -> "application/json"))
      .postData(Json.toJson(applicationRequest).toString()).asString
    Json.parse(createdResponse.body).as[Application]
  }
}