package asura.pea.model

import asura.common.util.StringUtils

/** node data
  *
  * @param status node status
  * @param runId  report id of last job
  * @param start  start time of last job
  * @param end    end time of last job
  * @param code   code of last job
  * @param errMsg error message of last job
  */
case class MemberStatus(
                         var status: String = MemberStatus.WORKER_IDLE,
                         var runId: String = StringUtils.EMPTY,
                         var start: Long = 0L,
                         var end: Long = 0L,
                         var code: Int = 0,
                         var errMsg: String = null,
                       )

object MemberStatus {

  val WORKER_IDLE = "idle"
  val WORKER_RUNNING = "running"

  val REPORTER_RUNNING = WORKER_RUNNING
  val REPORTER_REPORTING = "reporting"
  val REPORTER_FINISHED = "finished"

  // extra worker status in reporter
  val REPORTER_WORKER_IIL = "ill"
  val REPORTER_WORKER_GATHERING = "gathering"
  val REPORTER_WORKER_FINISHED = REPORTER_FINISHED
}
