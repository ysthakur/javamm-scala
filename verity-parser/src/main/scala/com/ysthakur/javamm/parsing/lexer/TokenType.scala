package com.ysthakur.verity.parsing.lexer

import com.ysthakur.verity.parsing.ast.infile.Modifier

/**
 *
 *
 * textMatters - Whether or not it has a different text each time
 *                    or all tokens of this type have the same text
 */
sealed trait TokenType(val textMatters: Boolean)
sealed trait ValidIdentifierTokenType extends TokenType
sealed trait FixedTextTokenType(val text: String) extends TokenType {
  override val textMatters: Boolean = false
}
sealed trait IgnoredTokenType extends TokenType
sealed trait ModifierTokenType extends FixedTextTokenType {
  def toModifier: Modifier = Modifier.valueOf(this.text)
}

enum SymbolTokenType(symbol: String)
    extends /*java.lang.Enum[SymbolTokenType]
    with*/ FixedTextTokenType(symbol) {
  case LPAREN extends SymbolTokenType("(")
  case RPAREN extends SymbolTokenType(")")
  case LSQUARE extends SymbolTokenType("[")
  case RSQUARE extends SymbolTokenType("]")
  case LCURLY extends SymbolTokenType("{")
  case RCURLY extends SymbolTokenType("}")
  case COMMA extends SymbolTokenType(",")
  case SEMICOLON extends SymbolTokenType(";")
  case COLONX2 extends SymbolTokenType("::")
  case COLON extends SymbolTokenType(":")
  case PIPELINE extends SymbolTokenType("|>")
  case RT_ARROW extends SymbolTokenType("->")
  case DOT extends SymbolTokenType(".")
  case LTX2 extends SymbolTokenType("<<")
  case GTX3 extends SymbolTokenType(">>>")
  case GTX2 extends SymbolTokenType("|>>")
  case LTEQ extends SymbolTokenType("<=")
  case GTEQ extends SymbolTokenType(">=")
  case EQX3 extends SymbolTokenType("===")
  case EQX2 extends SymbolTokenType("==")
  case LT extends SymbolTokenType("<")
  case GT extends SymbolTokenType(">")
  case NOTEQ extends SymbolTokenType("!=")
  case EQ extends SymbolTokenType("=")
  case ANDX2 extends SymbolTokenType("&&")
  case ORX2 extends SymbolTokenType("||")
  case AND extends SymbolTokenType("&")
  case OR extends SymbolTokenType("|")
  case CARET extends SymbolTokenType("^")
  case EXCL_MARK extends SymbolTokenType("!")
  case TILDE extends SymbolTokenType("~")
  case PLUSX2 extends SymbolTokenType("++")
  case PLUS extends SymbolTokenType("+")
  case MINUSX2 extends SymbolTokenType("--")
  case MINUS extends SymbolTokenType("-")
  case STAR extends SymbolTokenType("*")
  case FWDSLASH extends SymbolTokenType("/")
  case BACKSLASH extends SymbolTokenType("\\")
  case QUESTION extends SymbolTokenType("?")
  case MODULO extends SymbolTokenType("%")
  case AT extends SymbolTokenType("@")
}

enum KeywordTokenType(text: String)
    extends java.lang.Enum[KeywordTokenType]
    with FixedTextTokenType(text) {
  case IMPORT extends KeywordTokenType("import")
  case PACKAGE extends KeywordTokenType("package")
  case PUBLIC extends KeywordTokenType("public") with ModifierTokenType
  case PRIVATE extends KeywordTokenType("private") with ModifierTokenType
  case PROTECTED extends KeywordTokenType("protected") with ModifierTokenType
  case THIS extends KeywordTokenType("this") with ModifierTokenType
  case SUPER extends KeywordTokenType("super") with ModifierTokenType
  case DEFAULT extends KeywordTokenType("default") with ModifierTokenType
  case EXTENDS extends KeywordTokenType("extends") with ModifierTokenType
  case STATIC extends KeywordTokenType("static") with ModifierTokenType
  case ABSTRACT extends KeywordTokenType("abstract") with ModifierTokenType
  case FINAL extends KeywordTokenType("final") with ModifierTokenType
  case NATIVE extends KeywordTokenType("native") with ModifierTokenType
  case TRANSIENT extends KeywordTokenType("transient") with ModifierTokenType
  case VOLATILE extends KeywordTokenType("volatile") with ModifierTokenType
  case SYNCHRONIZED extends KeywordTokenType("synchronized") with ModifierTokenType
  case CONST extends KeywordTokenType("const") with ModifierTokenType
  case RULE extends KeywordTokenType("rule") with ValidIdentifierTokenType
  case SWITCH extends KeywordTokenType("switch")
  case CASE extends KeywordTokenType("case")
  case WHILE extends KeywordTokenType("while")
  case FOR extends KeywordTokenType("for")
  case IF extends KeywordTokenType("if")
  case ELSE extends KeywordTokenType("else")
  case VAR extends KeywordTokenType("var")
  case CLASS extends KeywordTokenType("class")
  case TRAIT extends KeywordTokenType("trait")
  case ENUM extends KeywordTokenType("enum")
  case WHERE extends KeywordTokenType("where")
  case THROWS extends KeywordTokenType("throws")
  case IMPLIES extends KeywordTokenType("implies") with ValidIdentifierTokenType
  case ASSERT extends KeywordTokenType("assert")
  case NEW extends KeywordTokenType("new")
  case CONTINUE extends KeywordTokenType("continue")
  case BREAK extends KeywordTokenType("break")
  case THROW extends KeywordTokenType("throw")
  case RETURN extends KeywordTokenType("return")
  case AS extends KeywordTokenType("as")
  case IS extends KeywordTokenType("is")
  case NULL extends KeywordTokenType("null")
  case TRUE extends KeywordTokenType("true")
  case FALSE extends KeywordTokenType("false")
  case WITH extends KeywordTokenType("with") with ValidIdentifierTokenType
  case BOOL extends KeywordTokenType("boolean")
  case BYTE extends KeywordTokenType("byte")
  case SHORT extends KeywordTokenType("short")
  case INT extends KeywordTokenType("int")
  case CHAR extends KeywordTokenType("char")
  case FLOAT extends KeywordTokenType("float")
  case DOUBLE extends KeywordTokenType("double")
  case LONG extends KeywordTokenType("long")
  case VOID extends KeywordTokenType("void")
}

enum ReservedWord(text: String)
    extends java.lang.Enum[ReservedWord]
    with FixedTextTokenType(text) {
  case INSTANCEOF extends ReservedWord("instanceof")
  case UNDERSCORE extends ReservedWord("_") with ValidIdentifierTokenType
}

enum RegexTokenType(val regex: String) extends java.lang.Enum[RegexTokenType] with TokenType(true) {
  case WSP extends RegexTokenType("""\s+""") with IgnoredTokenType
  case VALID_ID extends RegexTokenType("""[A-Za-z_$][A-Za-z0-9_$]*""") with ValidIdentifierTokenType
  case NUM_LITERAL extends RegexTokenType("""-?[0-9]+(\.[0-9]+)?[FfDL]?""")
  case CHAR_LITERAL extends RegexTokenType("""'([^\\']|\\.)'""")
  case STR_LITERAL extends RegexTokenType(""""(\\.|[^\\"])*"""")
  case SINGLE_LINE_COMMENT extends RegexTokenType("""//.*?(\r\n|\r|\n|\Z)""") with IgnoredTokenType
  case DOC_COMMENT extends RegexTokenType("""/\*\*(.|\n|\r\n|\r)*?\*/""") with IgnoredTokenType
  case MULTILINE_COMMENT extends RegexTokenType("""/\*(.|\n|\r\n|\r)*?\*/""") with IgnoredTokenType
}

object FixedTextTokenType {
  def unapply(arg: FixedTextTokenType): Option[String] = Some(arg.text)
}
object RegexTokenType {
  def unapply(arg: RegexTokenType): Option[String] = Some(arg.regex)
}

import java.util.regex.{Matcher, Pattern}

object TokenTypeObj {}

object TokenType {

  // export com.ysthakur.verity.parsing.lexer.SymbolTokenType._
  // export com.ysthakur.verity.parsing.lexer.RegexTokenType._
  // export com.ysthakur.verity.parsing.lexer.KeywordTokenType._
  // export com.ysthakur.verity.parsing.lexer.ReservedWord._

  def tryMatch(tokenType: TokenType, input: StringBuilder, offset: Int): MatchResult = {
    try {
    (tokenType match {
      case ftt: FixedTextTokenType => tryMatchText(ftt.text)
      case rtt: RegexTokenType => tryMatchRegex(rtt.regex)
    })(input, offset)
    } catch {
      case e: Exception => {
        println(s"Caught exception, tokenType=$tokenType, input=$input")
        throw e
      }
    }
  }

  def tryMatchText(text: String)(sb: StringBuilder, offset: Int): MatchResult = {
    val input = sb.toString
    val inputLen = input.size
    val size = text.size
    if (size == text.size && text == input)
      return FullMatch(
        Match(input, offset, offset + size),
        couldMatchMore = false
      )
    else if (inputLen < size) {
      if (text.startsWith(input.toString)) return NeedsMore
    } else if (inputLen > size) {
      if (input.indexOf(text) == 0)
        return PartialMatch(Match(input, offset, offset + size))
    }
    NoMatch
  }

  def tryMatchRegex(regex: String)(input: CharSequence, start: Int): MatchResult = {
    val pattern = java.util.regex.Pattern.compile(s"^$regex$$").asInstanceOf[Pattern]
    val matcher = pattern.matcher(input).asInstanceOf[Matcher]
    if (input == " {" || regex == " {") {
      //println(s"Regex=$regex")
    }
    try {
      if (matcher.matches()) {
        if (regex == RegexTokenType.VALID_ID.regex) {
          //println(s"VALID ID MATCH! requireEnd=${matcher.requireEnd}")
        }
        FullMatch(
          Match(matcher.group().asInstanceOf[String], start, start + matcher.end.asInstanceOf[Int]),
          matcher.requireEnd()
        )
      }
      else if (matcher.find(0))
        PartialMatch(Match(matcher.group().asInstanceOf[String], start, start + matcher.end))
      else if (matcher.hitEnd()) {
        // if (regex == RegexTokenType.STR_LITERAL.regex) {
        //   println(s"String needs more! $matcher")
        // }
        NeedsMore
      } else NoMatch
    } catch {
      case e: StackOverflowError => throw e
    }
  }
}