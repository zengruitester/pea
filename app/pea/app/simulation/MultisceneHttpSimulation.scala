package pea.app.simulation


import io.gatling.core.Predef._
import io.gatling.core.action.builder._
import io.gatling.core.controller.inject.closed.ClosedInjectionStep
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.http.Predef._
import io.gatling.http.check.HttpCheck
import io.gatling.http.request.builder.HttpRequestBuilder
import io.gatling.core.structure.{ChainBuilder, PopulationBuilder}
import io.gatling.http.action.HttpRequestActionBuilder
import pea.app.gatling.PeaSimulation
import pea.app.model.multiscene.{MultisScenarioSingScenario, _}
import io.gatling.core.session.{Expression, Session}
import pea.app.actor.ResponseMonitorActor
import pea.app.model.params.{AssertionItem, DurationParam, HttpAssertionParam}
import pea.app.model.Injection
import pea.app.{PeaConfig, multisScenariosMessage}
import pea.common.util.StringUtils

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
/**
  * 多请求模板 场景唯一 后续场景可以为多个
  */

class MultisceneHttpSimulation extends PeaSimulation {
  override val description: String = getClass.getName
  val BASE_FILE_PATH = ""
  val KEY_BODY = "BODY"
  val KEY_STATUS = "STATUS"

  val scns = multisScenariosMessage.multisScenarios.map(multisScenario =>{
    multisScenario.`type` match {
      case MultisScenarios.TYPE_CONTEXT => getScenarioBuilder(multisScenario)
      case MultisScenarios.TYPE_SINGLE_REQUEST => null
    }
  })
//  println(scns)
//  setUp(scns)
//  setUp(scenario("").exec().inject(atOnceUsers(1)))

  def getScenarioBuilder(multisScenario:MultisScenarios): PopulationBuilder ={
    val scnName =  if (StringUtils.isNotEmpty(multisScenario.`name`)) {
      multisScenario.`name`
    } else {
      "parallel-%s".format(multisScenario.id)
    }

    val chains = getChains(multisScenario.requests)

    val scn = if(multisScenariosMessage.getFilepath().length!=0 || multisScenariosMessage.getFilepath() !=""){
      scenario(scnName).feed(csv(BASE_FILE_PATH+"/"+multisScenariosMessage.getFilepath()).batch.circular).exec(chains)
    } else {
      scenario(scnName).exec(chains)
    }
    val populationBuilders = if (isOpenInjectionModel(multisScenario)) {
      println(scn.inject(getOpenInjectionSteps(multisScenario)).protocols(http.disableCaching))
    } else {
      scn.inject(getClosedInjectionSteps(multisScenario)).protocols(http.disableCaching)
    }
    populationBuilders
  }
  def isOpenInjectionModel(multisScenario:MultisScenarios): Boolean = {
    val injections = multisScenario.injections
    if (null != injections && injections.nonEmpty) {
      val firstType = injections(0).`type`
      !(firstType.equals(Injection.TYPE_CONSTANT_CONCURRENT_USERS) ||
        firstType.equals(Injection.TYPE_RAMP_CONCURRENT_USERS) ||
        firstType.equals(Injection.TYPE_INCREMENT_CONCURRENT_USERS))
    } else {
      false
    }
  }

  def getOpenInjectionSteps(multisScenario:MultisScenarios): Seq[OpenInjectionStep] = {
    val injections = multisScenario.injections
    if (null != injections && injections.nonEmpty) {
      injections.map(injection => {
        val duration = injection.duration
        injection.`type` match {
          case Injection.TYPE_NOTHING_FOR => nothingFor(toFiniteDuration(duration))
          case Injection.TYPE_AT_ONCE_USERS => atOnceUsers(injection.users)

          case Injection.TYPE_RAMP_USERS => rampUsers(injection.users) during toFiniteDuration(duration)
          case Injection.TYPE_CONSTANT_USERS_PER_SEC => constantUsersPerSec(injection.users) during toFiniteDuration(duration)
          case Injection.TYPE_RAMP_USERS_PER_SEC => rampUsersPerSec(injection.from) to injection.to during toFiniteDuration(duration)
          case Injection.TYPE_HEAVISIDE_USERS => heavisideUsers(injection.users) during toFiniteDuration(duration)
          case Injection.TYPE_INCREMENT_USERS_PER_SEC =>
            if (isValid(injection.separatedByRampsLasting)) {
              incrementUsersPerSec(injection.users)
                .times(injection.times)
                .eachLevelLasting(toFiniteDuration(injection.eachLevelLasting))
                .separatedByRampsLasting(toFiniteDuration(injection.separatedByRampsLasting))
                .startingFrom(injection.from)
            } else {
              incrementUsersPerSec(injection.users)
                .times(injection.times)
                .eachLevelLasting(toFiniteDuration(injection.eachLevelLasting))
                .startingFrom(injection.from)
            }
        }
      })
    } else {
      Nil
    }
  }
  def getClosedInjectionSteps(multisScenario:MultisScenarios): Seq[ClosedInjectionStep] = {
    val injections = multisScenario.injections
    if (null != injections && injections.nonEmpty) {
      injections.map(injection => {
        val duration = injection.duration
        injection.`type` match {
          case Injection.TYPE_CONSTANT_CONCURRENT_USERS => constantConcurrentUsers(injection.users) during toFiniteDuration(duration)
          case Injection.TYPE_RAMP_CONCURRENT_USERS => rampConcurrentUsers(injection.from) to (injection.to) during toFiniteDuration(duration)
          case Injection.TYPE_INCREMENT_CONCURRENT_USERS =>
            if (isValid(injection.separatedByRampsLasting)) {
              incrementConcurrentUsers(injection.users)
                .times(injection.times)
                .eachLevelLasting(toFiniteDuration(injection.eachLevelLasting))
                .separatedByRampsLasting(toFiniteDuration(injection.separatedByRampsLasting))
                .startingFrom(injection.from)
            } else {
              incrementConcurrentUsers(injection.users)
                .times(injection.times)
                .eachLevelLasting(toFiniteDuration(injection.eachLevelLasting))
                .startingFrom(injection.from)
            }
        }
      })
    } else {
      Nil
    }
  }

  @inline
  def isValid(duration: DurationParam): Boolean = {
    null != duration && StringUtils.isNotEmpty(duration.unit) && duration.value >= 0
  }

  def toFiniteDuration(duration: DurationParam): FiniteDuration = {
    duration.unit match {
      case DurationParam.TIME_UNIT_MILLI => duration.value millis
      case DurationParam.TIME_UNIT_SECOND => duration.value seconds
      case DurationParam.TIME_UNIT_MINUTE => duration.value minutes
      case DurationParam.TIME_UNIT_HOUR => duration.value hours
    }
  }

  /**
    * 构造并行列表中的数据
    * @param multisScenarioSingScenario
    * @return
    */
  def getChains(multisScenarioSingScenario: Seq[MultisScenarioSingScenario]):ChainBuilder ={
    var test = List[ActionBuilder]()

    multisScenarioSingScenario.foreach(multisScenarioSingScenario =>{
      multisScenarioSingScenario.`type` match{
        case MultisScenarioSingScenario.TYPE_EXEC => {
          test = test :+ toRequestBuilder(multisScenarioSingScenario.request)
          test = test :+ toSessionBuilder()
        }
        case MultisScenarioSingScenario.TYPE_PAUSE => test = test :+ toPauseBuilder(multisScenarioSingScenario.pause)
        case MultisScenarioSingScenario.TYPE_DO_IF => test = test :+ toDoIfBuilder(multisScenarioSingScenario.conditional)
        case MultisScenarioSingScenario.TYPE_DO_IF_Equals => test = test :+ toDoIfEqualsBuilder(multisScenarioSingScenario.conditional)
        case MultisScenarioSingScenario.TYPE_DO_IF_OR_ELSE => test = test :+ toDoIfOrElseBuilder(multisScenarioSingScenario.conditional)
        case MultisScenarioSingScenario.TYPE_DO_IF_EQUALS_ORELSE => test = test :+ toDoIfEqualsOrElseBuilder(multisScenarioSingScenario.conditional)
        case MultisScenarioSingScenario.TYPE_DO_SWITCH => test = test :+ toDoSwitchBuilder(multisScenarioSingScenario.switch)
        case MultisScenarioSingScenario.TYPE_DO_SWITCH_OR_ELSE => test :+ toDoSwitchOrElseBuilder(multisScenarioSingScenario.switch)
        case MultisScenarioSingScenario.TYPE_RANDOM_SWITCH => test :+ toRandomSwitchBuilder(multisScenarioSingScenario.randome)
        case MultisScenarioSingScenario.TYPE_RANDOM_SWITCH_OR_ELSE => test :+ toRandomSwitchOrElseBuilder(multisScenarioSingScenario.randome)
        case MultisScenarioSingScenario.TYPE_UNIFORM_RANDOM_SWITCH => test :+ toUniformRandomSwitchBuilder(multisScenarioSingScenario.randome)
        case MultisScenarioSingScenario.TYPE_ROUND_ROBIN_SWITCH => test :+ toRoundRobinwitchBuilder(multisScenarioSingScenario.randome)
        case MultisScenarioSingScenario.TYPE_TRY_MAX => null
        case MultisScenarioSingScenario.TYPE_EXIT_BLOCK_ON_FAIL => null
        case MultisScenarioSingScenario.EXIT_HERE_IF_FAILED => null

      }
    })
    var aaa = ChainBuilder(test)
    aaa
  }

  def toSessionBuilder():SessionHookBuilder ={
    val data:Expression[Session] = session => {
      if (multisScenariosMessage.verbose && null != PeaConfig.responseMonitorActor && session.contains(KEY_BODY)) {
        val status = session(KEY_STATUS).as[Int]
        val response = session(KEY_BODY).as[String]
        PeaConfig.responseMonitorActor ! ResponseMonitorActor.formatResponse(status, response)
      }
      session
    }
    new SessionHookBuilder(data,true)
  }


  /**
    *
    * @param duration
    * @return
    */
  def toPauseBuilder(duration:MultisScenarioPauseParams):PauseBuilder ={
    val pauseBuilder = new PauseBuilder(duration.duration,None)
    pauseBuilder
  }


  def toDoIfBuilder(conditional:MultisScenarioSingleConditional):IfBuilder ={
    val ifData = conditional.ifcontext

    var condition = ifData.condition.`type` match {
      case ConditionalParams.TYPE_ORDINARY => getConditionOrdinaryExpression(ifData.condition)
      case ConditionalParams.TYPE_SESSION => getConditionSessionExpression(ifData.condition)
    }
    var thenNext = getChains(ifData.value)
    new IfBuilder(condition, thenNext, None)
  }
  def getConditionSessionExpression(newcondition:ConditionalParams):Expression[Boolean] ={
    newcondition.connect match {
      case ConditionalParams.CONNECT_TYPE_EQUALTO => session:Session => session(newcondition.key.toString).as[String]  == newcondition.value.toString
      case ConditionalParams.CONNECT_TYPE_NOT_EQUALTO => (session:Session) => session(newcondition.key.toString).as[String] != newcondition.value.toString
      case ConditionalParams.CONNECT_TYPE_CONTAIN => (session:Session) => session(newcondition.key.toString).as[String].contains(newcondition.value.toString)
      case ConditionalParams.CONNECT_TYPE_NOT_CONTAIN => (session:Session) => !session(newcondition.key.toString).as[String].contains(newcondition.value.toString)
    }
  }
  def getConditionOrdinaryExpression(newcondition:ConditionalParams):Expression[Boolean] ={
      newcondition.connect match {
        case ConditionalParams.CONNECT_TYPE_EQUALTO  => newcondition.key.toString == newcondition.value.toString
        case ConditionalParams.CONNECT_TYPE_NOT_EQUALTO => newcondition.key.toString != newcondition.value.toString
        case ConditionalParams.CONNECT_TYPE_CONTAIN => newcondition.key.toString.contains(newcondition.value.toString)
        case ConditionalParams.CONNECT_TYPE_NOT_CONTAIN => !newcondition.key.toString.contains(newcondition.value.toString)
      }

  }

  def toDoIfEqualsBuilder(conditional:MultisScenarioSingleConditional):IfBuilder ={
    val ifData = conditional.ifcontext


    var aa = ifData.condition.`type` match {
      case ConditionalParams.TYPE_ORDINARY => getConditionOrdinaryExpression(ifData.condition)
      case ConditionalParams.TYPE_SESSION => getConditionSessionExpression(ifData.condition)
    }
    var thenNext = getChains(ifData.value)
    var elseNext = getChains(conditional.elsecontext)
    new IfBuilder(aa, thenNext, Some(elseNext))
  }

  def toDoIfOrElseBuilder(conditional:MultisScenarioSingleConditional):IfBuilder ={
    null
  }

  def toDoIfEqualsOrElseBuilder(conditional:MultisScenarioSingleConditional):IfBuilder ={
    null
  }

  def toDoSwitchBuilder(conditional:MultisScenarioSingleSwitch):SwitchBuilder ={
    null
  }
  def toDoSwitchOrElseBuilder(conditional:MultisScenarioSingleSwitch):SwitchBuilder ={
    null
  }
  def toRandomSwitchBuilder(conditional:MultisScenarioSingleRandom):SwitchBuilder ={
    null
  }
  def toRandomSwitchOrElseBuilder(conditional:MultisScenarioSingleRandom):RandomSwitchBuilder ={
    null
  }
  def toUniformRandomSwitchBuilder(conditional:MultisScenarioSingleRandom):UniformRandomSwitchBuilder ={
    null
  }
  def toRoundRobinwitchBuilder(conditional:MultisScenarioSingleRandom):RoundRobinSwitchBuilder ={
    null
  }
  private def equalityCondition(actual: Expression[Any], expected: Expression[Any]): Expression[Boolean] =
    (session: Session) =>
      for {
        expected <- expected(session)
        actual <- actual(session)
      } yield expected == actual
//  private def doIf(condition: Expression[Boolean], thenNext: ChainBuilder, elseNext: Option[ChainBuilder]): IfBuilder = {
//    new IfBuilder(condition, thenNext, elseNext)
//  }
def getChecks(assertions:HttpAssertionParam): Seq[HttpCheck] = {
  val checks = ArrayBuffer[HttpCheck]()
  if (null != assertions) {
    if (null != assertions.status && null != assertions.status.list) {
      assertions.status.list.foreach(item => {
        item.op match {
          case AssertionItem.TYPE_EQ => checks += status.is(item.expect.asInstanceOf[Int])
          case _ =>
        }
      })
    }
    if (null != assertions.header && null != assertions.header.list) {
      assertions.header.list.foreach(item => {
        item.op match {
          case AssertionItem.TYPE_EQ => checks += header(item.path).is(item.expect.asInstanceOf[String])
          case _ =>
        }
      })
    }
    if (null != assertions.body && null != assertions.body.list) {
      assertions.body.list.foreach(item => {
        item.op match {
          case AssertionItem.TYPE_JSONPATH => checks += jsonPath(item.path).is(item.expect.asInstanceOf[String])
          case _ =>
        }
      })
    }
  }
  checks
}
  /**
    * 构造HttpRequestActionBuilder 请求数据
    * @param request
    * @return HttpRequestActionBuilder
    */
  def toRequestBuilder(request: MultisScenarioSingleRequest):HttpRequestActionBuilder ={
    val builder = if(request.getVirtualhost()!=null || request.virtualhost.length!=0){
      baseAction(request).virtualHost(request.getVirtualhost())
    }else{
      baseAction(request)
    }
    builder
  }

  /**
    * 基础请求方法构造
    */
  def baseAction(request: MultisScenarioSingleRequest):HttpRequestBuilder ={

    val builder = http(StringUtils.notEmptyElse(request.name, request.url))
      .httpRequest(request.method, request.url)
      .queryParamMap(request.getParams())
      .headers(request.getHeaders())
          .body(StringBody(request.getBody()))
          .check(getChecks(request.assertions): _*)
    if (multisScenariosMessage.verbose) {
      builder.check(
        bodyString.saveAs(KEY_BODY),
        status.saveAs(KEY_STATUS),
      )
    } else {
      builder
    }
  }




}
