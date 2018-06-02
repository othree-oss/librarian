package io.othree.librarian.slick.definitions

import java.util.UUID

import io.othree.librarian.slick.{BaseRepository, BaseTableDefinition, DatabaseComponent}
import io.othree.librarian.slick.models.MockModel

import scala.concurrent.ExecutionContext

trait MockModelDefinition extends BaseRepository
  with BaseTableDefinition { this : DatabaseComponent =>

  import profile.api._

  class MockModelTable(tag: Tag) extends TableDefWithId[MockModel](tag, "mockRepo", "mock") {
    def name: Rep[String] = column[String]("name")

    def * = (id, name) <> (
      (columns : (UUID, String)) => MockModel.tupled(columns),
      (model : MockModel) => MockModel.unapply(model)
    )
  }

  class MockModelRepository()
                           (override implicit val db: Database, ec: ExecutionContext)
  extends ModelWithIdRepository[MockModelTable, MockModel]()(db, ec) {

    val table = TableQuery[MockModelTable]
  }
}
