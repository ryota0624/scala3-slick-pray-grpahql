package router

import caliban.*
import caliban.CalibanError.ExecutionError
import caliban.interop.tapir.HttpInterpreter
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.Schema.auto.*
import caliban.schema.{ArgBuilder, Schema}
import graphql.tweet.*
import graphql.tweet.Types.{Tweet, User}
import org.apache.pekko.actor.ActorSystem
import play.api.*
import play.api.routing.*
import play.api.routing.sird.*
import zio.{Runtime, Unsafe}

import java.time.LocalDate
import javax.inject.*
import scala.util.Try

class GraphqlRouter @Inject() (val system: ActorSystem) extends SimpleRouter {
  given ActorSystem = system

  given runtime: Runtime[Any] = Runtime.default

  given Schema[Any, Types.Stat] = Schema.gen
  given Schema[Any, Tweet] = Schema.gen
  given Schema[Any, User] = Schema.gen
  given Schema[Any, Operations.Mutation] = Schema.gen
  given Schema[Any, Operations.Query] = Schema.gen
  given Schema[Any, Types.MutationCreateTweetArgs] = Schema.gen
  given Schema[Any, Types.MutationDeleteTweetArgs] = Schema.gen
  given Schema[Any, Types.MutationMarkTweetReadArgs] = Schema.gen

  val query = Operations.Query(
    Tweet = { args => ??? },
    Tweets = { args => ??? },
    Notifications = { args => ??? },
    NotificationsMeta = zio.ZIO.succeed(Some(Types.Meta(Some(1)))),
    TweetsMeta = zio.ZIO.succeed(Some(Types.Meta(Some(1)))),
    User = { args => ??? }
  )
  val mutation = Operations.Mutation(
    createTweet = { args => ??? },
    deleteTweet = { args => ??? },
    markTweetRead = { args => ??? }
  )

  val api = graphQL(
    RootResolver(query, mutation)
  )
  implicit val localDateArgBuilder: ArgBuilder[LocalDate] = {
    case Value.StringValue(value) =>
      Try(LocalDate.parse(value))
        .fold(
          ex =>
            Left(
              ExecutionError(
                s"Can't parse $value into a LocalDate",
                innerThrowable = Some(ex)
              )
            ),
          Right(_)
        )
    case other =>
      Left(ExecutionError(s"Can't build a LocalDate from input $other"))
  }

  override def routes: Router.Routes = {

    val api = graphQL(
      RootResolver[Operations.Query](query)
    )
    val interpreter = Unsafe.unsafe(implicit u =>
      runtime.unsafe.run(api.interpreter).getOrThrow()
    )
    Router.from {
      case req @ POST(p"/graphql") =>
        PlayAdapter.makeHttpService(HttpInterpreter(interpreter)).apply(req)
      case req @ GET(p"/graphiql") =>
        PlayAdapter.makeGraphiqlService("/graphql").apply(req)
    }.routes
  }
}
