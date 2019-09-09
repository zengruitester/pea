package asura.pea.dubbo

import asura.pea.dubbo.api.GreetingService
import asura.pea.dubbo.provider.GreetingsServiceImpl
import com.alibaba.dubbo.config.{ApplicationConfig, RegistryConfig, ServiceConfig}

import scala.io.StdIn

object GreetingProviderApp extends RegistryAddressConfig {

  def main(args: Array[String]): Unit = {
    val service = new ServiceConfig[GreetingService]()
    service.setApplication(new ApplicationConfig("pea-dubbo-provider"))
    service.setRegistry(new RegistryConfig(RegistryAddressNA))
    service.setInterface(classOf[GreetingService])
    service.setRef(new GreetingsServiceImpl())
    service.export()
    println(s"${service.getInterface}: ${service.getExportedUrls}")
    StdIn.readLine()
  }
}
