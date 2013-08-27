package nl.flotsam.scalate.scaml

package object i18n {

  /**
   * Think of it as a takeWhile meeting partition. It takes the
   * characters matching the criterion, and returns a tuple consisting
   * of the leading matching characters and the remainder.
   */
  def cut(str: String, predicate: Char => Boolean): (String, String) = {
    val builder = new StringBuilder
    var pos = 0
    val length = str.length
    while (pos < length && predicate(str.charAt(pos))) {
      builder.append(str.charAt(pos))
      pos += 1
    }
    (builder.toString, str.substring(pos))
  }

  /**
   * Returns leading whitespace, the non-whitespace portion and trailing whitespace.
   */
  def isolate(str: String): (String, String, String) = {
    val (leading, remainder) = cut(str, _.isWhitespace)
    val (trailing, meat) = cut(remainder.reverse, _.isWhitespace)
    (leading, meat.reverse, trailing.reverse)
  }

}
