package nl.flotsam.scalate.scaml.i18n

/**
 * The interface to be implemented by objects capable of providing a translation for a String based on a certain locale.
 * Note that at this stage, the locale should have already been picked.
 */
trait Bundle {

  def translate(str: String): String

}
