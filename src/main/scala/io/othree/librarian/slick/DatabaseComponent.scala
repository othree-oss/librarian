package io.othree.librarian.slick

import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.{JdbcProfile, PositionedParameters, SetParameter}

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, ExecutionContext, Future}

trait DatabaseComponent extends LazyLogging {
  val profile: JdbcProfile

  import profile.api._

  class DbResult[T](val action: DBIO[T])(implicit val db: Database, ec: ExecutionContext) {

    def future: Future[T] = db.run(action)

    def future(transactionally : Boolean) : Future[T] = {
      if (transactionally) {
        db.run(action.transactionally)
      } else {
        future
      }
    }

    def result(timeout: Duration): T = Await.result(future, timeout)

    def result: T = result(1 second)

  }

  case object Now
  case object Later
  case object Wait

  implicit class DBIOConverter[T](action: DBIO[T]) {

    def <>()(implicit db: Database, ec: ExecutionContext) : DbResult[T] = {
      new DbResult[T](action)(db, ec)
    }

    def async()(implicit db: Database, ec: ExecutionContext) : Future[T] = {
      val future = <>()(db, ec).future
      future.failed map {
        case ex: Exception =>
          logger.error("Database query failed to execute", ex)
      }
      future
    }

    def asyncTransaction()(implicit db: Database, ec: ExecutionContext) : Future[T] = {
      val future = <>()(db, ec).future(true)
      future.failed map {
        case ex: Exception =>
          logger.error("Database query failed to execute", ex)
      }
      future
    }

    def now(duration: scala.concurrent.duration.Duration)(implicit db: Database, ec: ExecutionContext) : T = {
      <>()(db, ec).result(duration)
    }

    def now()(implicit db: Database, ec: ExecutionContext) : T = {
      <>()(db, ec).result
    }
  }

  implicit object SetByteArrayParameter extends SetParameter[Array[Byte]] {
    override def apply(value: Array[Byte], parameters: PositionedParameters): Unit = {
      parameters.setBytes(value)
    }
  }

}