package nl.flotsam.scalate.scaml.i18n

import org.fusesource.scalate.scaml._
import org.fusesource.scalate.{Binding, TemplateSource, TemplateEngine}
import org.fusesource.scalate.support.Text
import org.fusesource.scalate.support.Code
import scala.util.DynamicVariable

/**
 * A replacement for ScamlCodeGenerator, adding support for i18n and l10n.
 *
 * If you install this ScamlCodeGenerator instead of the default one, you get a couple of new capabilities:
 *
 * First of all, it offers a way to declaritively specify that the content of some elements should be looked up in
 * resource bundle. You do that by adding an attribute to the element for which you want the contents to come from a
 * resource bundle. The default name of that attribute is 'l10n', but the constructor allows you to set that to
 * something of your preference.
 *
 * As an example, suppose this is what you had before:
 *
 * {{{
 * <div>wonderful</div>
 * }}}
 *
 * Now, let's add that l10n attribute. (Note: you can leave out the text if you want the text to come from a resource
 * bundle, but there are reasons to leave it in. More on that later.)
 *
 * {{{
 * <div l10n="wonderful">wonderful</div>
 * }}}
 *
 * With this in place, add two files to the classpath. One named app.properties, containing:
 *
 * {{{
 *   wonderful=wonderful
 * }}}
 *
 * and a second one called app_de.properties, containing:
 *
 * {{{
 *   wonderful=wunderbar
 * }}}
 *
 * If you now invoke the layout function of the TemplateEngine without any additional properties, you will get:
 *
 * {{{
 * <div>wonderful</div>
 * }}}
 *
 * However, if you pass a locale property, and set it to Locale.GERMAN, you will get:
 *
 * {{{
 * <div>wunderbar</div>
 * }}}
 *
 * Note that by default, the l10n attribute will be dropped. If you don't want that, then you can keep it included by
 * specifying dropl10n = true on the constructor.
 *
 * @param handler
 * @param l10AttrName
 * @param dropl10n
 */
class I18nScamlCodeGenerator(handler: Handler = Handler.using("app"),
                             l10AttrName: String = "l10n",
                             dropl10n: Boolean = true,
                             includeLang: Boolean = true)
  extends ScamlCodeGenerator {

  protected class I18nSourceBuilder() extends SourceBuilder {

    private val attrNameAsText = Text(l10AttrName)

    /**
     * Dirty little hack to make sure we carry over the message id from the element to the code that is actually
     * expected to generate the call to get the text from a resource bundle.
     */
    private val msgId = new DynamicVariable[Option[String]](None)

    def gettext(part: String, msgId: Option[String]) = handler.handle(part, msgId)

    override def generateBindings(bindings: Traversable[Binding])(body: => Unit): Unit =
      super.generateBindings(bindings.toList ::: handler.bindings())(body)


    override def generate(statement: Element): Unit = {
      statement.attributes.collectFirst {
        case (`attrNameAsText`, LiteralText(text, _)) => text.mkString
      } match {
        case Some(id) =>
          statement match {
            case Element(_, _, text, Nil, _, _) =>
              msgId.withValue(Some(id)) {
                val l10nfiltered =
                  if (dropl10n) statement.attributes.filterNot(_._1 == attrNameAsText)
                  else statement.attributes
                val langIncluded =
                  handler.localName match {
                    case Some(localName) if includeLang =>
                      (Text("lang"), EvaluatedText(
                        code = Text(localName),
                        body = List.empty,
                        preserve = false,
                        sanitize = Some(true),
                        ugly = false
                      )) :: l10nfiltered
                    case _ => l10nfiltered
                  }
                super.generate(statement.copy(attributes = langIncluded))
              }
            case _ =>
              super.generate(statement)
          }
        case _ => super.generate(statement)
      }
    }

    override def generateTextExpression(statement: TextExpression, is_line: Boolean) {
      statement match {
        case s: LiteralText if msgId.value.isDefined => {
          if (is_line) {
            write_indent
          }
          var literal = true;
          for (part <- s.text) {
            // alternate between rendering literal and interpolated text
            flush_text
            if (literal) {
              if (!part.isEmpty) {
                val (leading, meat, trailing) = isolate(part)
                def insertIfNotEmpty(str: String) =
                  if (!str.isEmpty) this << "$_scalate_$_context <<< (\"" + str + "\")"
                insertIfNotEmpty(leading)
                this << "$_scalate_$_context <<< ( " :: gettext(meat, msgId.value) :: " );" :: Nil
                insertIfNotEmpty(trailing)
              }
              literal = false
            } else {
              s.sanitize match {
                case None =>
                  this << "$_scalate_$_context <<< ( " :: part :: " );" :: Nil
                case Some(true) =>
                  this << "$_scalate_$_context.escape( " :: part :: " );" :: Nil
                case Some(false) =>
                  this << "$_scalate_$_context.unescape( " :: part :: " );" :: Nil
              }
              literal = true
            }
          }
          if (is_line) {
            write_nl
          }
        }
        case _ => super.generateTextExpression(statement, is_line)
      }
    }

  }


  override def generate(engine: TemplateEngine, source: TemplateSource, bindings: Traversable[Binding]) = {
    val uri = source.uri
    val hamlSource = source.text
    val statements = (new ScamlParser).parse(hamlSource)
    val builder = new I18nSourceBuilder()
    builder.generate(engine, source, bindings, statements)
    Code(source.className, builder.code, Set(uri), builder.positions)
  }
}
