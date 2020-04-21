package pea.app.model.multiscene

import pea.app.model.params.{DurationParam, FeederParam, ThrottleParam}
import pea.app.model.{LoadMessage, LoadTypes}
import pea.common.util.StringUtils

case class MultisScenariosMessage(
                                   var name: String,
                                   var multisScenarios: List[MultisScenarios],
                                   var report: Boolean = true,
                                   var simulationId: String = null,
                                   var start: Long = 0L,
                                   var maxDuration: DurationParam = null,
                                   var feeder: FeederParam = null,
                                   var filepath: String = null,
                                   var throttle: ThrottleParam = null,
                                    ) extends LoadMessage {

  val `type`: String = LoadTypes.GATLINGJOBS
  def getFilepath(): String = {
    StringUtils.notEmptyElse(filepath, StringUtils.EMPTY)
  }
  def isValid(): Exception = {
    if (null == multisScenarios || multisScenarios.isEmpty) {
      new RuntimeException("Empty` httpScenarios")
    }
    else {
      null
    }
  }
}
