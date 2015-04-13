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
   * actual work horse setting
   * @param key string
   * @param value string
   * @return
   */
  def setSeq(key:String, value: String) : Future[Any] = {
    Future.firstCompletedOf(Seq(myCache.set(key, value, minDuration), Promise.timeout("Oops", maxDuration)))
  }

  /**
   * actual work horse getting
   * @param key string
   * @return
   */
  def getSeq(key:String) : Future[Any] = {
    Future.firstCompletedOf(Seq(myCache.get[String](key), Promise.timeout("Oops", minDuration)))
  }

  /**
   * actual work horse deleting
   * @param key string
   * @return
   */
  def delSeq(key:String) : Future[Any] = {
    Future.firstCompletedOf(Seq(myCache.delete(key), Promise.timeout("Oops", maxDuration)))
  }


  /**
   * set cache key to value asynchronously
   * @param key string
   * @param value string
   * @return
   */
  def memset(key: String, value: String) = Action.async {
    setSeq(key, value).map {
      case i : Unit   => Ok("ok")
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
    getSeq(key).map {
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
    delSeq(key).map {
      case i: Boolean => Ok(if (i) "hit" else "miss")
      case t: AnyRef => Ok("broke")
    }
  }
}
