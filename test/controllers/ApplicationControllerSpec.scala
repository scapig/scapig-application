package controllers

import models.JsonFormatters._
import models._
import org.joda.time.DateTimeUtils
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verifyZeroInteractions
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import services.ApplicationService
import utils.UnitSpec

import scala.concurrent.Future.{failed, successful}

class ApplicationControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterAll {

  val collaboratorEmail = "admin@app.com"
  val redirectUris = Seq("http://redirecturi")
  val createAppRequest = CreateApplicationRequest("app name", "app description", redirectUris,
    Set(Collaborator(collaboratorEmail, Role.ADMINISTRATOR)))

  val updateAppRequest = UpdateApplicationRequest("updated name", "updated description", Seq("http://updatedRedirectUri"), RateLimitTier.SILVER)

  trait Setup {
    val mockApplicationService: ApplicationService = mock[ApplicationService]
    val underTest = new ApplicationController(Helpers.stubControllerComponents(), mockApplicationService)

    val request = FakeRequest()

    val application = Application(createAppRequest.name, createAppRequest.description,
      createAppRequest.collaborators, createAppRequest.redirectUris)
    val productionClientId = application.credentials.production.clientId
    val environmentApplication = EnvironmentApplication(productionClientId, application)
    val productionServerToken = application.credentials.production.serverToken
  }

  override def beforeAll {
    DateTimeUtils.setCurrentMillisFixed(10000)
  }

  override def afterAll {
    DateTimeUtils.setCurrentMillisSystem()
  }

  "create" should {

    "succeed with a 201 (Created) with the application when payload is valid and service responds successfully" in new Setup {
      given(mockApplicationService.create(createAppRequest)).willReturn(successful(application))

      val result: Result = await(underTest.create()(request.withBody(Json.toJson(createAppRequest))))

      status(result) shouldBe Status.CREATED
      jsonBodyOf(result).as[Application] shouldBe application
    }

    "fail with a 400 (Bad Request) when the json payload is invalid for the request" in new Setup {

      val body = """{ "invalid": "json" }"""

      val result: Result = await(underTest.create()(request.withBody(Json.parse(body))))

      status(result) shouldBe Status.BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse("""{"code":"INVALID_REQUEST","message":"description is required"}""")
      verifyZeroInteractions(mockApplicationService)
    }
  }

  "update" should {

    "succeed with a 200 (Ok) with the application when payload is valid and service responds successfully" in new Setup {
      given(mockApplicationService.update(application.id.toString, updateAppRequest)).willReturn(successful(application))

      val result: Result = await(underTest.update(application.id.toString)(request.withBody(Json.toJson(updateAppRequest))))

      status(result) shouldBe Status.OK
      jsonBodyOf(result).as[Application] shouldBe application
    }

    "fail with a 400 (Bad Request) when the json payload is invalid for the request" in new Setup {

      val body = """{ "invalid": "json" }"""

      val result: Result = await(underTest.update(application.id.toString)(request.withBody(Json.parse(body))))

      status(result) shouldBe Status.BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse("""{"code":"INVALID_REQUEST","message":"description is required"}""")
      verifyZeroInteractions(mockApplicationService)
    }
  }

  "fetch" should {

    "succeed with a 200 (Ok) with the application when the application exists" in new Setup {

      given(mockApplicationService.fetch(application.id.toString))
        .willReturn(successful(application))

      val result: Result = await(underTest.fetch(application.id.toString)(request))

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe Json.toJson(application)
    }

    "fail with a 404 (Not Found) when the application does not exist" in new Setup {
      given(mockApplicationService.fetch(application.id.toString)).willReturn(failed(ApplicationNotFoundException()))

      val result: Result = await(underTest.fetch(application.id.toString)(request))

      status(result) shouldBe Status.NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code": "NOT_FOUND", "message": "no application found for id ${application.id}"}""")
    }
  }

  "queryDispatcher" should {

    "succeed with a 200 (Ok) with the environment application when the application exists for a clientId" in new Setup {

      given(mockApplicationService.fetchByClientId(productionClientId))
        .willReturn(successful(Some(environmentApplication)))

      val result: Result = await(underTest.queryDispatcher()(FakeRequest("GET", s"?clientId=$productionClientId")))

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe Json.toJson(environmentApplication)
    }

    "fail with a 404 (Not Found) when the application does not exist for a clientId" in new Setup {
      given(mockApplicationService.fetchByClientId("anotherClientId")).willReturn(successful(None))

      val result: Result = await(underTest.queryDispatcher()(FakeRequest("GET", s"?clientId=anotherClientId")))

      status(result) shouldBe Status.NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code": "NOT_FOUND", "message": "no application found for clientId anotherClientId"}""")
    }

    "succeed with a 200 (Ok) with the environment application when the application exists for a serverToken" in new Setup {

      given(mockApplicationService.fetchByServerToken(productionServerToken))
        .willReturn(successful(environmentApplication))

      val result: Result = await(underTest.queryDispatcher()(FakeRequest("GET", s"?serverToken=$productionServerToken")))

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe Json.toJson(environmentApplication)
    }

    "fail with a 404 (Not Found) when the application does not exist for a serverToken" in new Setup {
      given(mockApplicationService.fetchByServerToken("anotherServerToken")).willReturn(failed(ApplicationNotFoundException()))

      val result: Result = await(underTest.queryDispatcher()(FakeRequest("GET", s"?serverToken=anotherServerToken")))

      status(result) shouldBe Status.NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code": "NOT_FOUND", "message": "no application found for serverToken anotherServerToken"}""")
    }

  }

  "fetchAllByCollaboratorEmail" should {

    "succeed with a 200 (Ok) with the user's application" in new Setup {

      given(mockApplicationService.fetchAllByCollaboratorEmail(collaboratorEmail))
        .willReturn(successful(Seq(application)))

      val result: Result = await(underTest.fetchAllByCollaboratorEmail(collaboratorEmail)(FakeRequest()))

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe Json.toJson(Seq(application))
    }
  }

  "authenticate" should {
    val authenticateRequest = AuthenticateRequest("clientId", "clientSecret")

    "succeed with a 200 (Ok) with the environment application when the credentials are valid" in new Setup {

      given(mockApplicationService.authenticate(authenticateRequest))
        .willReturn(successful(Some(environmentApplication)))

      val result: Result = await(underTest.authenticate()(request.withBody(Json.toJson(authenticateRequest))))

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe Json.toJson(environmentApplication)
    }

    "fail with a 401 (Unauthorized) when the credentials are invalid" in new Setup {
      given(mockApplicationService.authenticate(authenticateRequest))
        .willReturn(successful(None))

      val result: Result = await(underTest.authenticate()(request.withBody(Json.toJson(authenticateRequest))))

      status(result) shouldBe Status.UNAUTHORIZED
    }
  }
}
