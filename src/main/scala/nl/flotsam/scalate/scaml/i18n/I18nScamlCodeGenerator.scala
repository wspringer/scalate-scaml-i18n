package nl.flotsam.scalate.scaml.i18n

import org.fusesource.scalate.scaml.{LiteralText, TextExpression, ScamlParser, ScamlCodeGenerator}
import org.fusesource.scalate.{Binding, TemplateSource, TemplateEngine}
import org.fusesource.scalate.support.{Text, Code}

class I18nScamlCodeGenerator extends ScamlCodeGenerator {

  protected class I18nSourceBuilder extends SourceBuilder {
    override def generateTextExpression(statement: TextExpression, is_line: Boolean) {
      statement match {
        case s: LiteralText => {
          println("Literal!!!")
          if (is_line) {
            write_indent
          }
          var literal = true;
          for (part <- s.text) {
            // alternate between rendering literal and interpolated text
            if (literal) {
              writeSanitized(part, None, literal)
              literal = false
            } else {
              flush_text
              writeSanitized(part, s.sanitize, literal)
              literal = true
            }
          }
          if (is_line) {
            write_nl
          }
        }
        case _ =>
          super.generateTextExpression(statement, is_line)
      }
    }

    private def writeSanitized(part: Text, sanitize: Option[Boolean], literal: Boolean) {
      println(isolate(part.toString))
      val (leading, meat, trailing) = isolate(part.toString)
      this << "$_scalate_$_context <<< (\"" + leading + "\");" :: Nil
      println("LEADING: \"" + leading + "\"")
      println("MEAT: " + meat)
      println("TRAILING: " + leading)
      val trimmed = new Text(meat).setPos(part.pos)
      (sanitize, literal) match {
        case (None, false) =>
          this << "$_scalate_$_context <<< ( gettext(" + trimmed + ") );" :: Nil
        case (Some(true), false) =>
          this << "$_scalate_$_context.escape( gettext(" + trimmed + ") );" :: Nil
        case (Some(false), false) =>
          this << "$_scalate_$_context.unescape( gettext(" + trimmed + ") );" :: Nil
        case (_, true) =>
          this << "$_scalate_$_context <<< ( gettext(\"" + trimmed + "\") );" :: Nil
      }
      this << "$_scalate_$_context <<< (\"" + trailing + "\");" :: Nil
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
