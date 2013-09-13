package isucon2

import com.hazelcast.query.Predicates
/*
case class Variation(id: Int, name: String)
case class Ticket(id: Int, name: String, variations: Seq[Variation])
case class Artist(id: Int, name: String, tickets: Seq[Ticket])

object Master {
  val artists = Map[Int, Artist](
    1 -> Artist(1, "NHN48", Seq(
      Ticket(1, "西武ドームライブ",            Seq(Variation(1, "アリーナ席"),Variation(2, "スタンド席"))),
      Ticket(2, "東京ドームライブ",            Seq(Variation(3, "アリーナ席"),Variation(4, "スタンド席")))
    )),

    2 -> Artist(2, "はだいろクローバーZ", Seq(
      Ticket(3, "さいたまスーパーアリーナライブ",  Seq(Variation(5, "アリーナ席"), Variation(6, "スタンド席"))),
      Ticket(4, "横浜アリーナライブ",            Seq(Variation(7, "アリーナ席"), Variation(8, "スタンド席"))),
      Ticket(5, "西武ドームライブ",              Seq(Variation(9, "アリーナ席"), Variation(10, "スタンド席")))
    ))
  )

  // Index
  val ticketId2ArtistId = Map(
      1 -> 1,
      2 -> 1,
      3 -> 2,
      4 -> 2,
      5 -> 2
  )
}
*/

case class Artist(id: Int, name: String)
case class Ticket(id: Int, name: String, artistId: Int)
case class Variation(id: Int, name: String, ticketId: Int)

object Master {
  val artists = Seq[Artist](
    Artist(1, "NHN48"),
    Artist(2, "はだいろクローバーZ")
  )

  val tickets = Seq[Ticket](
    Ticket(1, "西武ドームライブ",            1),
    Ticket(2, "東京ドームライブ",            1),
    Ticket(3, "さいたまスーパーアリーナライブ", 2),
    Ticket(4, "横浜アリーナライブ",           2),
    Ticket(5, "西武ドームライブ",            2)
  )

  val variations = Seq[Variation](
    Variation(1, "アリーナ席", 1),
    Variation(2, "スタンド席", 1),
    Variation(3, "アリーナ席", 2),
    Variation(4, "スタンド席", 2),
    Variation(5, "アリーナ席", 3),
    Variation(6, "スタンド席", 3),
    Variation(7, "アリーナ席", 4),
    Variation(8, "スタンド席", 4),
    Variation(9, "アリーナ席", 5),
    Variation(10, "スタンド席", 6)
  )

  // Initialize Insert
  val artistsDb = xitrum.Config.hazelcastInstance.getMap[Int, Artist]("artists")
  artistsDb.addIndex("name", true)
  for (artist <- artists) artistsDb.put(artist.id, artist)

  // select query
//  val a = artistsDb.get(Predicates.equal("name", "NHN48"))
//  val resultSet = artistsDb.entrySet(
//      Predicates.or(
//          Predicates.equal("name", "NHN48"),
//          Predicates.equal("name", "はだいろクローバーZ")
//      )
//  )

  val ticketDb = xitrum.Config.hazelcastInstance.getMap[Int, Ticket]("tickets")
  ticketDb.addIndex("name", true)
  ticketDb.addIndex("artistId", true)
  for (ticket <- tickets) ticketDb.put(ticket.id, ticket)

  val variationDb = xitrum.Config.hazelcastInstance.getMap[Int, Variation]("variations")
  variationDb.addIndex("name", true)
  variationDb.addIndex("ticketId", true)

  for (variation <- variations) variationDb.put(variation.id, variation)

  /**
   * conds:
   * - name
   * - artistId
   * - name, artistId
   */
  def getTickets(name: Option[String], artistId: Option[Int]): Seq[Ticket] = {
    if (name.isDefined && artistId.isDefined) {
      tickets.filter { t => t.name == name.get && t.artistId == artistId.get }
    } else if (name.isDefined) {
      tickets.filter(_.name == name.get)
    } else if (artistId.isDefined) {
      tickets.filter(_.artistId == artistId.get)
    } else {
      tickets
    }
  }
}
