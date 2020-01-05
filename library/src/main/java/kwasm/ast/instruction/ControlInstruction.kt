/*
 * Copyright 2020 Google LLC
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

package kwasm.ast.instruction

import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.ast.module.TypeUse
import kwasm.ast.type.ResultType

/**
 * Base class for all control [Instruction] implementations.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/text/instructions.html#control-instructions):
 *
 * Structured control instructions can bind an optional symbolic label identifier. The same label
 * identifier may optionally be repeated after the corresponding `end` and `else` pseudo
 * instructions, to indicate the matching delimiters.
 *
 * ```
 *   blockinstr(I) ::=
 *      ‘block’ I′:label(I) rt:resulttype (in:instr(I′))* ‘end’ id?
 *          => block rt in* end (if id? = ϵ ∨ id? = label)
 *      ‘loop’ I′:label(I) rt:resulttype (in:instr(I′))* ‘end’ id?
 *          => loop rt in* end (if id? = ϵ ∨ id? = label)
 *      ‘if’ I′:label(I) rt:resulttype (in^1:instr(I′))* (‘else’ id?^1 (in^2:instr(I'))*)? ‘end’ id?^2
 *          => if rt in*^1 else in*2 end (if id?^1 = ϵ ∨ id?^1 = label, id?^2 = ϵ ∨ id?^2 = label)
 * ```
 *
 * All other control instruction are represented verbatim.
 *
 * ```
 *   plaininstr(I)  ::= ‘unreachable’                                   => unreachable
 *                      ‘nop’                                           => nop
 *                      ‘br’ l:labelidx(I)                              => br l
 *                      ‘br_if’ l:labelidx(I)                           => br_if l
 *                      ‘br_table' l*:vec(labelidx(I)) l^N:labelidx(I)  => br_table l∗ l^N
 *                      ‘return’                                        => return
 *                      ‘call’ x:funcidx(I)                             => call x
 *                      ‘call_indirect’ x,I′:typeuse(I)                 => call_indirect x (if I′={})
 * ```
 *
 * **Note** The side condition stating that the identifier context `I′` must be empty in the rule
 * for `call_indirect` enforces that no identifier can be bound in any `param` declaration appearing
 * in the type annotation.
 */
sealed class ControlInstruction : Instruction {
    data class Block(
        val label: Identifier.Label,
        val result: ResultType,
        val instructions: List<Instruction>
    ) : ControlInstruction(), BlockInstruction

    data class Loop(
        val label: Identifier.Label,
        val result: ResultType,
        val instructions: List<Instruction>
    ) : ControlInstruction(), BlockInstruction

    data class If(
        val label: Identifier.Label,
        val result: ResultType,
        val positiveInstructions: List<Instruction>,
        val negativeInstructions: List<Instruction>
    ) : ControlInstruction(), BlockInstruction

    object Unreachable : ControlInstruction()

    object NoOp : ControlInstruction()

    data class Break(
        val labelIndex: Index<Identifier.Label>
    ) : ControlInstruction()

    data class BreakIf(
        val labelIndex: Index<Identifier.Label>
    ) : ControlInstruction()

    data class BreakTable(
        val targets: List<Index<Identifier.Label>>,
        val defaultTarget: Index<Identifier.Label>
    ) : ControlInstruction()

    object Return : ControlInstruction()

    data class Call(val functionIndex: Index<Identifier.Function>) : ControlInstruction()

    data class CallIndirect(val typeUse: TypeUse) : ControlInstruction()
}
