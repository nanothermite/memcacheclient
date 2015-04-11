package controllers

import _root_.common.Shared
import play.api.mvc._
import play.api.libs.concurrent.Promise
import shade.memcached.Memcached

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  val myCache:Memcached = Shared.memd.get
  val minDuration:Duration = 1.milli
  val maxDuration:Duration = 2.milli

  /**
   * set cache key to value asynchronously
   * @param key string
   * @param value string
   * @return
   */
  def memset(key: String, value: String) = Action.async {
    val op: Future[Unit] = myCache.set(key, value, minDuration)
    val timeoutFuture = Promise.timeout("Oops", maxDuration)
    Future.firstCompletedOf(Seq(op, timeoutFuture)).map {
      case i: Unit => Ok("ok")
      case t : AnyRef => Ok("unset")
    }
  }

  /**
   * blocking version
   * @param key string
   * @return
   */
  def memgetBlock(key: String) = Action {
    val res = myCache.awaitGet[String](key) match {
      case Some(value) => value
      case None        => s"key $key not found"
    }
    Ok(res)
  }

  /**
   * asynch get key for minDuration
   * @param key string
   * @return
   */
  def memget(key: String) = Action.async {
    val futureInt = myCache.get[String](key)
    val timeoutFuture = Promise.timeout("Oops", minDuration)
    Future.firstCompletedOf(Seq(futureInt, timeoutFuture)).map {
      case i: Option[String] => Ok(if (i != None) i.get else "nf")
      case t: AnyRef => Ok("broke")
    }
  }

  /**
   * async drop key for minDuration
   * @param key string
   * @return
   */
  def memdrop(key: String) = Action.async {
    val futureBool = myCache.delete(key)
    val timeoutFuture = Promise.timeout("Oops", maxDuration)
    Future.firstCompletedOf(Seq(futureBool, timeoutFuture)).map {
      case i: Boolean => Ok(if (i) "hit" else "miss")
      case t: AnyRef => Ok("broke")
    }
  }
}
