package redis

import akka.util.ByteString
import redis.protocol._
import scala.util.Try
import scala.annotation.tailrec
import scala.collection.mutable

trait RedisValueConverter[A] {
  def from(a: A): ByteString
}

object RedisValueConverter {

  implicit object StringConverter extends RedisValueConverter[String] {
    def from(s: String): ByteString = ByteString(s)
  }

  implicit object ShortConverter extends RedisValueConverter[Short] {
    def from(i: Short): ByteString = ByteString(i.toString)
  }

  implicit object IntConverter extends RedisValueConverter[Int] {
    def from(i: Int): ByteString = ByteString(i.toString)
  }

  implicit object LongConverter extends RedisValueConverter[Long] {
    def from(i: Long): ByteString = ByteString(i.toString)
  }

  implicit object FloatConverter extends RedisValueConverter[Float] {
    def from(f: Float): ByteString = ByteString(f.toString)
  }

  implicit object DoubleConverter extends RedisValueConverter[Double] {
    def from(d: Double): ByteString = ByteString(d.toString)
  }

  implicit object CharConverter extends RedisValueConverter[Char] {
    def from(c: Char): ByteString = ByteString(c)
  }

  implicit object ByteConverter extends RedisValueConverter[Byte] {
    def from(b: Byte): ByteString = ByteString(b)
  }

  implicit object ArrayByteConverter extends RedisValueConverter[Array[Byte]] {
    def from(b: Array[Byte]): ByteString = ByteString(b)
  }

  implicit object ByteStringConverter extends RedisValueConverter[ByteString] {
    def from(bs: ByteString): ByteString = bs
  }

}

trait MultiBulkConverter[A] {
  def to(redisReply: MultiBulk): Try[A]
}

object MultiBulkConverter {

  implicit object SeqStringMultiBulkConverter extends MultiBulkConverter[Seq[String]] {
    def to(reply: MultiBulk): Try[Seq[String]] = Try(reply.responses.map(r => {
      r.map(_.toString)
    }).get)
  }

  implicit object SeqByteStringMultiBulkConverter extends MultiBulkConverter[Seq[ByteString]] {
    def to(reply: MultiBulk): Try[Seq[ByteString]] = Try(reply.responses.map(r => {
      r.map(_.toByteString)
    }).get)
  }

  implicit object SeqOptionByteStringMultiBulkConverter extends MultiBulkConverter[Seq[Option[ByteString]]] {
    def to(reply: MultiBulk): Try[Seq[Option[ByteString]]] = Try(reply.responses.map(r => {
      r.map(_.asOptByteString)
    }).get)
  }

  implicit object MapStringByteStringMultiBulkConverter extends MultiBulkConverter[Map[String, ByteString]] {
    def to(reply: MultiBulk): Try[Map[String, ByteString]] = Try(reply.responses.map(r => {
      val seq = r.map(_.toByteString)
      seqToMap(seq)
    }).get)

    def seqToMap(s: Seq[ByteString]): Map[String, ByteString] = {
      require(s.length % 2 == 0, "odd number of elements")
      @tailrec
      def recur(s: Seq[ByteString], acc: Map[String, ByteString]): Map[String, ByteString] = {
        if (s.isEmpty) {
          acc
        } else {
          recur(s.tail.tail, acc + (s.head.utf8String -> s.tail.head))
        }
      }
      recur(s, Map.empty[String, ByteString])
    }
  }

  implicit object SeqByteStringDoubleMultiBulkConverter extends MultiBulkConverter[Seq[(ByteString, Double)]] {
    def to(reply: MultiBulk): Try[Seq[(ByteString, Double)]] = Try(reply.responses.map(r => {
      val seq = r.map(_.toByteString)
      seqToSeqTuple2(seq)
    }).get)

    def seqToSeqTuple2(s: Seq[ByteString]): Seq[(ByteString, Double)] = {
      require(s.length % 2 == 0, "odd number of elements")
      val queue = mutable.Queue[(ByteString, Double)]()
      @tailrec
      def recur(s: Seq[ByteString]) {
        if (s.nonEmpty) {
          val score = java.lang.Double.valueOf(s.tail.head.utf8String)
          queue.enqueue((s.head, score))
          recur(s.tail.tail)
        }
      }
      recur(s)
      queue.toSeq
    }
  }

  implicit object OptionTupleStringByteStringMultiBulkConverter extends MultiBulkConverter[Option[(String, ByteString)]] {
    def to(reply: MultiBulk): Try[Option[(String, ByteString)]] = Try(reply.responses.map(r => {
      Some(r.head.toString, r.tail.head.toByteString)
    }).getOrElse(None))
  }

  implicit object SeqBooleanMultiBulkConverter extends MultiBulkConverter[Seq[Boolean]] {
    def to(reply: MultiBulk): Try[Seq[Boolean]] = Try(reply.responses.map(r => {
      r.map(_.toString == "1")
    }).get)
  }

}
