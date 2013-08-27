package nl.flotsam.scalate.scaml.i18n

import org.fusesource.scalate.scaml.{LiteralText, TextExpression, ScamlParser, ScamlCodeGenerator}
import org.fusesource.scalate.{Binding, TemplateSource, TemplateEngine}
import org.fusesource.scalate.support.{Text, Code}

class I18nScamlCodeGenerator extends ScamlCodeGenerator {

  protected class I18nSourceBuilder extends SourceBuilder {
    override def generateTextExpression(statement: TextExpression, is_line: Boolean) {
      statement match {
        case s: LiteralText => {
          if (is_line) {
            write_indent
          }
          var literal = true;
          def gettext(part: String) = "gettext(\"" + part + "\")"
          for (part <- s.text) {
            // alternate between rendering literal and interpolated text
            flush_text
            if (literal) {
              if (!part.isEmpty) {
                val (leading, meat, trailing) = isolate(part)
                def insertIfNotEmpty(str: String) =
                  if (!str.isEmpty) this << "$_scalate_$_context <<< (\"" + str + "\")"
                insertIfNotEmpty(leading)
                this << "$_scalate_$_context <<< ( " :: gettext(meat) :: " );" :: Nil
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
