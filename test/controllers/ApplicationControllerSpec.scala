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

import scala.concurrent.Future.successful

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
    val productionClientId = application.tokens.production.clientId

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
      createdApplication shouldBe application.copy(tokens = createdApplication.tokens, id = createdApplication.id)

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

    "fail with a 404 (Not Found) when the api-definition does not exist" in new Setup {
      given(mockApplicationService.fetch(application.id.toString)).willReturn(successful(None))

      val result: Result = await(underTest.fetch(application.id.toString)(request))

      status(result) shouldBe Status.NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code": "NOT_FOUND", "message": "no application found for id ${application.id}"}""")
    }
  }

  "fetchByClientId" should {

    "succeed with a 200 (Ok) with the application when the application exists" in new Setup {

      given(mockApplicationService.fetchByClientId(productionClientId))
        .willReturn(successful(Some(application)))

      val result: Result = await(underTest.fetchByClientId(productionClientId)(request))

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe Json.toJson(application)
    }

    "fail with a 404 (Not Found) when the api-definition does not exist" in new Setup {
      given(mockApplicationService.fetchByClientId("anotherClientId")).willReturn(successful(None))

      val result: Result = await(underTest.fetchByClientId("anotherClientId")(request))

      status(result) shouldBe Status.NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code": "NOT_FOUND", "message": "no application found for clientId anotherClientId"}""")
    }
  }

}
