package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{AbstractController, ControllerComponents}
import services.ApplicationService

@Singleton
class ApplicationController  @Inject()(cc: ControllerComponents,
                                       applicationService: ApplicationService) extends AbstractController(cc) with CommonControllers {

}
