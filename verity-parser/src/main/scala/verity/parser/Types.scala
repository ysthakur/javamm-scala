package verity.parser

import verity.ast._
import verity.ast.infile._
import verity.ast.infile.unresolved._
import verity.parser.Core.{argList, identifierText, identifierWithTextRange}

import fastparse.JavaWhitespace._
import fastparse._

object Types {

  //todo clear this up?
  def upperBound[_: P]: P[UnresolvedTypeRef] = P("extends" ~/ typeRef)
  def lowerBound[_: P]: P[UnresolvedTypeRef] = P("super" ~/ typeRef)
  // def typeBound[_: P] = P(Index ~ StringIn("super", "extends").! ~/ Index ~ typeRef)

  def typeRef[_: P]: P[UnresolvedTypeRef] =
    P(identifierText ~ ("." ~ identifierText).rep ~ typeArgList).map {
      case (first, restPath, args) =>
        UnresolvedTypeRef(
          first +: restPath,
          args,
          None
        )
    }
  def wildCard[_: P]: P[UnresolvedWildcard] =
    P("?" ~ ("extends" ~ typeRef).? ~ ("super" ~ typeRef).?).map { case (upper, lower) =>
      UnresolvedWildcard(upper, lower)
    }
  def primitiveType[_: P]: P[PrimitiveType] =
    P(StringIn("boolean", "byte", "char", "short", "int", "float", "long", "double").! ~ Index)
      .map { case (typ, end) =>
        PrimitiveType(PrimitiveTypeDef.fromName(typ).get, TextRange(end - typ.length, end))
      }
  /**
   * A type that isn't just a wildcard, possibly an array type
   */
  def nonWildcardType[_: P]: P[Type] = P((primitiveType | typeRef) ~ ("[" ~ Index ~ "]" ~ Index).rep).map {
    case (innerType, brackets) =>
      brackets.foldLeft(innerType: Type){ case (typ, (start, end)) => ArrayType(typ, TextRange(start, end)) }
  }
  def typeArg[_: P]: P[Type] = P(wildCard | nonWildcardType)
  def typeArgList[_: P]: P[TypeArgList] = P(("<" ~/ Index ~ argList(typeArg: P[Type]) ~ ">" ~ Index).?).map {
    case Some((start, args, end)) => TypeArgList(args, TextRange(start, end))
    case None => TypeArgList(Nil, TextRange.synthetic)
  }

  def returnType[_: P]: P[Type] =
    P(("void" ~ !CharPred(!_.isUnicodeIdentifierPart) ~/ Index) | nonWildcardType) .map { t =>
      (t: @unchecked) match {
        case end: Int => new VoidTypeRef(TextRange(end - 4, end))
        case typ: Type => typ
      }
    }

  def typeParam[_: P]: P[TypeParam] = P(identifierWithTextRange ~ upperBound.? ~ lowerBound.?).map {
    case (name, nameRange, upper, lower) =>
      new TypeParam(
        name,
        upper.getOrElse(BuiltinTypes.objectTypeDef.makeRef),
        lower.getOrElse(NothingTypeDef.makeRef),
        nameRange
      )
  }
  def typeParamList[_: P]: P[TypeParamList] =
    P("<" ~/ Index ~ typeParam ~ ("," ~ typeParam).rep ~ ">" ~ Index).map {
      case (start, first, rest, end) => new TypeParamList(first +: rest, TextRange(start, end))
    }

}
