package services

import javax.inject.{Inject, Singleton}

import repository.ApplicationRepository

@Singleton
class ApplicationService @Inject()(applicationRepository: ApplicationRepository) {

}
