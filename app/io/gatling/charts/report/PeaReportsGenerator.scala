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

import java.nio.file.Path

import io.gatling.charts.component.ComponentLibrary
import io.gatling.charts.config.ChartsFiles.{globalFile, menuFile}
import io.gatling.charts.template.{MenuTemplate, PageTemplate}
import io.gatling.commons.stats.RequestStatsPath
import io.gatling.commons.util.ScanHelper.deepCopyPackageContent
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingFiles._
import pea.app.gatling.PeaRequestStatistics

private[gatling] class PeaReportsGenerator(implicit configuration: GatlingConfiguration) {

  def generateFor(reportsGenerationInputs: ReportsGenerationInputs): (Path, PeaRequestStatistics) = {
    import reportsGenerationInputs._

    def hasAtLeastOneRequestReported: Boolean =
      logFileReader.statsPaths.exists(_.isInstanceOf[RequestStatsPath])

    def generateMenu(): Unit = new TemplateWriter(menuFile(reportFolderName)).writeToFile(new MenuTemplate().getOutput)

    def generateStats(): PeaRequestStatistics = new PeaStatsReportGenerator(reportsGenerationInputs, ComponentLibrary.Instance).generate()

    def generateAssertions(): Unit = new AssertionsReportGenerator(reportsGenerationInputs, ComponentLibrary.Instance).generate()

    def copyAssets(): Unit = {
      deepCopyPackageContent(GatlingAssetsStylePackage, styleDirectory(reportFolderName))
      deepCopyPackageContent(GatlingAssetsJsPackage, jsDirectory(reportFolderName))
    }

    if (!hasAtLeastOneRequestReported)
      throw new UnsupportedOperationException("There were no requests sent during the simulation, reports won't be generated")

    val reportGenerators =
      List(
        new AllSessionsReportGenerator(reportsGenerationInputs, ComponentLibrary.Instance),
        new GlobalReportGenerator(reportsGenerationInputs, ComponentLibrary.Instance),
        new RequestDetailsReportGenerator(reportsGenerationInputs, ComponentLibrary.Instance),
        new GroupDetailsReportGenerator(reportsGenerationInputs, ComponentLibrary.Instance)
      )

    copyAssets()
    generateMenu()
    PageTemplate.setRunInfo(logFileReader.runMessage, logFileReader.runEnd)
    reportGenerators.foreach(_.generate())
    val statistics = generateStats()
    generateAssertions()

    (globalFile(reportFolderName), statistics)
  }
}
