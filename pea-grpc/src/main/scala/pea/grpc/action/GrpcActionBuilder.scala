package pea.grpc.action

import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.check.{MultipleFindCheckBuilder, ValidatorCheckBuilder}
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.grpc.{Channel, Metadata}
import pea.grpc.check.{GrpcCheck, ResponseExtract}
import pea.grpc.request.HeaderPair

import scala.collection.breakOut
import scala.concurrent.Future

case class GrpcActionBuilder[Req, Res](
                                        requestName: Expression[String],
                                        method: Channel => Req => Future[Res],
                                        payload: Expression[Req],
                                        headers: List[HeaderPair[_]] = Nil,
                                        checks: List[GrpcCheck[Res]] = Nil,
                                      ) extends ActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action = GrpcAction(this, ctx, next)

  def header[T](key: Metadata.Key[T])(value: Expression[T]) =
    copy(headers = HeaderPair(key, value) :: headers)

  def check(checks: GrpcCheck[Res]*) =
    copy(checks = this.checks ::: checks.toList)

  private def mapToList[T, U](s: Seq[T])(f: T => U) = s.map[U, List[U]](f)(breakOut)

  // In fact they can be added to checks using .check, but the type Res cannot be inferred there
  def extract[X](f: Res => Option[X])(ts: (ValidatorCheckBuilder[ResponseExtract, Res, X] => GrpcCheck[Res])*) = {
    val e = ResponseExtract.extract(f)
    copy(checks = checks ::: mapToList(ts)(_.apply(e)))
  }

  def exists[X](f: Res => Option[X]) = extract(f)(_.exists.build(ResponseExtract.materializer))

  def extractMultiple[X](f: Res => Option[Seq[X]])(ts: (MultipleFindCheckBuilder[ResponseExtract, Res, X] => GrpcCheck[Res])*) = {
    val e = ResponseExtract.extractMultiple[Res, X](f)
    copy(checks = checks ::: mapToList(ts)(_.apply(e)))
  }
}
