package repository

import javax.inject.{Inject, Singleton}

import models.{APIIdentifier, Application, HasSucceeded}
import play.api.libs.json.{JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection
import models.JsonFormatters._
import reactivemongo.play.json.ImplicitBSONHandlers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SubscriptionRepository @Inject()(val reactiveMongoApi: ReactiveMongoApi)  {

  def repository: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("tapi-subscription"))

  def subscribe(applicationId: String, apiIdentifier: APIIdentifier): Future[HasSucceeded] = {
    repository.flatMap(collection =>
      collection.update(
        Json.obj("appId" -> applicationId),
        Json.obj("$addToSet" -> Json.obj("api" -> apiIdentifier)),
        upsert = true
      ) map (_ => HasSucceeded)
    )
  }

  def unsubscribe(applicationId: String, apiIdentifier: APIIdentifier): Future[HasSucceeded] = {
    repository.flatMap(collection =>
      collection.update(
        Json.obj("appId" -> applicationId),
        Json.obj("$pull" -> Json.obj("api" -> apiIdentifier)),
        upsert = true
      ) map (_ => HasSucceeded)
    )
  }

  def isSubscribed(applicationId: String, apiIdentifier: APIIdentifier): Future[Boolean] = {
    repository.flatMap(collection =>
      collection.count(Some(Json.obj("$and" -> Json.arr(
        Json.obj("appId" -> applicationId.toString),
        Json.obj("api.context" -> apiIdentifier.context),
        Json.obj("api.version" -> apiIdentifier.version))))) map {
        case 1 => true
        case _ => false
      }
    )
  }

  def getSubscriptions(applicationId: String): Future[Seq[APIIdentifier]] = {
    repository.flatMap(collection =>
      collection.find(Json.obj("appId" -> applicationId)).one[JsValue] map {
        _.fold(Seq[APIIdentifier]()) { d => (d \ "api").as[Seq[APIIdentifier]]}
      }
    )
  }
}

