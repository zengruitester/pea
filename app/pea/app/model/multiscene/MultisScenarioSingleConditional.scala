package pea.app.model.multiscene

case class MultisScenarioSingleConditional(
      var ifcontext:IfConditionalParams=null,
      var elsecontext:Seq[MultisScenarioSingScenario] =null
    ) {

}
