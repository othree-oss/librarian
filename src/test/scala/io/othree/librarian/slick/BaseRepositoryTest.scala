package io.othree.librarian.slick

import java.util.UUID

import io.othree.aok.BaseTest
import io.othree.librarian.slick.definitions.MockModelDefinition
import io.othree.librarian.slick.models.MockModel
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import slick.jdbc.{H2Profile, JdbcProfile}

@RunWith(classOf[JUnitRunner])
class BaseRepositoryTest extends BaseTest
  with MockModelDefinition
  with DatabaseComponent {

  override val profile: JdbcProfile = H2Profile

  import profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit var db : Database = _
  var repository : MockModelRepository = _

  before {
    db = Database.forConfig("mockRepo")

    sqlu""" CREATE SCHEMA "mockRepo" """ now

    val mockTable = TableQuery[MockModelTable]
    val schema = mockTable.schema
    schema.create now

    DBIO.seq(
      sqlu"""insert into "mockRepo"."mock"("id","name") values('00000001-0000-0000-0000-000000000000','test') """, // id = 1
      sqlu"""insert into "mockRepo"."mock"("id","name") values('00000002-0000-0000-0000-000000000000','other test') """, // id = 2
      sqlu"""insert into "mockRepo"."mock"("id","name") values('00000003-0000-0000-0000-000000000000','repeated') """, // id = 3
      sqlu"""insert into "mockRepo"."mock"("id","name") values('00000004-0000-0000-0000-000000000000','repeated') """ // id = 4
    ) now

    repository = new MockModelRepository()
  }

  after {
    val mockTable = TableQuery[MockModelTable]
    val schema = mockTable.schema
    schema.drop now

    sqlu""" DROP SCHEMA "mockRepo" """ now
  }

  "BaseRepository" when {

    "getting rows by a condition" must {
      "return the requested models" in {
        val models = repository.getAllBy(table=> table.name === "repeated") now

        models should have size 2
        models should contain (MockModel(UUID.fromString("00000003-0000-0000-0000-000000000000"), "repeated"))
        models should contain (MockModel(UUID.fromString("00000004-0000-0000-0000-000000000000"), "repeated"))
      }
    }

    "deleting rows by a condition" must {
      "delete the requested row" in {
        val rowCount = repository.deleteBy(table => table.name === "repeated") now

        rowCount shouldBe 2
      }
    }

    "counting all rows" must {
      "return the correct number of rows" in {
        val rowCount = repository.countAll() now

        rowCount shouldBe 4
      }
    }

    "getting all rows" must {
      "return all models deserialized" in {
        val models = repository.getAll now

        models should have size 4
        models should contain (MockModel(UUID.fromString("00000001-0000-0000-0000-000000000000"), "test"))
        models should contain (MockModel(UUID.fromString("00000002-0000-0000-0000-000000000000"), "other test"))
        models should contain (MockModel(UUID.fromString("00000003-0000-0000-0000-000000000000"), "repeated"))
        models should contain (MockModel(UUID.fromString("00000004-0000-0000-0000-000000000000"), "repeated"))

      }
    }

    "inserting a new row" must {
      "return the number of affected rows" in {
        val model = MockModel(UUID.fromString("00000005-0000-0000-0000-000000000000"), "NewEntity")

        val result = repository.insert(model) now

        result shouldBe 1
      }
    }

    "getting any row by a condition" must {
      "return any model deserialized with that condition" in {
        val result = repository.getAnyBy(table => table.name === "repeated") now

        result shouldBe defined
        result.get should have (
          'name ("repeated")
        )
      }
    }

    "getting the first row by a condition" must {
      "return the first model deserialized with that condition" in {
        val result = repository.getFirstBy(table => table.name === "repeated") now

        result shouldBe defined
        result.get should have (
          'id (UUID.fromString("00000004-0000-0000-0000-000000000000")),
          'name ("repeated")
        )
      }
    }

    "updating a model" must {
      "return the number of rows modified" in {
        val model = MockModel(UUID.fromString("00000004-0000-0000-0000-000000000000"), "repeated")

        val result = repository.update(model) now
      }
    }

    "getting a row by id" must {
      "return the deserialized model" in {
        val maybeModel = repository.getById(UUID.fromString("00000001-0000-0000-0000-000000000000")) now

        maybeModel shouldBe Some(MockModel(UUID.fromString("00000001-0000-0000-0000-000000000000"), "test"))
      }
    }

    "setting a byte array paramter" must {
      "correctly insert the value" in {
        val bytes = "someTest".getBytes
        val result : Int = sqlu""" insert into "mockRepo"."mock"("id","name") values('00000006-0000-0000-0000-000000000000', $bytes) """ now

        result shouldBe 1
      }
    }

    "validating that a row exists" must {
      "return true if it does" in {
        val exists = repository.exists(UUID.fromString("00000001-0000-0000-0000-000000000000")) now

        exists shouldBe true
      }

      "return false if it doesn't" in {
        val exists = repository.exists(UUID.fromString("00000500-0000-0000-0000-000000000000")) now

        exists shouldBe false
      }
    }


  }
}
