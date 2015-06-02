package io.strongtyped.active.slick.docexamples

import io.strongtyped.active.slick.dao.EntityDao
import io.strongtyped.active.slick._
import slick.driver.H2Driver

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


object MappingActiveSlickIdentifiable extends TableQueries with JdbcProfileProvider {

  val jdbcProfile = H2Driver
  import jdbcProfile.api._

  case class Foo(name: String, id: Option[Int] = None) extends Identifiable {
    override type Id = Int
  }

  class FooTable(tag: Tag) extends EntityTable[Foo](tag, "FOOS") {
    def name = column[String]("NAME")
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def * = (name, id.?) <>(Foo.tupled, Foo.unapply)
  }

  object FooDao extends EntityDao[Foo, FooTable](jdbcProfile) {

    val tableQuery = EntityTableQuery[Foo, FooTable](tag => new FooTable(tag))

    val idLens = SimpleLens[Foo, Option[Int]](_.id, (foo, id) => foo.copy(id = id))

    def createSchema = {
      tableQuery.schema.create

    }
  }

  def run[T](block: Database => T): T = {
    val db = Database.forURL("jdbc:h2:mem:active-slick", driver = "org.h2.Driver")
    try {
      Await.ready(db.run(FooDao.createSchema), 200 millis)
      block(db)
    } finally {
      db.close()
    }
  }

  def main(args: Array[String]): Unit = {
    run { db =>
      val foo = Foo("foo")
      val fooWithId = Await.result(db.run(FooDao.save(foo)), 200 millis)
      assert(fooWithId.id.isDefined, "Foo's ID should be defined")
    }
  }
}
