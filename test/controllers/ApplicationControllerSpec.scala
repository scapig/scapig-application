package controllers

import models._
import models.JsonFormatters._
import models.RateLimitTier.BRONZE
import org.joda.time.{DateTime, DateTimeUtils}
import org.mockito.BDDMockito.given
import org.mockito.{ArgumentCaptor, Matchers}
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, verifyZeroInteractions}
import org.scalactic.Equality
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import services.ApplicationService
import utils.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class ApplicationControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterAll {

  val applicationUrls = ApplicationUrls(Seq("http://redirecturi"), "http://conditionUrl", "http://privacyUrl")
  val createAppRequest = CreateApplicationRequest("app name", "app description", applicationUrls,
    Set(Collaborator("admin@app.com", Role.ADMINISTRATOR)))

  trait Setup {
    val mockApplicationService: ApplicationService = mock[ApplicationService]
    val underTest = new ApplicationController(Helpers.stubControllerComponents(), mockApplicationService)

    val request = FakeRequest()

    given(mockApplicationService.createOrUpdate(any())).willAnswer(returnSame[Application])

    val application = Application(createAppRequest.name, createAppRequest.description,
      createAppRequest.collaborators, createAppRequest.applicationUrls)
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

    "succeed with a 200 with the application when payload is valid and service responds successfully" in new Setup {

      val result: Result = await(underTest.create()(request.withBody(Json.toJson(createAppRequest))))

      status(result) shouldBe Status.OK
      val createdApplication = jsonBodyOf(result).as[Application]
      createdApplication shouldBe application.copy(credentials = createdApplication.credentials, id = createdApplication.id)

      verify(mockApplicationService).createOrUpdate(createdApplication)
    }

    "fail with a 400 (Bad Request) when the json payload is invalid for the request" in new Setup {

      val body = """{ "invalid": "json" }"""

      val result: Result = await(underTest.create()(request.withBody(Json.parse(body))))

      status(result) shouldBe Status.BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse("""{"code":"INVALID_REQUEST","message":"description is required"}""")
      verifyZeroInteractions(mockApplicationService)
    }
  }

  "fetch" should {

    "succeed with a 200 (Ok) with the application when the application exists" in new Setup {

      given(mockApplicationService.fetch(application.id.toString))
        .willReturn(successful(Some(application)))

      val result: Result = await(underTest.fetch(application.id.toString)(request))

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe Json.toJson(application)
    }

    "fail with a 404 (Not Found) when the application does not exist" in new Setup {
      given(mockApplicationService.fetch(application.id.toString)).willReturn(successful(None))

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
