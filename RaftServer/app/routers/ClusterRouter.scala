package routers

import controllers.ClusterController
import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

class ClusterRouter @Inject()(controller: ClusterController) extends SimpleRouter {

  override def routes: Routes = {
    case GET(p"/") => controller.getCluster
    case POST(p"/$id") => controller.postCluster(id.toInt)
  }
}
