package com.ysthakur.javamm.parsing.lexer

import java.io.{BufferedInputStream, File, FileInputStream, FileWriter, IOException, InputStream}

import com.ysthakur.javamm.CompilationError
import com.ysthakur.javamm.parsing._
import com.ysthakur.javamm.parsing.lexer.TokenType

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks.breakable

case class Lexer(file: BufferedInputStream, logFile: String = "./log.txt") {
  private[lexer] var lastToken: Token[_] = _
  private var position = Position(0, 0, 0)
  private val LOG = new FileWriter(File(logFile))

  def this(file: File) = this(new BufferedInputStream(new FileInputStream(file)))

  @throws[IOException]
  def getNext: Option[Char] = {
    val res = file.read()
    //System.out.println(s"($res,${res.toChar})`")
    if (res == -1) None else Some(res.toChar)
  }

  def end(): Unit = file.close()

  /**
    * The last match it had. Includes the [[com.ysthakur.javamm.parsing.grammar.PatternCase]] and the text
    * that was matched and a tuple in the format (`startOffset`, `endOffset`).
    * The end offset's not inclusive.
    */
  var lastMatch: Match = _
  var offset: Int = 0
  private[parsing] var lastInput: mutable.StringBuilder = StringBuilder()

  def tokenize(): Iterable[Token[?]] = {
    try {
      return tokenize_()
    } catch {
      case e: java.util.regex.PatternSyntaxException => {
        //println("`" + e.getPattern + "`")
        end()
        throw e
      }
    } finally {
      //println("asdkfja;sldjf;askljdf")
      end()
    }
  }

  /**
    *
    */
  @throws[BadCharacterError]
  def tokenize_(): Iterable[Tok] = {
    var lastInput = StringBuilder().append(getNext match {
      case Some(c) => c
      case None => return List.empty
    })
    val tokens = ListBuffer[Token[?]]()
    val tokenTypes = 
      (ReservedWord.values.toIterable ++
        SymbolTokenType.values.toIterable ++
        KeywordTokenType.values.toIterable ++
        RegexTokenType.values.toIterable)
        .asInstanceOf[Iterable[java.lang.Enum[_] with TokenType]]
    log(s"Last input = $lastInput, ${lastInput(0).toInt}")
    while (lastInput.nonEmpty ||
        (getNext match {
          case Some(next) =>
            log(s"Next = $next")
            lastInput = new StringBuilder().append(next)
            true
          case None => false
       })
    ) {
      val (matched: Match, tokenType: TokenType, range: TextRange, acc: StringBuilder) =
        tryMatch(tokenTypes, position.copy(), lastInput).getOrElse(
            throw BadCharacterError(lastInput.head), position.row, position.col, this.offset)
      if (!tokenType.isInstanceOf[IgnoredTokenType]) {
        tokens.addOne(tokenType match {
          case textTokenType: FixedTextTokenType =>
            InvariantToken(textTokenType, range)
          case regexTokenType: RegexTokenType =>
            VariantToken(regexTokenType, matched.matched.toString, range)
          case _ => throw new CompilationError("Oops! Match error")
        })
      }
      this.position = range.end
      lastInput = new StringBuilder(acc.substring(0))
      log(s"Matched = $matched, lastInput=`$lastInput`")
    }
    end()
    tokens.toList
  }

  def tryMatch(
      tokenTypes: Iterable[TokenType],
      startPos: Position,
      lastInput: StringBuilder
  ): Option[(Match, TokenType, TextRange, StringBuilder)] = {
    val acc = StringBuilder(lastInput.toString)
    log(s"Entering, acc=`$acc`")

    var lastMatch: (Match, TokenType)|Null = null
    var (lastMatchLength, currentLength) = (0, 1)
    var lastPos: Position = startPos.copy()
    var lastChar: Char = -1.toChar
    
    var currentPos: Position = startPos.copy()
    val origOffset = startPos.copy()

    var possibleFutureMatches = mutable.Set[TokenType]()
    while ({
      breakable {
          for (tokenType <- tokenTypes)
            TokenType.tryMatch(tokenType, acc, currentPos.offset) match {
              case FullMatch(matched: Match, couldMatchMore: Boolean) =>
                {
                  log(s"Full match! $matched, tokentype=$tokenType")
                  if (lastMatchLength < currentLength) {
                    lastMatch = (matched, tokenType)
                    lastPos = currentPos.copy()
                    lastMatchLength = currentLength
                    log(s"Setting to lastMatch, currentLength=$currentLength")
                  }
                  if (couldMatchMore) possibleFutureMatches += tokenType
                }
              case `NeedsMore` => possibleFutureMatches.addOne(tokenType)
              case res => {
                if (res.isInstanceOf[PartialMatch]) log(s"Partial match $res for tokentype=$tokenType")
                possibleFutureMatches -= tokenType
                if (lastMatch == res) lastMatch == null
              }
            }
      }
      
      log(s"Possible future matches = $possibleFutureMatches\n")
      if (possibleFutureMatches.isEmpty) {
        lastMatch match {
          case null =>
            throw new Exception(
                s"""Bad character(s) "$acc" at start offset $origOffset, $lastPos"""
            )
          case last: (Match, TokenType) => {
            log("Matched! Found=\"" + last._2 + "\"")
            val matched = last._1
            acc.delete(0, matched.end - matched.start)
            log(s"Returning, acc = `$acc`")
            return Some((last._1, last._2, TextRange(startPos, currentPos), acc))
          }
        }
      } else {
        val next = getNext match {
          case Some(c) => c
          case None => lastMatch match {
            case null =>
              throw new Exception("Unexpected end of file!")
            case last: (Match, TokenType) =>
              log("Matched in else branch! Found=\"" + last._2 + "\"")
              val matched = last._1
              acc.delete(0, matched.end - matched.start)
              log(s"Returning from else branch, acc = `$acc`")
              return Some((last._1, last._2, TextRange(startPos, currentPos), acc))
          }
        }
        acc.append(next)
        log(s"Acc with next=$acc")
        val (row, col) = if (lastChar == '\r') {
          if (next != '\n') (currentPos.row + 1, 0) 
          else (currentPos.row, currentPos.col)
        } else if (lastChar == '\n') {
          (currentPos.row + 1, 0)
        } else (currentPos.row, currentPos.col + 1)
        currentPos = Position(row, col, currentPos.offset + 1)
        currentLength += 1
        lastChar = next
      }
      possibleFutureMatches.nonEmpty
    }) {}
    None
  }

  private def log(msg: Any) = LOG.append(msg.toString).append('\n')
}

object Lexer {
  def tokenize(file: File): Iterable[Token[?]] = {
    val lexer = Lexer(new BufferedInputStream(new FileInputStream(file)))
    try {
      return lexer.tokenize()
    } catch {
      case e: Exception => {
        println("asdhfaksjdfhlaksjdhflakjsdhfkjashldfjkashdflkjashdf")
        lexer.end()
        throw e
      }
    } finally {
      println("asdkfja;sldjf;askljdf")
      lexer.end()
    }
  }
}