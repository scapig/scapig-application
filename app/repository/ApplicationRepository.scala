package repository

import javax.inject.{Inject, Singleton}

import models.{Application, ApplicationNotFoundException}
import models.JsonFormatters._
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ApplicationRepository @Inject()(val reactiveMongoApi: ReactiveMongoApi)  {

  def repository: Future[JSONCollection] =
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

  def fetch(id: String): Future[Application] = {
    repository.flatMap(collection =>
      collection.find(Json.obj("id"-> id)).one[Application] map {
        case Some(app) => app
        case _ => throw ApplicationNotFoundException()
      }
    )
  }

  def fetchAllByCollaboratorEmail(collaboratorEmail: String): Future[Seq[Application]] = {
    repository.flatMap(collection =>
      collection.find(Json.obj("collaborators.emailAddress" -> collaboratorEmail))
        .cursor[Application]().collect(100, FailOnError[Seq[Application]]())
    )
  }

  def fetchByClientId(clientId: String): Future[Option[Application]] = {
    repository.flatMap(collection =>
      collection.find(Json.obj("$or"-> Json.arr(
        Json.obj("credentials.production.clientId" -> clientId),
        Json.obj("credentials.sandbox.clientId" -> clientId)
      ))).one[Application]
    )
  }

  def fetchByServerToken(serverToken: String): Future[Application] = {
    repository.flatMap(collection =>
      collection.find(Json.obj("$or"-> Json.arr(
        Json.obj("credentials.production.serverToken" -> serverToken),
        Json.obj("credentials.sandbox.serverToken" -> serverToken)
      ))).one[Application] map {
        case Some(app) => app
        case None => throw ApplicationNotFoundException()
      }
    )
  }

  private def createIndex(field: String, indexName: String): Future[WriteResult] = {
    repository.flatMap(collection =>
      collection.indexesManager.create(Index(Seq((field, IndexType.Ascending)), Some(indexName)))
    )
  }

  private def ensureIndexes() = {
    Future.sequence(Seq(
      createIndex("idName", "idIndex"),
      createIndex("credentials.production.serverToken", "productionServerTokenIndex"),
      createIndex("credentials.sandbox.serverToken", "sandboxServerTokenIndex"),
      createIndex("credentials.production.clientId", "productionClientIdIndex"),
      createIndex("credentials.sandbox.clientId", "sandboxClientIdIndex"),
    ))
  }

  ensureIndexes()

}
