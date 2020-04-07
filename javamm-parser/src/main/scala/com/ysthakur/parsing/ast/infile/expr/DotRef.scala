package com.ysthakur.parsing.ast.infile.expr

import com.ysthakur.parsing.grammar._
import com.ysthakur.parsing.parser._
import com.ysthakur.parsing.ast.Types._

case class DotRef(iterable: Iterable[VarRef]) extends Expr {
    val startOffset = iterable.head.startOffset
    val endOffset = iterable.last.endOffset
    override def text: String = ???
    //def this(pattern: PatternWithMatch[RepeatPattern[Node], Match[Node]]) = this(null)

}