package repository

import javax.inject.{Inject, Singleton}

import models.Application
import models.JsonFormatters._
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ApplicationRepository @Inject()(val reactiveMongoApi: ReactiveMongoApi)  {

  val repository: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("tapi-application"))

  def save(application: Application): Future[Application] = {
    repository.flatMap(collection =>
      collection.update(
        Json.obj("id"-> application.id), application, upsert = true) map {
        case result: UpdateWriteResult if result.ok => application
        case error => throw new RuntimeException(s"Failed to save application ${error.errmsg}")
      }
    )
  }

  def fetch(id: String): Future[Option[Application]] = {
    repository.flatMap(collection =>
      collection.find(Json.obj("id"-> id)).one[Application]
    )
  }

  def fetchByClientId(clientId: String): Future[Option[Application]] = {
    repository.flatMap(collection =>
      collection.find(Json.obj("$or"-> Json.arr(
        Json.obj("tokens.production.clientId" -> clientId),
        Json.obj("tokens.sandbox.clientId" -> clientId)
      ))).one[Application]
    )
  }

}
