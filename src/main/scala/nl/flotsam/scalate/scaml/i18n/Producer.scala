package nl.flotsam.scalate.scaml.i18n

import org.apache.commons.lang.StringEscapeUtils
import java.util.Properties
import org.fusesource.scalate.Binding

trait Producer {

  def produce(part: String, msgId: Option[String]): String

  def bindings(): List[Binding]

}

object Producer {

  private class NoBundle extends Producer {
    def produce(part: String, msgId: Option[String]) = "\"" + StringEscapeUtils.escapeJava(part) + "\""
    override def bindings(): List[Binding] = List.empty
  }

  private class ResourceBundle extends NoBundle {
    override def produce(part: String, msgId: Option[String]) = "bundle.getString(\"" + msgId.get + "\")"

    override def bindings(): List[Binding] = List(
      new Binding(
        name = "locale",
        className = "java.util.Locale",
        defaultValue = Some("java.util.Locale.ENGLISH")
      ),
      new Binding(
        name = "bundle",
        className = "java.util.ResourceBundle",
        defaultValue = Some("java.util.ResourceBundle.getBundle(\"test\", locale)")
      )
    )
  }

  private class BundleCollecting(properties: Properties) extends ResourceBundle {
    override def produce(part: String, msgId: Option[String]) = {
      properties.put(part, part)
      super.produce(part, msgId)
    }
  }

  def noBundle: Producer = new NoBundle
  def bundle: Producer = new ResourceBundle
  def bundle(properties: Properties, msgId: Option[String]): Producer =
    new BundleCollecting(properties)

}



