package pea.app.model.multiscene

case class MultisScenarioSingleRandom(
                                       var ifcontext:IfConditionalParams,
                                       var elsecontext:Seq[Any] =null
                                          ) {

}
