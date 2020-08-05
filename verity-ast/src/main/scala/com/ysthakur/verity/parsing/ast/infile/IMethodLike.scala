package com.ysthakur.verity.parsing.ast.infile

import com.ysthakur.verity.parsing.ast.infile.expr.Expr

trait IMethodLike extends HasModifiers {
  def returnType: TypeRef
  def name: String
  def ctparams: CTParamList
  def params: ParamList
  
  def body: Option[Block] //Option[Block|Expr]
  def body_=(newBody: Option[Block] /*Option[Block|Expr]*/): Unit
}