package controllers

import components.ClusterControllerComponents
import javax.inject.{Inject, Singleton}
import models.ClusterItem
import models.ClusterState._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, Request}

@Singleton
class ClusterController @Inject()(cc: ClusterControllerComponents) extends AbstractController(cc) {

  def getClusterState(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val state = cc.service.getClusterState()
    Ok(Json.toJson(state))
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
