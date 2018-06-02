package io.othree.librarian.slick

import java.util.UUID

import scala.concurrent.ExecutionContext

trait BaseRepository extends BaseTableDefinition {
  this: DatabaseComponent =>

  import profile.api._

  abstract class BaseRepository[A <: TableDef[B], B]()
                                                    (implicit val db: Database, ec: ExecutionContext) {
    val table: TableQuery[A]

    def getAllBy(condition: A => Rep[Boolean]): DBIO[Seq[B]] = {
      val q = for {e <- table if condition(e)} yield e

      q.result
    }

    def deleteBy(condition: A => Rep[Boolean]): DBIO[Int] = {
      val toDelete = for {e <- table if condition(e)} yield e
      toDelete.delete 
    }

    def countAll(): DBIO[Int] = {
      table.length.result 
    }

    def getAll: DBIO[Seq[B]] = {
      table.result 
    }

    def insert(model: B): DBIO[Int] = {
      val q = table += model
      q 
    }

    def getAnyBy(condition: A => Rep[Boolean]): DBIO[Option[B]] = {
      val q = table.filter {
        condition(_)
      }

      q.result.headOption 
    }
  }

  abstract class ModelWithIdRepository[A <: TableDefWithId[B], B <: ModelWithId]
  ()
  (override implicit val db: Database, ec: ExecutionContext)
    extends BaseRepository[A, B]()(db, ec) {

    private val compiledGetById = Compiled(getByIdQuery _)

    private def getByIdQuery(id: Rep[UUID]) = {
      table.filter(row => row.id === id)
    }

    implicit private val executionContext = ec

    def getFirstBy(condition: A => Rep[Boolean]): DBIO[Option[B]] = {
      val q = table.filter {
        condition(_)
      }
      q.sortBy(r => r.id.desc).result.headOption 
    }

    def update(updatedModel: B): DBIO[Int] = {
      val q = compiledGetById(updatedModel.id)

      q.update(updatedModel)
    }

    def getById(id: UUID): DBIO[Option[B]] = {
      compiledGetById(id).result.headOption
    }

    def exists(id: UUID): DBIO[Boolean] = {
      table.filter(_.id === id).exists.result
    }
  }

}
