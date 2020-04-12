package com.ysthakur.parsing.parser

import com.ysthakur.parsing.ast._
import com.ysthakur.parsing._

case class PatternAndConstructor[M <: Match[?], N <: Node](
  pattern: Pattern,
  val ctor: M => N
) extends Pattern {
  override type AsNode = N
  override type Input = pattern.Input

  override val isFixed: Boolean = pattern.isFixed
  override val isEager: Boolean = pattern.isEager
  override def tryMatch(input: List[Node], offset: Int, trace: Trace): ParseResult =
    pattern.tryMatch(input, offset, trace)
  override def create(matched: MatchIn): this.AsNode = ctor(matched.asInstanceOf)
  // override def copy: PatternAndConstructor[M, N] = PatternAndConstructor(pattern, ctor)
  // override def tryCreate(input: Iterable[Node], offset: Int): (ParseResult, scala.Option[this.AsNode]) = ???
}

// case class ApplicablePattern[P <: Pattern](pattern: P) {
//   def apply[M <: Match[?], N <: Node](ctor: M => N): PatternAndConstructor[M, N] = 
//     PatternAndConstructor[M, N](pattern, ctor)
//   // def >>[M <: PatternMatch[pattern.I], N](
//   //   ctor: M => N
//   // ): PatternAndConstructor[M, N] = PatternAndConstructor(pattern, ctor)
// }

implicit def toApplicablePattern[P <: Pattern, M <: Match[?], N <: Node](
    pattern: P
  ): (M => N) => PatternAndConstructor[M, N] = PatternAndConstructor[M, N](pattern, _)