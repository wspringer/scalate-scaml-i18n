package nl.flotsam.scalate.scaml.i18n

import org.fusesource.scalate.scaml._
import org.fusesource.scalate.{Binding, TemplateSource, TemplateEngine}
import org.fusesource.scalate.support.Text
import org.fusesource.scalate.support.Code
import scala.util.DynamicVariable

class I18nScamlCodeGenerator(producer: Producer) extends ScamlCodeGenerator {

  private val msgIdName = "msgid"

  protected class I18nSourceBuilder() extends SourceBuilder {

    private val msgId = new DynamicVariable[Option[String]](None)

    def gettext(part: String, msgId: Option[String]) = producer.produce(part, msgId)

    override def generateBindings(bindings: Traversable[Binding])(body: => Unit): Unit =
      super.generateBindings(bindings.toList ::: producer.bindings())(body)

    override def generate(statement: Element): Unit = {
      statement.attributes.collectFirst {
        case (Text(`msgIdName`), LiteralText(text, _)) => text.mkString
      } match {
        case Some(id) =>
          statement match {
            case Element(_, _, text, Nil, _, _) =>
              msgId.withValue(Some(id)) {
                super.generate(statement.copy(attributes = statement.attributes.filterNot(_._1 == Text(msgIdName))))
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
