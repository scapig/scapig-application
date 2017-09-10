package services

import javax.inject.{Inject, Singleton}

import models.Application
import repository.ApplicationRepository

import scala.concurrent.Future

@Singleton
class ApplicationService @Inject()(applicationRepository: ApplicationRepository) {

  def fetch(id: String): Future[Option[Application]] = {
    applicationRepository.fetch(id)
  }

  def createOrUpdate(application: Application): Future[Application] = applicationRepository.save(application)

}
