package com.ysthakur.verity.parsing.parser

import com.ysthakur.verity.parsing.lexer._
import com.ysthakur.verity.parsing.{Position, TextRange}
import com.ysthakur.verity.parsing.ast._
import com.ysthakur.verity.parsing.lexer.SymbolTokenType._
import com.ysthakur.verity.parsing.lexer.RegexTokenType._
import com.ysthakur.verity.parsing.lexer.KeywordTokenType._
import com.ysthakur.verity.parsing.lexer.ReservedWord._
import com.ysthakur.verity.parsing.lexer.TokenType._
import com.ysthakur.verity.parsing.parser.Pattern._

import scala.language.postfixOps

//noinspection ScalaUnnecessaryParentheses
private object ParserPatterns {

  type TTP = TokenTypePattern

  val opCtor = (op: Node) => Op(op.text)

  val EOL: Pattern = SEMICOLON
  val numLiteral = NUM_LITERAL |>> {
    case token: Token[?] => NumLiteral(token.text)
  }
  val booleanLiteral = TRUE | FALSE |>> {
    case Token(TRUE) => TrueLiteral
    case Token(FALSE) => FalseLiteral
  }
  val literal = numLiteral | booleanLiteral | 
      (THIS |>> { case token: Tok => ThisRef(token.range)}) 
    | (SUPER |>> { case token: Tok => SuperRef(token.range)})
  val modifier: Pattern = (input: List[Tok], pos: Position, trace: Trace) => {
    if (input.isEmpty) Matched.empty(input, TextRange.empty(pos))
    else input.head match {
      case token@Token(mod: ModifierTokenType) => Matched(() => mod.toModifier, input.tail, token.range)
      case f => Failed(f, List("modifier"), pos)
    }
  }
  val unaryPreOp = PLUSX2 | MINUSX2 | MINUS | EXCL_MARK | TILDE |>> opCtor
  val unaryPostOp = PLUSX2 | MINUSX2 |>> opCtor
  val identifier = FunctionPattern((input: List[Tok], pos: Position) =>
    if (input.nonEmpty) input.head match {
      case token@Token(tt: ValidIdentifierTokenType) =>
        Matched(() => ValidIdNode(token.text), input.tail, token.range)
      case other => Failed(other, List("Identifier"), pos)
    }
    else Failed(EmptyToken, List("Identifier"), pos)
  )
  val unreservedId = FunctionPattern((input: List[Tok], pos: Position) => {
    if (input.nonEmpty) input.head match {
      case token @ Token(tt: ValidIdentifierTokenType) =>
        if (!token.isInstanceOf[ReservedWord])
          Matched(
              () =>
                ValidIdNode(
                    token.text
                ),
              input.tail,
              token.range
          )
        else Failed(token, List("Non-reserved identifier"), token.range.start)
      case other => Failed(other, List("Valid identifier"), pos)
    }
    Failed(EmptyToken, List("Valid identifier"), pos)
  })
  val dotReference = identifier - ((DOT - identifier) *) |>> {
    case (validId: ValidIdNode) => DotRef(List(validId))
    case (validId: ValidIdNode) - (nodeList: NodeList[?]) =>
      DotRef(
        validId :: (nodeList.asInstanceOf[NodeList[?]].nodes)
            .map(_.asInstanceOf[ConsNode[?, ValidIdNode]].n2.asInstanceOf[ValidIdNode])
            .toList)
  }
  val pkgStmt = PACKAGE - dotReference - EOL |>> {
    case pkg - (dotRef: DotRef) - eol =>
      //println(s"Is package $pkg - $dotRef - $eol")
      PackageStmt(dotRef)
  }
  val importStatement =
    IMPORT - dotReference - ((DOT - STAR) ?) - SEMICOLON |>> {
      case impt - (ref: DotRef) - star - semicolon => {
        //println(s"Is import $impt - $ref - $star")
        Import(ref, star != null)
      }
    }
  "firstLevelExpr" := identifier | literal

  "unaryExpr" := ("expr" - unaryPostOp) | (unaryPreOp - "expr") |>> {
    case (op: Op) - (expr: Expr) => UnaryPreExpr(op, expr)
    case (expr: Expr) - (op: Op) => UnaryPostExpr(expr, op)
  }
  
  PatternClass.make(
    name = "specialExpr",
    "firstLevelExpr",
    "binaryExpr"
  )

  // "binaryExpr" := FunctionPattern((input, pos, trace) => {
  //   val statement = input.findLast()
  // }) |>> {
  //   case (lexpr: Expr) - op - (rexpr: Expr) =>
  //     //println(s"GJGKJGJDFSG!!! - [$rexpr]");
  //     BinaryExpr(lexpr, opCtor(op), rexpr)
  //   case a - b - (c - d) => println(s"$c \n\t\t$d"); throw new Error("riyto8tq64")
  //   case x => println(s"QPPETQeRWJ#5 ${x.getClass()} $x"); x
  // } Extends PatternRef("expr")

  "binaryExpr" := LeftAssocPattern(
    p1 = "expr",
    p2 = ((STAR | FWDSLASH | MODULO)
      | (PLUS | MINUS)
      | (LTX2 | GTX2 | GTX3)
      | (LT | LTEQ | GT | GTEQ)
      | (EQX2 | NOTEQ)
      | AND
      | CARET
      | OR
      | ANDX2
      | ORX2) - "expr"
  ) |>> {
    case (lexpr: Expr) - op - (rexpr: Expr) =>
      println(s"GJGKJGJDFSG!!! - [$rexpr]");
      BinaryExpr(lexpr, opCtor(op), rexpr)
    //case a - b - (c - d) => println(s"$c \n\t\t$d"); throw new Error("riyto8tq64")
    case x => println(s"\n\nQPPETQeRWJ#5 ${x.getClass()} $x..."); unwrapBinaryExpr(x)
  } Extends PatternRef("expr")


  def unwrapBinaryExpr(node: Node): BinaryExpr = {
    node match {
      case (lexpr: Expr) - ((op: Tok) - (rexpr: Expr)) => 
        println("poiuytr")
        BinaryExpr(lexpr, opCtor(op), rexpr)
      case ConsNode(p1: ConsNode[_, _], ConsNode(op: Tok, rexpr: Expr)) => 
        println("\nqwedfghhjk;")
        BinaryExpr(unwrapBinaryExpr(p1), opCtor(op), rexpr)
      case x => println(s"\nQPPETQeRWJ#5 ${x.getClass()} $x..."); throw new Error("alksdjfasdf")
    }
  }

/*ConsNode(
  ConsNode(
    ValidIdNode(gah),
    ConsNode(
      InvariantToken(PLUS,TextRange(Position(7,4,118),Position(7,5,119))),
      NumLiteral(78))
  ),
  ConsNode(
    InvariantToken(PLUS,TextRange(Position(7,9,123),Position(7,10,124))),
    NumLiteral(0)
  )
)*/

  // "binaryExpr" :=
  //   "expr" - (
  //     (STAR | FWDSLASH | MODULO)
  //     | (PLUS | MINUS)
  //     | (LTX2 | GTX2 | GTX3)
  //     | (LT | LTEQ | GT | GTEQ)
  //     | (EQX2 | NOTEQ)
  //     | AND
  //     | CARET
  //     | OR
  //     | ANDX2
  //     | ORX2) - "expr" |>> {
  //   case (lexpr: Expr) - op - (rexpr: Expr) =>
  //     println(s"GJGKJGJDFSG!!! - [$rexpr]");
  //     BinaryExpr(lexpr, opCtor(op), rexpr)
  //   case a - b - (c - d) => println(s"$c \n\t\t$d"); throw new Error("riyto8tq64")
  //   case x => println(s"QPPETQeRWJ#5 ${x.getClass()} $x"); x
  // } Extends PatternRef("expr")

  "dotSelect" := "expr" - DOT - identifier |>> {
    case expr - dot - validId => DotChainedExpr(expr.asInstanceOf, validId.asInstanceOf)
  }
  "parenExpr" := LPAREN - "expr" - RPAREN |>> {
    case lparen - (expr: Node) - rparen => expr
  }
  "arrayAccess" := "expr" - LSQUARE - "expr" - RSQUARE |>> {
    case (arr: Expr) - l - (index: Expr) - r => ArraySelect(arr, index)
  }
  // lazy val leftAssocExpr := PatternClass.make(
  //   name = "leftAssocExpr",
  //   expressions.reversed: _*
  // )

  val expressions = List[Pattern](
      "firstLevelExpr",
      "binaryExpr"//,
      /*"dotSelect",
      "arrayAccess",
      "unaryExpr",*/
      //"firstLevelExpr",
      /*"parenExpr"*/)
  PatternClass.make(
      name = "expr",
      expressions: _*
  )
  /*PatternClass.make(
    name = "shortExpr",
    expressions.reverse: _*
  )*/

  "typeRef" := ""

  val assignment =
    "expr" - ((PLUS | MINUS | STAR | FWDSLASH | MODULO) ?) - EQ - "expr" - EOL |>> {
      case variable - op - eq - expr - eol => ???
    }

  val varDeclFirstPart = "typeRef" - unreservedId |>> {
    case (typeRef: com.ysthakur.verity.parsing.ast.infile.TypeRef) - (validId: ValidIdNode) =>
      ConsNode(typeRef, validId)
  }
  val localVarDecl = varDeclFirstPart - EOL
  "typeDecl" := "abcd"

  val root: Pattern = (pkgStmt?) - (importStatement*) - ("binaryExpr"?) |>> {
    case pkg - imports - typeDefs => typeDefs
  }

  def [N <: Node](p: Pattern) |>> (ctor: Node => N): PatternAndConstructor[N] =
    PatternAndConstructor(p, ctor)
}