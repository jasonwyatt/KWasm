/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kwasm.script.command.assertion

import kwasm.ast.AstNode
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Token
import kwasm.script.command.Action
import kwasm.script.command.Command
import kwasm.script.command.parseAction
import kwasm.script.execution.ScriptContext

/**
 * (assert_trap <action> <string>)
 */
class AssertActionTrap(
    val action: Action,
    val messageContains: String
) : Command<Unit> {
    override fun execute(context: ScriptContext) {
        var okay = false
        try {
            action.execute(context)
        } catch (e: Throwable) {
            if (messageContains !in e.message ?: "") {
                throw AssertionError(
                    "Expected exception with message containing: \"$messageContains\"",
                    e
                )
            }
            okay = true
        }
        if (!okay) {
            throw AssertionError(
                "Expected exception with message containing: " +
                    "\"$messageContains\", none thrown."
            )
        }
    }
}

fun List<Token>.parseAssertActionTrap(fromIndex: Int): ParseResult<AssertActionTrap>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "assert_trap")) return null
    currentIndex++

    val action = parseAction(currentIndex)
        ?: throw ParseException("Expected action", contextAt(currentIndex))
    currentIndex += action.parseLength

    val message = parseLiteral(currentIndex, String::class)
    currentIndex += message.parseLength

    if (!isClosedParen(currentIndex)) {
        throw ParseException("Expected close paren", contextAt(currentIndex))
    }
    currentIndex++

    return ParseResult(
        AssertActionTrap(action.astNode, message.astNode.value),
        currentIndex - fromIndex
    )
}
