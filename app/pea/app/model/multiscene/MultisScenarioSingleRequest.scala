package pea.app.model.multiscene

import pea.app.model.params.HttpAssertionParam
import pea.common.util.StringUtils

case class MultisScenarioSingleRequest(
                          var name: String= null,
                          var url: String= null,
                          var method: String= null,
                          var headers: Map[String, String]= null,
                          var body: String= null,
                          var virtualhost: String= null,
                          var params: Map[String, String]= null,
                          var assertions: HttpAssertionParam = null,
                          var parentid: Long,
                          var id: Long,
                        ) {

  def getHeaders(): Map[String, String] = {
    if (null == headers) Map.empty else headers
  }

  def getBody(): String = {
    StringUtils.notEmptyElse(body, StringUtils.EMPTY)
  }
  def getVirtualhost(): String = {
    StringUtils.notEmptyElse(virtualhost, StringUtils.EMPTY)
  }
  def getParams(): Map[String, String] = {
    if (null == params) Map.empty else params
  }
}
