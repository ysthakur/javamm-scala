package verity.ast.infile

import verity.parsing.TextRange
import verity.ast.infile.Expr

import scala.collection.mutable.ListBuffer

class Field(
    override val name: String,
    override val modifiers: ListBuffer[Modifier],
    var myType: Type,
    var initExpr: Option[Expr] = None,
    override val textRange: TextRange
) extends VariableDecl
    with HasModifiers
    with HasType {
  override def text: String = ???
}
