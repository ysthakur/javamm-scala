package com.ysthakur.parsing.parser

import com.ysthakur.parsing._
import com.ysthakur.parsing.ast.Node
import com.ysthakur.parsing.lexer._

case class TokenTypePattern(val tokenType: TokenType) extends Pattern {
  override type AsNode = Token[TokenType]
  //override type Input <: Token[TokenType]
  type Tok = Token[tokenType.type]

  override val isEager: Boolean = false
  override val isFixed: Boolean = true
  override def tryMatch(input: List[Node], offset: Int, trace: Trace): ParseResult = {
    input.head match {
      case token: Token[?] => if (token.tokenType == tokenType) 
          return Matched(token, input.tail, token.endOffset)
    }
    Failed
  }
  override def create(matched: MatchIn): this.AsNode = {
    ???
  }
  // override def tryCreate(input: Iterable[Node], offset: Int): (ParseResult, scala.Option[this.AsNode]) = ???
}

implicit def toTokenTypePattern(tokenType: TokenType): TokenTypePattern =
TokenTypePattern(tokenType)