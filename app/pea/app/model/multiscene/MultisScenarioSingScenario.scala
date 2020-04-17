package pea.app.model.multiscene

import pea.app.model.params.HttpAssertionParam

case class MultisScenarioSingScenario(
                          var request: MultisScenarioSingleRequest= null,
                          var conditional: MultisScenarioSingleConditional= null,
                          var switch: MultisScenarioSingleSwitch= null,
                          var randome: MultisScenarioSingleRandom= null,
                          var pause: MultisScenarioPauseParams= null,
                          var `type`:String,
                        ) {


}

object MultisScenarioSingScenario {

  // Conditional statements
  val TYPE_EXEC =  "exec"
  val TYPE_PAUSE = "pause"
  val TYPE_DO_IF = "doIf"
  val TYPE_DO_IF_Equals = "doIfEquals"
  val TYPE_DO_IF_OR_ELSE = "doIfOrElse"
  val TYPE_DO_IF_EQUALS_ORELSE = "doIfEqualsOrElse"
  val TYPE_DO_SWITCH = "doSwitch"
  val TYPE_DO_SWITCH_OR_ELSE = "doSwitchOrElse"


  // Randome statements

  val TYPE_RANDOM_SWITCH = "randomSwitch"
  val TYPE_RANDOM_SWITCH_OR_ELSE = "randomSwitchOrElse"
  val TYPE_UNIFORM_RANDOM_SWITCH = "uniformRandomSwitch"
  val TYPE_ROUND_ROBIN_SWITCH = "roundRobinSwitch"

  // Errors handling
  val TYPE_TRY_MAX = "tryMax"
  val TYPE_EXIT_BLOCK_ON_FAIL = "exitBlockOnFail"
  val EXIT_HERE_IF_FAILED = "exitHereIfFailed"

}

/**
  * val TYPE_EXEC =  "exec"
  * val TYPE_EXEC_WITHIN = "execWithin"
  * val TYPE_PAUSE = "pause"
  * val TYPE_PAUSE_WITHIN = "pauseWithin"
  * val TYPE_DO_IF = "doIf"
  * val TYPE_DO_IF_STOP = "doIfStop"
  * val TYPE_DO_IF_Equals = "doIfEquals"
  * val TYPE_DO_IF_Equals_STOP = "doIfEqualsStop"
  * val TYPE_DO_IF_OR_ELSE_IF_START = "doIfOrElseIfStart"
  * val TYPE_DO_IF_OR_ELSE_IF_STOP = "doIfOrElseIfStop"
  * val TYPE_DO_IF_OR_ELSE_ELSE_START = "doIfOrElseElseStart"
  * val TYPE_DO_IF_OR_ELSE_ELSE_STOP = "doIfOrElseElseStop"
  * val TYPE_DO_IF_EQUALS_ORELSE_IF_START = "doIfEqualsOrElseIfStart"
  * val TYPE_DO_IF_EQUALS_ORELSE_IF_STOP = "doIfEqualsOrElseIfStop"
  * val TYPE_DO_IF_EQUALS_ORELSE_ELSE_START = "doIfEqualsOrElseElseStart"
  * val TYPE_DO_IF_EQUALS_ORELSE_ELSE_STOP = "doIfEqualsOrElseElseStop"
  *
  * */
