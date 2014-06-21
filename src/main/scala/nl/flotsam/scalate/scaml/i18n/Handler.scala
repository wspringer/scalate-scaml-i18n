package nl.flotsam.scalate.scaml.i18n

import org.apache.commons.lang.StringEscapeUtils
import java.util.Properties
import org.fusesource.scalate.Binding

/**
 * The I18nScamlCodeGenerator will locate elements with a special attribute, giving the message identifier. If that is
 * included, then we need to generate a bundle lookup instead of generating the text to be printed. The Handler
 * implementation is responsible for doing the right thing.
 */
trait Handler {

  /**
   * Generates the Scala code to print the given String. Might generate a ResourceBundle lookup.
   * @param text The text to be rendered.
   * @param msgId The message id of the text to be rendered.
   * @return The Scala expression getting the text.
   */
  def handle(text: String, msgId: Option[String]): String

  def localName: Option[String]

  /**
   * Different handlers might have different needs of objects to be bound by default. This allows the Handler to
   * specify what it needs.
   */
  def bindings(): List[Binding]

  /**
   * Sometimes you want the Handler to remember all the message ids and strings it visited, like when generating
   * a default properties file. By calling this method, you can put the Handler in a state where it is gathering all
   * of that.
   */
  def collecting(properties: Properties) = new Handler {
    override def handle(text: String, msgId: Option[String]): String = {
      msgId.foreach(properties.put(_, text))
      Handler.this.handle(text, msgId)
    }

    override def bindings(): List[Binding] = Handler.this.bindings()

    override def localName: Option[String] = Handler.this.localName

  }

}

object Handler {

  /**
   * Return a Handler that uses the ResourceBundle with the given name.
   *
   * @param bundle The name of the bundle.
   * @param bundleName The variable name used inside the generated code to access the bundle.
   * @param localeName The variable name used inside the code to access the current locale.
   */
  def using(bundle: String,
            bundleName: String = "bundle",
            localeName: String = "locale"
  ) = new Handler {

    def handle(text: String, msgId: Option[String]) = msgId match {
      case Some(id) => "bundle.getString(\"" + msgId.get + "\")"
      case None => "\"" + StringEscapeUtils.escapeJava(text) + "\""
    }

    override def localName: Option[String] = Some(localeName)

    def bindings(): List[Binding] = List(
      new Binding(
        name = localeName,
        className = "java.util.Locale",
        defaultValue = Some("java.util.Locale.ENGLISH")
      ),
      new Binding(
        name = bundleName,
        className = "java.util.ResourceBundle",
        defaultValue = Some("java.util.ResourceBundle.getBundle(\"" + bundle + "\", locale)")
      )
    )
  }

  /**
   * Returns a Handler that will not do a lookup.
   */
  def withoutLookup = new Handler {
    def handle(text: String, msgId: Option[String]) = "\"" + StringEscapeUtils.escapeJava(text) + "\""
    override def bindings(): List[Binding] = List.empty
    override def localName: Option[String] = None
  }

}



