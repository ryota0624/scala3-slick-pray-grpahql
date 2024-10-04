package controllers

import generated.database.Tables.{
  StudentCourses,
  StudentCoursesRow,
  Students,
  StudentsRow
}
import io.circe.generic.auto.*
import io.circe.syntax.*
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.circe.Circe
import play.api.mvc.*
import play.db.NamedDatabase
import slick.jdbc.SQLiteProfile.api.*

import javax.inject.*
import scala.util.Random

/** This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class UserController @Inject() (
    @NamedDatabase(
      "students"
    ) protected val dbConfigProvider: DatabaseConfigProvider
)(val controllerComponents: ControllerComponents)
    extends BaseController
    with Circe
    with HasDatabaseConfigProvider[slick.jdbc.SQLiteProfile] {

  private case class User(
      id: Int,
      name: String,
      age: Int
  )

  case class CreateUser(
      name: String,
      age: Int
  )

  /** Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method will be
    * called when the application receives a `GET` request with a path of `/`.
    */
  def index() = Action.async { implicit request: Request[AnyContent] =>
    import scala.concurrent.ExecutionContext.Implicits.global

    val db = dbConfigProvider.get.db

    val results = db.run(
      Students.joinLeft(StudentCourses).on(_.studentId === _.studentId).result
    )
    results.map({ result =>
      Ok(views.html.user.index(result.map({ case (student, studentCourse) =>
        student.studentName.toString() + " " + studentCourse.fold("None")(sc =>
          sc.courseId
        )
      })))
    })

  }

  def show(id: Int) = Action { implicit request: Request[AnyContent] =>
    Ok(User(id, "John", 30).asJson.spaces4)
  }

  def createUser = Action.async(circe.tolerantJson[CreateUser]) {

    implicit request: Request[CreateUser] =>
      import scala.concurrent.ExecutionContext.Implicits.global

      val createUser = request.body
      val user = User(1, createUser.name, createUser.age)
      val student: StudentsRow =
        StudentsRow(Option(Random.nextInt()), Option(createUser.name))
      val course = StudentCoursesRow(Option("1"), student.studentId)
      val insert = Students += student

      dbConfig.db
        .run(
          DBIO.seq(
            Students += student,
            StudentCourses += course
          ).transactionally
        )
        .map(_ => Ok(student.asJson))
  }
}
