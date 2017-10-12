package services

import javax.inject.{Inject, Singleton}

import models.{APIIdentifier, Application, HasSucceeded}
import repository.{ApplicationRepository, SubscriptionRepository}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SubscriptionService @Inject()(applicationRepository: ApplicationRepository, subscriptionRepository: SubscriptionRepository) {

  def subscribe(applicationId: String, apiIdentifier: APIIdentifier): Future[HasSucceeded] = {
    for {
      _ <- applicationRepository.fetch(applicationId)
      result <- subscriptionRepository.subscribe(applicationId, apiIdentifier)
    } yield result
  }

  def unsubscribe(applicationId: String, apiIdentifier: APIIdentifier): Future[HasSucceeded] = {
    subscriptionRepository.unsubscribe(applicationId, apiIdentifier)
  }

  def getSubscriptions(applicationId: String): Future[Seq[APIIdentifier]] = {
    for {
      _ <- applicationRepository.fetch(applicationId)
      apis <- subscriptionRepository.getSubscriptions(applicationId)
    } yield apis
  }

  def isSubscribed(applicationId: String, context: String, version: String): Future[Boolean] = {
    subscriptionRepository.isSubscribed(applicationId, APIIdentifier(context, version))
  }
}
