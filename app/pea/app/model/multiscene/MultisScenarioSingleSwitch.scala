package pea.app.model.multiscene

case class MultisScenarioSingleSwitch(
           var context:String,
           var ifcontext:Map[String,Seq[Any]] ,
           var elsecontext:Seq[Any] =null
        ) {

}
