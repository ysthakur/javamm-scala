package com.ysthakur.parsing

package object dsl {

    implicit def toState[T](stateName: String)(implicit states: Map[String, State[T]]): State[T] =
        states.getOrElse(stateName, throw new NullPointerException())

    implicit def toRegex(regex: String): RegexPattern = RegexPattern(regex)

}
