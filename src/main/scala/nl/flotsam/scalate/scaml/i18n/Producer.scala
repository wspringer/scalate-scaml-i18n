package nl.flotsam.scalate.scaml.i18n

import org.apache.commons.lang.StringEscapeUtils
import java.util.Properties

trait Producer {

  def produce(part: String): String

}

object Producer {

  private class NoBundle extends Producer {
    def produce(part: String) = "\"" + StringEscapeUtils.escapeJava(part) + "\""
  }

  private class Bundle extends NoBundle {
    override def produce(part: String) = "bundle.translate(" + super.produce(part) + ")"
  }

  private class BundleCollecting(properties: Properties) extends Bundle {
    override def produce(part: String) = {
      properties.put(part, part)
      super.produce(part)
    }
  }

  def noBundle: Producer = new NoBundle
  def bundle: Producer = new Bundle
  def bundle(properties: Properties): Producer =
    new BundleCollecting(properties)

}



