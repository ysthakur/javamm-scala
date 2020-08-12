package com.ysthakur.verity.parsing.ast.infile.expr

import com.ysthakur.verity.parsing.TextRange

case class BinaryExpr(left: Expr, op: Op, right: Expr) extends Expr {
  override def text: String = s"(${left.text} ${op.text} ${right.text})"
//  val startOffset = left.startOffset
//  val endOffset = right.endOffset
  //def unapply(): (CharSequence, Int, Int) = ???
  override lazy val textRange = left.textRange to right.textRange
}