package io.othree.librarian.slick

import java.util.UUID

import slick.ast.ColumnOption

trait BaseTableDefinition { this : DatabaseComponent =>
  import profile.api._

  abstract class TableDef[A](tag: Tag, schema: String, tableName: String) extends Table[A](tag, Some(schema), tableName)

  abstract class TableDefWithId[A](tag: Tag, schema: String, tableName: String) extends TableDef[A](tag, schema, tableName) {
    def id: Rep[UUID] = column[UUID]("id", getIdColumnOptions:_*)

    private def getIdColumnOptions = {
        Array[ColumnOption[UUID]](O.PrimaryKey)
    }
  }
}
