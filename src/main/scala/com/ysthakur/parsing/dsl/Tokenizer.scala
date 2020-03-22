package com.ysthakur.parsing.dsl

import com.ysthakur.parsing.{FullMatch, NeedsMore, NoMatch, PartialMatch}

import scala.collection.immutable.Map
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

/**
 *
 * @param stateNames The names of the states this tokenizer/parser will have
 * @tparam Input       The type of the input (character, token)
 * @tparam Accumulator The data structure all the pieces of the input are stored in
 *                     (a StringBuilder or ASTNode)
 */
abstract class Tokenizer[Input, Output, Accumulator <: Iterable[Input]]
(protected val current: Accumulator, stateNames: String*) extends Dynamic {

    val defaultState: State = State("DEFAULT")
    var currentState: State = defaultState
    private val stateCases: ListBuffer[StateCase[Input]] = new ListBuffer[StateCase[Input]]()
    private var last: Input = _

    implicit val states: Map[String, State] = makeStates(stateNames)
    implicit val thisTokenizer: Tokenizer[Input, Output, Accumulator] = this


    /**
     * Get the next character (if this is a lexer) or token (if this is a parser)
     *
     * @return
     */
    abstract def getNext: Input

    /**
     * Add this piece of the input (character/token) to the
     * accumulator, which is a StringBuilder or something
     */
    abstract def accumulate(input: Input): Unit

    def proceed(): Unit = {
        last = getNext
        accumulate(last)
        val stateCase = stateCases.find(sc => sc.state == currentState).getOrElse(
            stateCases.find(sc => sc.state == defaultState)
                .getOrElse(throw new Error("No state matches this!"))
        )
        var possibleFutureMatches = ListBuffer[Pattern[Input]]()
        do {
            for (pc <- stateCase.patternCases) {
                val matchRes = pc.pattern.tryMatch(current)
                matchRes match {
                    case NoMatch() => possibleFutureMatches -= pc.pattern
                    case FullMatch() =>
                        pc.action()
                        possibleFutureMatches = ListBuffer.empty
                    case NeedsMore() => possibleFutureMatches :+= pc.pattern
                    //TODO handle this better
                    case PartialMatch(_) =>
                        throw new Error("This should not happen! " +
                            "There should have been a full match before!")
                }
            }
        } while (possibleFutureMatches.nonEmpty)
    }

    /**
     * Return the next character/token without consuming it
     *
     * @return
     */
    abstract def peekNext: Input

    /**
     * Return the last character/token that it processed
     *
     * @return
     */
    def peekLast: Input = last

    private def makeStates(stateNames: Seq[String]): Map[String, State] =
        stateNames.map(name => (name, State(name))).appended((defaultState.name, defaultState)).toMap

    private[dsl] def addStateCase(stateCase: StateCase[Input]): Unit = stateCases.addOne(stateCase)

    /**
     * Get a state by its name. Not for anything other than getting states
     * by their name
     *
     * @param stateName the name of the state
     * @return
     */
    protected def selectDynamic(stateName: String): State =
        states.getOrElse(stateName, throw new NullPointerException())
}
