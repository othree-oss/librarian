package io.othree.librarian.slick.models

import java.util.UUID

import io.othree.librarian.slick.ModelWithId

case class MockModel(id: UUID, name: String) extends ModelWithId
