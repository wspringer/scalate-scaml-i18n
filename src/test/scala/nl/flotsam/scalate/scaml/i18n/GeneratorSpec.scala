package nl.flotsam.scalate.scaml.i18n

import org.specs2.mutable.Specification
import org.fusesource.scalate.{TemplateSource, TemplateEngine}
import java.io.File
import org.fusesource.scalate.util.FileResourceLoader
import org.fusesource.scalate.scaml.ScamlCodeGenerator
import java.util.{Locale, ResourceBundle, Properties}

class GeneratorSpec extends Specification {

  "The SCAML generator" should {

    "allow you to generate valid Scalate code" in {
      val props = new Properties
      val generator = new I18nScamlCodeGenerator(Handler.using("test").collecting(props))
      val workingDir = new File(System.getProperty("user.dir"))
      val testResourceDir = new File(workingDir, "src/test/resources")
      val engine = new TemplateEngine(List(testResourceDir))
      val loader = new FileResourceLoader(List(testResourceDir))
      val source = TemplateSource.fromUri("test.scaml", loader)
      source.engine = engine
      println(generator.generate(engine, source, List.empty).toString())
      engine.codeGenerators += "scaml" -> generator
      println(engine.layout(source))
      println(engine.layout(source, Map("locale" -> Locale.GERMAN)))
      println(props.toString)
      ok
    }

  }

}
