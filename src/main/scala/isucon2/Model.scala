package isucon2

import akka.actor.{Actor, ActorRef, Props}

object ReqReset
object ReqSelect

case class ReqBuy(variationId: Int, seatNo: String, memberId: String)
case class ResBuy(seatNo: String, bought: Boolean)

class Seat extends Actor with akka.actor.ActorLogging {
  private var bought = false

  def receive = {
    case ReqReset =>
      bought = false

    case buy @ ReqBuy(_, seatNo, _) =>
      log.info("bbbb: " + bought)

      if (!bought) {
        bought = true
        sender ! ResBuy(seatNo, true)
        DB.recentlyBoughtRef ! buy
      } else {
        sender ! ResBuy(seatNo, false)
      }
  }
}

class RecentlyBought extends Actor {
  private var recentlyBought = Seq[ReqBuy]()

  def receive = {
    case buy: ReqBuy =>
      recentlyBought = buy +: recentlyBought
      if (recentlyBought.length > 10) recentlyBought = recentlyBought.take(10)

    case ReqReset =>
      recentlyBought = Seq[ReqBuy]()

    case ReqSelect =>
      sender ! recentlyBought
  }
}

//------------------------------------------------------------------------------

case class SeatKey(variationId: Int, seatNo: String)

object DB {
  val STADIUM_WIDTH  = 2
  val STADIUM_HEIGHT = 2

  val seatNos: Seq[String] =
    for (i <- 0 until STADIUM_WIDTH; j <- 0 until STADIUM_HEIGHT) yield "%02d-%02d".format(i, j)

  val recentlyBoughtRef = xitrum.Config.actorSystem.actorOf(Props(classOf[RecentlyBought]))

  private var lookup = Map[SeatKey, ActorRef]()
  addStadiumVariation(1)
  addStadiumVariation(2)

  def reset() {
    lookup.values.foreach(_ ! ReqReset)
    recentlyBoughtRef ! ReqReset
  }

//  def getMaster(master:String):List[Map[String,String]] =
//    master match {
//      case "artists" => Master.getArtists
//      case "variations" => Master.getValiations
//      case "tickets"    => Master.getTickets
//  }
//
//  def getByIdMaster(id:String, master:String):Map[String,String] =
//    master match {
//      case "artists" => Master.getArtists(id.toInt)
//      case "variations" => Master.getValiations(id.toInt)
//      case "tickets"    => Master.getTickets(id.toInt)
//  }
//  def filtering(target:Map[String,String],query:Map[String,String]):Boolean =
//    target.
//
//  def filterMaster(filter:Map[String,String], master:List[Map[String,String]]):List[Map[String,String]] =
//    master.filter(artist => filtering(artist,filter))

  def getSeat(variationId: Int, seatNo: String) =
    lookup.get(SeatKey(variationId, seatNo))

  private def addStadiumVariation(variationId: Int) {
    for (seatNo <- seatNos) {
      val ref  = xitrum.Config.actorSystem.actorOf(Props(classOf[Seat]), variationId + "-" + seatNo)
      lookup += SeatKey(variationId, seatNo) -> ref
    }
  }
}
