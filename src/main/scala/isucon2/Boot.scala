package isucon2

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

import akka.actor.Actor

import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http.HttpResponseStatus

import xitrum.{ActionActor, SkipCsrfCheck, Server}
import xitrum.annotation.{GET, POST}
import xitrum.util.Loader

object Boot {
  def main(args: Array[String]) {
    Server.start()
  }
}

//------------------------------------------------------------------------------

trait DefaultLayout extends ActionActor with SkipCsrfCheck{
  override def layout = renderViewNoLayout(classOf[DefaultLayout])

  protected var recentlyBought: Seq[ReqBuy] = _
  protected def onRecentlyBought()
  protected def receiveRecentlyBought: Actor.Receive = {
    case recentlyBought =>
      this.recentlyBought = recentlyBought.asInstanceOf[Seq[ReqBuy]]
      at("recentlyBought") = this.recentlyBought
      println("recentlyBought" + recentlyBought)
      onRecentlyBought()
  }

  DB.recentlyBoughtRef ! ReqSelect
}

//------------------------------------------------------------------------------

@GET("/admin")
class Admin extends DefaultLayout {
  protected def onRecentlyBought() {}

  def execute() {
    respondView()
  }
}

@POST("/admin")
class Init extends ActionActor with SkipCsrfCheck{
  def execute() {
//    DB.reset()
    redirectTo("/admin", HttpResponseStatus.FOUND)
  }
}

@GET("/admin/order")
class CSV extends ActionActor {
  def execute() {
//    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/csv")
//    response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, body.getBytes(CharsetUtil.UTF_8).length)
//    respondText(body)
  }
}

//------------------------------------------------------------------------------

@GET("")
class SiteIndex extends DefaultLayout {
  private var artists: List[Map[String,String]] = _

  protected def onRecentlyBought() {
    if (artists != null) respondView()
  }
  def execute() {
//    artists = DB.getMaster("artists")
//    at("artists") = artists
    context.become(receiveRecentlyBought)
    if (recentlyBought != null)  respondView()
  }
}

@GET("artist/:artistId")
class ArtistShow extends DefaultLayout {
  private var artist: Map[String,String] = _

  protected def onRecentlyBought() {}

  def execute() {
//    val artistId = param[Int]("artistId")
//    val artist   = Master.artists(artistId)
//    val tickets  = artist.tickets
  }
}

@GET("ticket/:ticketid")
class TicketShow extends DefaultLayout {
  protected def onRecentlyBought() {}

  def execute() {
    val ticketid = param("ticketid")

    respondView()
  }
}

@POST("buy")
class TicketBuy extends DefaultLayout {
  private var memberId:    String      = _
  private var variationId: Int         = _

  private var uncheckedSeatNos: Seq[String] = _
  private var boughtSeatNo:   String      = _

  protected def onRecentlyBought() {
    if (boughtSeatNo != null) respondText("Bought: " + boughtSeatNo + " , recentlyBought: " + recentlyBought)
  }

  def execute() {
    memberId    = param("member_id")
    variationId = param[Int]("variation_id")

    uncheckedSeatNos = util.Random.shuffle(DB.seatNos)

    val buyResult: Actor.Receive = {
      case ResBuy(_, false) =>
        check()

      case ResBuy(seatNo, true) =>
        println(true)
        boughtSeatNo = seatNo
        if (recentlyBought != null) {
          val buy = ReqBuy(variationId, seatNo, memberId)
          if (!recentlyBought.contains(buy)) recentlyBought = buy +: recentlyBought

          respondText("Bought: " + seatNo + " , recentlyBought: " + recentlyBought)
        }
    }

    context.become(buyResult orElse receiveRecentlyBought)
    check()
  }

  private def check() {
    if (uncheckedSeatNos.isEmpty) {
      if (recentlyBought != null) respondText("Soldout, recentlyBought: " + recentlyBought)
      return
    }

    val seatNo = uncheckedSeatNos.head
    uncheckedSeatNos = uncheckedSeatNos.tail

    val seatRef = DB.getSeat(variationId, seatNo).get
    seatRef ! ReqBuy(variationId, seatNo, memberId)
  }
}
