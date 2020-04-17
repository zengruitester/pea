package pea.app.model.multiscene

case class ConditionalParams(
      var `type`:String,
      var connect:String,
      var key:Any,
      var value:Any
      ) {
}
object ConditionalParams{
  val TYPE_SESSION =  "session"
  val CONNECT_TYPE_EQUALTO = "equalto"
  val CONNECT_TYPE_NOT_EQUALTO = "notequalto"
  val CONNECT_TYPE_CONTAIN = "contain"
  val CONNECT_TYPE_NOT_CONTAIN = "notcontain"
  val CONNECT_TYPE_AND = "and"
  val CONNECT_TYPE_OR = "or"
  val TYPE_ORDINARY = "ordinary"
}
