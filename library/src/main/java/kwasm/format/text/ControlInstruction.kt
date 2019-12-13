/*
 * Copyright 2019 Google LLC
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

package kwasm.format.text

import kwasm.ast.ControlInstruction
import kwasm.ast.ControlInstruction.Break
import kwasm.ast.ControlInstruction.BreakIf
import kwasm.ast.ControlInstruction.BreakTable
import kwasm.ast.ControlInstruction.Call
import kwasm.ast.ControlInstruction.CallIndirect
import kwasm.ast.ControlInstruction.NoOp
import kwasm.ast.ControlInstruction.Return
import kwasm.ast.ControlInstruction.Unreachable
import kwasm.ast.Identifier
import kwasm.format.text.token.Token

/** This file contains parsing implementations for all of the various [ControlInstruction]s. */

/**
 * Attempts to parse a [ControlInstruction] from the receiving [Token] [List]. Returns `null` if
 * none was found.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/text/instructions.html#control-instructions):
 *
 * ```
 *   blockinstr(I)  ::=
 *      ‘block’ I′:label(I) rt:resulttype (in:instr(I′))* ‘end’ id?
 *          => block rt in* end (if id? = ϵ ∨ id? = label)
 *      ‘loop’ I′:label(I) rt:resulttype (in:instr(I′))* ‘end’ id?
 *          => loop rt in* end (if id? = ϵ ∨ id? = label)
 *      ‘if’ I′:label(I) rt:resulttype (in^1:instr(I′))∗ (‘else’ id?^1 (in^2:instr(I'))*)? ‘end’ id?^2
 *          => if rt in*^1 else in*2 end (if id?^1 = ϵ ∨ id?^1 = label, id?^2 = ϵ ∨ id?^2 = label)
 *   plaininstr(I)  ::= ‘unreachable’                                   => unreachable
 *                      ‘nop’                                           => nop
 *                      ‘br’ l:labelidx(I)                              => br l
 *                      ‘br_if’ l:labelidx(I)                           => br_if l
 *                      ‘br_table' l*:vec(labelidx(I)) l^N:labelidx(I)  => br_table l∗ l^N
 *                      ‘return’                                        => return
 *                      ‘call’ x:funcidx(I)                             => call x
 *                      ‘call_indirect’ x,I′:typeuse(I)                 => call_indirect x (if I′={})
 * ```
 */
fun List<Token>.parseControlInstruction(fromIndex: Int): ParseResult<out ControlInstruction>? =
    parseUnreachableContInstruction(fromIndex)
        ?: parseNoOpContInstruction(fromIndex)
        ?: parseReturnContInstruction(fromIndex)
        ?: parseBreakContInstruction(fromIndex)
        ?: parseBreakIfContInstruction(fromIndex)
        ?: parseBreakTableContInstruction(fromIndex)
        ?: parseCallContInstruction(fromIndex)
        ?: parseCallIndirectContInstruction(fromIndex)

private fun List<Token>.parseUnreachableContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? =
    this[fromIndex].asKeywordMatching("unreachable")?.let { ParseResult(Unreachable, 1) }

private fun List<Token>.parseNoOpContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? =
    this[fromIndex].asKeywordMatching("nop")?.let { ParseResult(NoOp, 1) }

private fun List<Token>.parseReturnContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? =
    this[fromIndex].asKeywordMatching("return")?.let { ParseResult(Return, 1) }

private fun List<Token>.parseBreakContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    this[fromIndex].asKeywordMatching("br") ?: return null

    val labelIndex = parseIndex<Identifier.Label>(fromIndex + 1)
    return ParseResult(Break(labelIndex.astNode), 2)
}

private fun List<Token>.parseBreakIfContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    this[fromIndex].asKeywordMatching("br_if") ?: return null

    val labelIndex = parseIndex<Identifier.Label>(fromIndex + 1)
    return ParseResult(BreakIf(labelIndex.astNode), 2)
}

private fun List<Token>.parseBreakTableContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    this[fromIndex].asKeywordMatching("br_table") ?: return null

    var nodesParsed = 1
    val parsedLabelIndexes = parseIndices<Identifier.Label>(fromIndex + nodesParsed, min = 1)
    nodesParsed += parsedLabelIndexes.parseLength

    val targets = parsedLabelIndexes.astNode.subList(0, parsedLabelIndexes.astNode.size - 1)
    val default = parsedLabelIndexes.astNode.last()

    return ParseResult(BreakTable(targets, default),  nodesParsed)
}

private fun List<Token>.parseCallContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    this[fromIndex].asKeywordMatching("call") ?: return null

    val index = parseIndex<Identifier.Function>(fromIndex + 1)
    return ParseResult(Call(index.astNode), 2)
}

private fun List<Token>.parseCallIndirectContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    this[fromIndex].asKeywordMatching("call_indirect") ?: return null

    val typeUse = parseTypeUse(fromIndex + 1)
    return ParseResult(CallIndirect(typeUse.astNode), 1 + typeUse.parseLength)
}
