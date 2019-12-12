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

package kwasm.ast

/**
 * Data class to hold the parameters and results of a FunctionType
 * from [the docs](https://webassembly.github.io/spec/core/text/types.html#function-types):
 *
 * Where functype is a [kwasm.format.text.Type.FunctionType]:
 *
 * ```
 *   functype ::=  ‘(’ ‘func’  t∗1:vec(param)  t∗2:vec(result) ‘)’ -> [t∗1]→[t∗2]
 *   param    ::=  ‘(’ ‘param’  id?  t:valtype ‘)’                 -> t
 *   result   ::=  ‘(’ ‘result’  t:valtype ‘)’                     -> t
 * ```
 */
data class FunctionType(
    val parameters: List<Param>,
    val returnValues: List<ValueType>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FunctionType

        if (parameters != other.parameters) return false
        if (returnValues != other.returnValues) return false

        return true
    }

    override fun hashCode(): Int {
        var result = parameters.hashCode()
        result = 31 * result + returnValues.hashCode()
        return result
    }
}