# SCAML Internationalization

Internationalization support for Scalate's SCAML implementation.

## The problem it addresses

Suppose that you have some text inside your SCAML file that needs to be localized:

    %p Wonderful

Suppose that you want to do all of that by hand. In that case, you could start by adding two attributes:

    -@ val bundle: java.util.ResourceBundle = …
    -@ val locale: java.util.Locale = …

If you'd have this, in place, you would have to replace your "Wonderful" text with this:

    %p= bundle.getString("wonderful", locale)

On top of that, you need to make sure you pass your ResourceBundle and Locale correctly when generating the output:

    engine.layout(source, Map("locale" -> Locale.GERMAN, "bundle" -> …))

And then, you obviously need to make sure that your "bundle" variable is actually pointing to a ResourceBundle that has a properties file for the given locale, containing the translation of the text identified with the key "wonderful" into the locale of your preference, say, German. If your bundle is called "app", then you'd need a file called `app_de.properties`, containing this:

    wonderful=Wunderbar

That's a lof work for getting a German version of "wonderful". This project is aiming to make it a little easier.
    
## How it addresses that problem

It tries to make it easier by changing Scalate to generate source code doing all of this, instead of having to code it by hand. This is how it works. 

If you use this project, then you will need to indicate the key of the snippet by adding some additional markup:

    %p(l10n="wonderful") Wonderful
    
This itself is obviously not going to have any effect. If you run this through Scalate, then the HTML rendered would be this:

    <p l10n="wonderful">Wonderful</p>
    
No big shakes. In order to see this project at work, we first need to change the configuration of the TemplateEngine object you're using:

    val engine = new TemplateEngine(defaultTemplateDirs)
    val generator = new I18nScamlCodeGenerator(
      handler = Handler.using("app"),
      dropl10n = true
    )
    engine.codeGenerators += "scaml" -> generator
    engine

If you run the same code through the TemplateEngine now, you will get code that tries to read a translation from the "app" ResourceBundle, getting it's files from the root of your classpath. (It currently will generate big honking exceptions if you don't have these files present on your classpath.)

Making sure it uses  a different locale is still done in the same way:

    engine.layout(source, Map("locale" -> Locale.GERMAN))
    
Mind you, the generator is just changing the way the normal SCAML to Scala generator normally works. It's not a preprocessor changing one version of SCAML into another version and only then generate the Scala code.

In order to have a first version your properties file (one with your default locale), you can slightly modify the Handler that got passed a few lines ago:

    val properties = new Properties
    val generator = new I18nScamlCodeGenerator(
      handler = Handler.using("app").collection(properties)
      dropl10n = true
    )

If you compile your templates with this TemplateEngine, the properties object will have the properties for which you need translations.

## Limitations

This is just a proof of concept. There are many more things that could be done to make this more useful:

__MessageFormat__

Perhaps expressions like these:

    -val appreciation = "super nice"
    %p(l10n="wonderful") This is just #{appreciation}!!!!

should be changed into:

    -val appreciation = "super nice"
    %p(l10n="wonderful")= new MessageFormat(bundle.getString("wonderful")).format(appreciation)

… and more things like that.

## Example project?

Sure, just check out [this project here](https://github.com/wspringer/unfiltered-scalate-i18n-test), based on Unfiltered.

