package controllers

import components.ClusterControllerComponents
import javax.inject.{Inject, Singleton}
import models.{ClusterItem, Person}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, Request, Result}

@Singleton
class ClusterController @Inject()(cc: ClusterControllerComponents) extends AbstractController(cc) {

  def getCluster: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok("hello world")
  }

  def postCluster(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.body.asJson match {
      case Some(json) =>
        Json.fromJson[Person](json) match {
          case JsSuccess(value, path) =>
            cc.service.showPerson(value)
            Accepted("hello world")
          case JsError(errors) =>
            BadRequest(s"Invalid person")
        }
      case None =>
        BadRequest(s"Request body is not a json")
    }
  }

  def postItemToCluster(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.body.asJson match {
      case Some(json) =>
        Json.fromJson[ClusterItem](json) match {
          case JsSuccess(value, _) =>
            val clusterState = cc.service.addItemToCluster(value)
            Accepted(Json.toJson(clusterState))
          case JsError(_) =>
            BadRequest(s"Invalid json")
        }
      case None =>
        BadRequest(s"Request body is not a json")
    }
  }
}
