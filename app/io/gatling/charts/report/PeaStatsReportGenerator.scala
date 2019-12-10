/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.charts.report

import com.typesafe.scalalogging.StrictLogging
import io.gatling.charts.component.{ComponentLibrary, GroupedCount, RequestStatistics, Statistics}
import io.gatling.charts.config.ChartsFiles._
import io.gatling.charts.stats.RequestPath
import io.gatling.charts.template.{ConsoleTemplate, GlobalStatsJsonTemplate, StatsJsTemplate}
import io.gatling.commons.stats._
import io.gatling.commons.util.NumberHelper._
import io.gatling.core.config.GatlingConfiguration
import pea.app.gatling.PeaRequestStatistics
import pea.app.gatling.PeaRequestStatistics.{PeaGroupedCount, PeaStatistics}

import scala.collection.breakOut

private[charts] class PeaStatsReportGenerator(reportsGenerationInputs: ReportsGenerationInputs, componentLibrary: ComponentLibrary)(implicit configuration: GatlingConfiguration) extends StrictLogging {

  import reportsGenerationInputs._

  def generate(): PeaRequestStatistics = {

    def percentiles(rank: Double, title: Double => String, total: GeneralStats, ok: GeneralStats, ko: GeneralStats) =
      Statistics(title(rank), total.percentile(rank), ok.percentile(rank), ko.percentile(rank))

    def computeRequestStats(name: String, requestName: Option[String], group: Option[Group]): RequestStatistics = {

      val total = logFileReader.requestGeneralStats(requestName, group, None)
      val ok = logFileReader.requestGeneralStats(requestName, group, Some(OK))
      val ko = logFileReader.requestGeneralStats(requestName, group, Some(KO))

      val numberOfRequestsStatistics = Statistics("request count", total.count, ok.count, ko.count)
      val minResponseTimeStatistics = Statistics("min response time", total.min, ok.min, ko.min)
      val maxResponseTimeStatistics = Statistics("max response time", total.max, ok.max, ko.max)
      val meanResponseTimeStatistics = Statistics("mean response time", total.mean, ok.mean, ko.mean)
      val stdDeviationStatistics = Statistics("std deviation", total.stdDev, ok.stdDev, ko.stdDev)

      val percentilesTitle = (rank: Double) => s"response time ${rank.toRank} percentile"

      val percentiles1 = percentiles(configuration.charting.indicators.percentile1, percentilesTitle, total, ok, ko)
      val percentiles2 = percentiles(configuration.charting.indicators.percentile2, percentilesTitle, total, ok, ko)
      val percentiles3 = percentiles(configuration.charting.indicators.percentile3, percentilesTitle, total, ok, ko)
      val percentiles4 = percentiles(configuration.charting.indicators.percentile4, percentilesTitle, total, ok, ko)
      val meanNumberOfRequestsPerSecondStatistics = Statistics("mean requests/sec", total.meanRequestsPerSec, ok.meanRequestsPerSec, ko.meanRequestsPerSec)

      val groupedCounts = logFileReader
        .numberOfRequestInResponseTimeRange(requestName, group).map {
        case (rangeName, count) => GroupedCount(rangeName, count, total.count)
      }

      val path = requestName match {
        case Some(n) => RequestPath.path(n, group)
        case None => group.map(RequestPath.path).getOrElse("")
      }

      RequestStatistics(name, path, numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanResponseTimeStatistics, stdDeviationStatistics, percentiles1, percentiles2, percentiles3, percentiles4, groupedCounts, meanNumberOfRequestsPerSecondStatistics)
    }

    def computeGroupStats(name: String, group: Group): RequestStatistics = {

      def groupStatsFunction: (Group, Option[Status]) => GeneralStats =
        if (configuration.charting.useGroupDurationMetric) {
          logger.debug("Use group duration stats.")
          logFileReader.groupDurationGeneralStats
        } else {
          logger.debug("Use group cumulated response time stats.")
          logFileReader.groupCumulatedResponseTimeGeneralStats
        }

      val total = groupStatsFunction(group, None)
      val ok = groupStatsFunction(group, Some(OK))
      val ko = groupStatsFunction(group, Some(KO))

      val numberOfRequestsStatistics = Statistics("numberOfRequests", total.count, ok.count, ko.count)
      val minResponseTimeStatistics = Statistics("minResponseTime", total.min, ok.min, ko.min)
      val maxResponseTimeStatistics = Statistics("maxResponseTime", total.max, ok.max, ko.max)
      val meanResponseTimeStatistics = Statistics("meanResponseTime", total.mean, ok.mean, ko.mean)
      val stdDeviationStatistics = Statistics("stdDeviation", total.stdDev, ok.stdDev, ko.stdDev)

      val percentiles1 = percentiles(configuration.charting.indicators.percentile1, _ => "percentiles1", total, ok, ko)
      val percentiles2 = percentiles(configuration.charting.indicators.percentile2, _ => "percentiles2", total, ok, ko)
      val percentiles3 = percentiles(configuration.charting.indicators.percentile3, _ => "percentiles3", total, ok, ko)
      val percentiles4 = percentiles(configuration.charting.indicators.percentile4, _ => "percentiles4", total, ok, ko)
      val meanNumberOfRequestsPerSecondStatistics = Statistics("meanNumberOfRequestsPerSecond", total.meanRequestsPerSec, ok.meanRequestsPerSec, ko.meanRequestsPerSec)

      val groupedCounts = logFileReader
        .numberOfRequestInResponseTimeRange(None, Some(group)).map {
        case (rangeName, count) => GroupedCount(rangeName, count, total.count)
      }

      val path = RequestPath.path(group)

      RequestStatistics(name, path, numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanResponseTimeStatistics, stdDeviationStatistics, percentiles1, percentiles2, percentiles3, percentiles4, groupedCounts, meanNumberOfRequestsPerSecondStatistics)
    }

    val rootContainer = GroupContainer.root(computeRequestStats(GlobalPageName, None, None))

    val statsPaths = logFileReader.statsPaths

    val groupsByHierarchy: Map[List[String], Group] = statsPaths
      .collect {
        case GroupStatsPath(group) => group
        case RequestStatsPath(_, Some(group)) => group
      }.map(group => group.hierarchy.reverse -> group)(breakOut)

    val seenGroups = collection.mutable.HashSet.empty[List[String]]

    def addGroupsRec(hierarchy: List[String]): Unit = {

      if (!seenGroups.contains(hierarchy)) {
        seenGroups += hierarchy

        hierarchy match {
          case _ :: tail if tail.nonEmpty => addGroupsRec(tail)
          case _ =>
        }

        val group = groupsByHierarchy(hierarchy)
        val stats = computeGroupStats(group.name, group)
        rootContainer.addGroup(group, stats)
      }
    }

    val requestStatsPaths = statsPaths.collect { case path: RequestStatsPath => path }
    requestStatsPaths.foreach {
      case RequestStatsPath(request, group) =>
        group.foreach { group =>
          addGroupsRec(group.hierarchy.reverse)
        }
        val stats = computeRequestStats(request, Some(request), group)
        rootContainer.addRequest(group, request, stats)
    }

    new TemplateWriter(statsJsFile(reportFolderName)).writeToFile(new StatsJsTemplate(rootContainer, false).getOutput(configuration.core.charset))
    new TemplateWriter(statsJsonFile(reportFolderName)).writeToFile(new StatsJsTemplate(rootContainer, true).getOutput(configuration.core.charset))
    new TemplateWriter(globalStatsJsonFile(reportFolderName)).writeToFile(new GlobalStatsJsonTemplate(rootContainer.stats, true).getOutput)
    println(ConsoleTemplate.println(rootContainer.stats, logFileReader.errors(None, None)))
    // modified by pea
    toRequestStatistics(rootContainer.stats)
  }

  def toRequestStatistics(stats: RequestStatistics): PeaRequestStatistics = {
    PeaRequestStatistics(
      name = stats.name,
      path = stats.path,
      numberOfRequestsStatistics = toPeaStatistics(stats.numberOfRequestsStatistics),
      minResponseTimeStatistics = toPeaStatistics(stats.minResponseTimeStatistics),
      maxResponseTimeStatistics = toPeaStatistics(stats.maxResponseTimeStatistics),
      meanStatistics = toPeaStatistics(stats.meanStatistics),
      stdDeviationStatistics = toPeaStatistics(stats.stdDeviationStatistics),
      percentiles1 = toPeaStatistics(stats.percentiles1),
      percentiles2 = toPeaStatistics(stats.percentiles2),
      percentiles3 = toPeaStatistics(stats.percentiles3),
      percentiles4 = toPeaStatistics(stats.percentiles4),
      groupedCounts = stats.groupedCounts.map(item => PeaGroupedCount(item.name, item.count, item.total)),
      meanNumberOfRequestsPerSecondStatistics = toPeaStatistics(stats.meanNumberOfRequestsPerSecondStatistics),
    )
  }

  def toPeaStatistics[T: Numeric](stats: Statistics[T]): PeaStatistics[T] = {
    PeaStatistics(stats.name, stats.total, stats.success, stats.failure)
  }
}
