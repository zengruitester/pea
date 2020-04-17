package pea.app.model.multiscene

case class IfConditionalParams(
      var condition:ConditionalParams,
      var value:Seq[MultisScenarioSingScenario]
      ) {
}
