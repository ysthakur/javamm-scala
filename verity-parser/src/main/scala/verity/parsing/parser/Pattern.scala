package verity.parsing.parser

import verity.{CompilationError, Lazy}
import verity.parsing.{Token, TokenType, TextRange}
import verity.parsing.ast._, infile._, expr._

/**
  * A pattern, like regex, that matches reader
  *
  * @tparam reader The type of the reader (Iterable of Char or Token)
  */
trait Pattern { self =>
  
  type Out

  val expected: List[String] = Nil

  def apply(reader: Reader, backtrack: Boolean): ParseResult[this.Out]

  /**
    * Just to compose multiple patterns. Match this pattern first, then
    * pattern `other`. It's left associative
    *
    * @param other
    * @return
    */
  def -[T <: Pattern](other: => T): ConsPattern[this.type, T] =
    new ConsPattern[this.type, T](this, other, false)

  /**
  * Just to compose multiple patterns. Match this pattern first, then
  * pattern `other`. If first pattern matches, will not backtrack.
  *
  * @param other
  * @return
  */
  def -![T <: Pattern](other: => T): ConsPattern[this.type, T] =
    new ConsPattern[this.type, T](this, other, false)

  def -(s: String): Pattern.Aux[Out] =
    (reader, backtrack) => tryMatch(reader, backtrack) match {
      case m: Matched[?] => 
        reader.nextExactText(s, TokenType.MISC, !backtrack) match {
          case None => Failed(Token.empty(m.range.start), List(s), m.range.start, backtrack)
          case _ => m
        }
      case f => f
    }

  /*inline*/ def tryMatch(reader: Reader, backtrack: Boolean): ParseResult[this.Out] =
    try {
      Pattern.indent += 2
      val res = apply(reader, backtrack)
      Pattern.indent -= 2
      res
    } catch {
      case e if !e.isInstanceOf[CompilationError] => 
        throw new CompilationError(s"Exception on reader ${reader.nextOrEmpty}", e)
    }

  //def ==(other: Pattern): Boolean = ???
//  def nextOrEmpty(reader: Reader): Token = Token.empty(0)
//    if (reader.hasNext) 
//      Token(TextRange(reader.offset, reader.offset + 1), "" + reader.chars(reader.charInd)) 
//    else Token.empty(0)

  def |[T <: Pattern](other: T): Pattern.Aux[this.Out | other.Out] =
    OrPattern(this, other).asInstanceOf[Pattern.Aux[this.Out | other.Out]]

  def * : Pattern.Aux[List[Out]] = repeat()
   def *\ : Pattern.Aux[List[Out]] = new RepeatPatternRev[this.type](this)
  //def *? : Pattern = RepeatPattern(this/*, isEager = false*/)
  def ? : Pattern.Aux[Out | Null] = MaybePattern[this.type](this)

  def repeat(min: Int=0, max: Int=Int.MaxValue): Pattern.Aux[List[Out]] = 
    RepeatPattern[this.type](this, min, max)
  
  def |>[N](ctor: Out => N): Pattern.Aux[N] = (reader, backtrack) => self.tryMatch(reader, backtrack).map(ctor)

//  def println(s: Any): Unit = {} //System.out.println(""+Pattern.indent+"  ".repeat(Pattern.indent) + s)
}

object Pattern {
  type Aux[N] = Pattern { type Out = N }

//  val allPatterns: mutable.LinkedHashMap[String, Pattern] = mutable.LinkedHashMap()
  var indent: Int = 0

  def fromOption(optPattern: (Reader, Boolean) => Option[Token], expected: List[String] = Nil): Pattern.Aux[Token] =
    (reader, backtrack) => {
        val start = reader.offset
        optPattern(reader, backtrack) match {
        case Some(token) => Matched(() => token, reader, token.textRange)
        case None => Failed(reader.nextOrEmpty, expected, start, backtrack)
      }
    }
    // reader.nextToken().getOrElse(Token(TextRange.empty(reader.offset), "", TokenType.MISC))

//  def nextOrEmpty(reader: Reader): Token = Token.empty(-1)
//    reader.peekChar.fold(Token.empty(-1))(c => Token(TextRange.empty(-1), "" + c))
}

trait INamedPattern extends Pattern {
  val name: String
  //override def canEqual(that: Any): Boolean = that.isInstanceOf[PatternRef]
  override def equals(obj: Any): Boolean = obj match {
    case named: INamedPattern => this.name == named.name
    case _ => false
  }
  //override def ==(other: Pattern): Boolean = this.equals(other)
  override def hashCode: Int = name.hashCode
}

def textRangeToEnd(reader: Reader): TextRange = TextRange(reader.offset, reader.length)

object - {
  def unapply[A, B](arg: ConsNode[A, B]): Option[(A, B)] = {
    Some(arg.n1, arg.n2)
  }
}

/**
 * Like RepeatPattern, but **immediately** evaluates and tries to fold left.
 */
class FoldLeft[A, B >: A](p1: Pattern.Aux[A], p2: Pattern, cut: Boolean, min: Int = 0, max: Int = Int.MaxValue)(combine: (B, p2.Out) => B) extends Pattern {
  type Out = B
  override def apply(reader: Reader, backtrack: Boolean): ParseResult[Out] = {
    val start = reader.offset
    p1.tryMatch(reader, backtrack) match {
      case Matched(create, _, range1) =>
        var acc: B = create()
        var numMatches = -1
        var end = range1.end
        while (numMatches < max) {
          p2.tryMatch(reader, backtrack) match {
            case Matched(create2, _, range2) =>
              acc = combine(acc, create2())
              end = range2.end
            case f: Failed =>
              if (numMatches < min) return f
              numMatches = max
          }
          numMatches += 1
        }
        if (numMatches < min) Failed(Token.empty(range1.start), List(), range1.start, !cut || backtrack)
        else {
          val finalAcc = acc
          Matched(() => finalAcc, reader, TextRange(start, end))
        }
      case f => f
    }
  }
}

case class ConsPattern[+T1 <: Pattern, +T2 <: Pattern](p1: T1, p2: T2, cut: Boolean, name: String = "", skipWS: Boolean = true) extends Pattern {
  type Out = ConsNode[p1.Out, p2.Out]
//  override def isFixed: Boolean = p1.isFixed && p2.isFixed
//  override def isEager: Boolean = p1.isEager && p2.isEager
  
  override def apply(reader: Reader, backtrack: Boolean): ParseResult[Out] = {
    // println("--------------------")
    // println(s"incons name=$name, reader=${reader.map(_.text)}")
    if (!name.isEmpty) System.out.println("----------------------------\n")

    p1.tryMatch(reader, backtrack) match {
      case Matched(create, rest, range) =>
        println("~~~~~~~~~~~~~~~")
        //println(s"Matched pattern 1, now matching $rest")
        if (skipWS) {
          println(s"Started skipping comments, reader=$reader")
          rest.skipCommentsAndWS()
          println(s"Finished skipping comments, reader=$reader")
        }
        p2.tryMatch(rest, backtrack) match {
        case Matched(create2, rest2, range2) =>
          //println(s"\nMatched conspattern!!!, \n\t reader=$reader \n rest2=$rest")
          // println(s"Matched, name=$name, rest2=${rest2.map(_.text)}")
          // if (!name.isEmpty) System.out.println(s"$name matched! reader=$reader")
          Matched(() => ConsNode(create(), create2()), rest2, TextRange(range.start, range2.end))
        case failed: Failed =>
          println(s"Didn't match second pattern, name=$name, failed=$failed, reader=${reader}")
          // if (!name.isEmpty) System.out.println(s"$name failed1=$failed, rest=$reader")
          failed.copy(canBacktrack=cut)
      }
      case failed: Failed =>
        println(s"Didn't match first cons, name=$name, failed=$failed, reader=${reader}")
        // if (!name.isEmpty) System.out.println(s"$name failed2=$failed, reader=$reader")
        failed
    }
  }
}

case class OrPattern(p1: Pattern, p2: Pattern, shouldFlatten: Boolean = true) extends Pattern {
  type Out = p1.Out | p2.Out

//  override def isFixed: Boolean = p1.isFixed && p2.isFixed
//  override def isEager: Boolean = p1.isEager || p2.isEager
  // lazy val p2 = _p2

  override val expected = p1.expected ::: p2.expected

  override def apply(reader: Reader, backtrack: Boolean): ParseResult[Out] =
    p1.tryMatch(reader, true) match {
      case f: Failed => 
        if (f.canBacktrack) p2.tryMatch(reader, backtrack)
        else f
      case m => m
    }
}

case class MaybePattern[P <: Pattern](pattern: P) extends Pattern {
  type Out = pattern.Out | Null
  override def apply(reader: Reader, backtrack: Boolean): ParseResult[Out] =
    val start = reader.offset
    pattern
      .tryMatch(reader, backtrack)
      .or(Matched(() => null, reader, TextRange.empty(start)))
}
/**
  *
  */
case class RepeatPattern[P <: Pattern](
    pattern: P,
    min: Int = 0,
    max: Int = Int.MaxValue,
    name: String = ""
//    override val isEager: Boolean
) extends Pattern {

  type Out = List[pattern.Out]

  override final def apply(reader: Reader, backtrack: Boolean) = {
    // println("--------------------")
    // println(s"inrepeat name=$name, reader=${reader.map{_.text}}")
    if (reader.isEmpty) println("\n\n\nreader is empty!!!!!!!!")

    @annotation.tailrec
    def recMatch(reader: Reader, start: Int, end: Int, count: Int, prev: List[() => pattern.Out]): ParseResult[Out] = {
      // println(s"reader=${reader.map(_.text)}")
      println(s"In recMatch, reader=$reader")
      if (reader.isEmpty) return makeNodeList(prev, start, end, reader)
      pattern.tryMatch(reader, backtrack) match {
        case m@Matched(create, rest, range) =>
          // println(s"\nprev=$prev\n")
          if (count < max) recMatch(rest, start, range.end, count + 1, create :: prev)
          else makeNodeList(create :: prev, start, range.end, rest)
        case f@Failed(got, expected, pos,  backtrack) =>
          println(s"Repeatpattern failed, got=$got, exp=$expected, reader=$reader")
          if (count < min) f
          else makeNodeList(prev, start, end, reader)
      }
    }

    recMatch(reader, reader.offset, reader.offset, 0, List.empty)
  }

  private def makeNodeList(results: List[() => pattern.Out], start: Int, end: Int, rest: Reader): Matched[Out] = {
    println(s"name=$name, results are ${results.map(_())}")
    val rev = results.reverse
    Matched(() => rev.map(_()), rest, TextRange(start, end))
  }
}

case class RepeatPatternRev[P <: Pattern](
  pattern: P,
  min: Int = 0,
  max: Int = Int.MaxValue,
  name: String = "",
  skipWS: Boolean = true
  //    override val isEager: Boolean
) extends Pattern {

  type Out = List[pattern.Out]

  override val expected = pattern.expected

  override final def apply(reader: Reader, backtrack: Boolean) = {
    // println("--------------------")
    // println(s"inrepeat name=$name, reader=${reader.map{_.text}}")
    if (reader.isEmpty) println("\n\n\nreader is empty!!!!!!!!")

    @annotation.tailrec
    def recMatch(reader: Reader, start: Int, end: Int, count: Int, prev: List[() => pattern.Out]): ParseResult[Out] = {
      // println(s"reader=${reader.map(_.text)}")
      if (reader.isEmpty) return makeNodeList(prev, start, end, reader)
      pattern.tryMatch(reader, backtrack) match {
        case m@Matched(create, rest, range) =>
          // println(s"\nprev=$prev\n")
          if (skipWS) rest.skipCommentsAndWS()
          if (count < max) recMatch(rest, start, range.end, count + 1, create :: prev)
          else makeNodeList(create :: prev, start, range.end, rest)
        case f@Failed(got, expected, pos,  backtrack) =>
          if (count < min) f
          else makeNodeList(prev, start, end, reader)
      }
    }

    recMatch(reader, reader.offset, reader.offset, 0, List.empty)
  }

  private def makeNodeList(results: List[() => pattern.Out], start: Int, end: Int, rest: Reader): Matched[Out] = {
    println(s"name=$name, results are ${results.map(_())}")
    Matched(() => results.map(_()), rest, TextRange(start, end))
  }
}

class ByNameP[R](getPattern: () => Pattern.Aux[R]) extends Pattern {
  type Out = R
  override def apply(reader: Reader, backtrack: Boolean) = getPattern()(reader, backtrack)
}

case class FunctionPattern[N](matchFun: Reader => ParseResult[N]) extends Pattern {
  type Out = N
  val id = new scala.util.Random().nextInt().toHexString               
  override def apply(reader: Reader, backtrack: Boolean) = matchFun(reader)
  override def toString: String = s"FunctionPattern$id"
}