package pea.app.model.multiscene

import pea.app.model.params.LoopParam
import pea.app.model.Injection
import pea.common.util.StringUtils

// https://gatling.io/docs/current/general/simulation_setup
case class MultisScenarios(
                          var id: Long,
                          var requests: Seq[MultisScenarioSingScenario],
                          var `name`: String,
                          var `type`: String,
                          var injections: Seq[Injection],
                          var loop: LoopParam = null
                    ){
  def getName(): String = {
    StringUtils.notEmptyElse(name, StringUtils.EMPTY)
  }
  def isValid(): Exception = {
    if (null == injections || injections.isEmpty) {
      new RuntimeException("Empty injections")
    } else {
      null
    }
  }

}

object MultisScenarios {
  val TYPE_CONTEXT = "context"
  val TYPE_SINGLE_REQUEST = "single"
}


