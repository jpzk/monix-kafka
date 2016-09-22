package monix.kafka.config

import com.typesafe.config.ConfigException.BadValue
import monix.kafka.KafkaProducerConfig

/** Enumeration for specifying the `acks` setting in [[KafkaProducerConfig]].
  *
  * Represents the number of acknowledgments the producer requires
  * the leader to have received before considering a request complete.
  * This controls the durability of records that are sent.
  *
  * For the available options see:
  *
  *  - [[Acks.Zero]]
  *  - [[Acks.NonZero]]
  *  - [[Acks.All]]
  */
sealed trait Acks extends Product with Serializable {
  def id: String
}

object Acks {
  @throws(classOf[BadValue])
  def apply(id: String): Acks =
    id match {
      case All.id => All
      case Number(nrStr) =>
        val nr = nrStr.toInt
        if (nr == 0) Zero else NonZero(nr)
      case _ =>
        throw new BadValue("kafka.acks", s"Invalid value: $id")
    }

  /** If set to zero then the producer will not wait
    * for any acknowledgment from the server at all.
    *
    * The record will be immediately added to the socket buffer and
    * considered sent. No guarantee can be made that the server has received
    * the record in this case, and the retries configuration will not
    * take effect (as the client won't generally know of any failures).
    * The offset given back for each record will always be set to -1.
    */
  case object Zero extends Acks {
    val id = "0"
  }

  /** This will mean the leader will write the record to its local
    * log but will respond without awaiting full acknowledgement
    * from all followers.
    *
    * In this case should the leader fail immediately after acknowledging
    * the record but before the followers have replicated it then the
    * record will be lost.
    */
  final case class NonZero(nr: Int) extends Acks {
    require(nr > 0, "nr > 0")
    val id = nr.toString
  }

  /** This means the leader will wait for the
    * full set of in-sync replicas to acknowledge the record.
    *
    * This guarantees that the record will not be lost as long as
    * at least one in-sync replica remains alive. This is the strongest
    * available guarantee.
    */
  case object All extends Acks {
    val id = "all"
  }

  // Regular expression for parsing IDs
  private val Number = """^(\d+)$""".r
}