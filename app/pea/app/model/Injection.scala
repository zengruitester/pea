package pea.app.model

import pea.app.model.params.DurationParam

// https://gatling.io/docs/current/general/simulation_setup
case class Injection(
                      var `type`: String,
                      var users: Int,
                      var from: Int = 0,
                      var to: Int = 0,
                      var duration: DurationParam = null,
<<<<<<< HEAD
                      var time: Int =1,
=======
                      var times: Int = 0,
                      var eachLevelLasting: DurationParam = null,
                      var separatedByRampsLasting: DurationParam = null,
>>>>>>> 6669f6af77d9bd4dee249ffa19cc2e781291e79b
                    )

object Injection {

  // Open Model
  val TYPE_NOTHING_FOR = "nothingFor"
  val TYPE_AT_ONCE_USERS = "atOnceUsers"
  val TYPE_RAMP_USERS = "rampUsers"
  val TYPE_CONSTANT_USERS_PER_SEC = "constantUsersPerSec"
  val TYPE_RAMP_USERS_PER_SEC = "rampUsersPerSec"
<<<<<<< HEAD
  val TYPE_INCREMENT_USERS_PERSEC = "incrementUsersPerSec"
=======
  val TYPE_HEAVISIDE_USERS = "heavisideUsers"
  val TYPE_INCREMENT_USERS_PER_SEC = "incrementUsersPerSec" // meta

  // Close Model
  val TYPE_CONSTANT_CONCURRENT_USERS = "constantConcurrentUsers"
  val TYPE_RAMP_CONCURRENT_USERS = "rampConcurrentUsers"
  val TYPE_INCREMENT_CONCURRENT_USERS = "incrementConcurrentUsers" // meta
>>>>>>> 6669f6af77d9bd4dee249ffa19cc2e781291e79b
}
