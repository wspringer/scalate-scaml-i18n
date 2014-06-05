package nl.flotsam.scalate.scaml.i18n

import org.fusesource.scalate.scaml._
import org.fusesource.scalate.{Binding, TemplateSource, TemplateEngine}
import org.fusesource.scalate.support.{Text, Code}
import java.util.{Locale, ResourceBundle, Properties}
import org.fusesource.scalate.support.Code
import scala.util.DynamicVariable

class I18nScamlCodeGenerator(bundle: ResourceBundle) extends ScamlCodeGenerator {

  private val msgId = "msgid"
  private val localeBindingName = "locale"

  protected class I18nSourceBuilder() extends SourceBuilder {

    private val locale = new DynamicVariable(Locale.ENGLISH)

    override def generate(statement: Element): Unit = {
      statement.attributes.collectFirst {
        case (Text(`msgId`), LiteralText(text, _)) => text.mkString
      } flatMap { id => Option(bundle.getString(id)) } match {
        case Some(msg) =>
          super.generate(statement.copy(text = Some(LiteralText(text = List(Text(msg)), sanitize = Some(true)))))
        case None =>
          super.generate(statement)
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
