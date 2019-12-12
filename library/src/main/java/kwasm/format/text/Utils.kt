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

import kwasm.format.ParseContext
import kwasm.format.ParseException

fun getOperationAndParameters(sequence: CharSequence, context: ParseContext?): Pair<CharSequence, List<CharSequence>> {
    if (sequence.first() != '(') {
        throw ParseException("Expecting opening ( for Operation", context)
    }
    if (sequence.last() != ')') {
        throw ParseException("Expecting closing ) for Operation", context)
    }

    val splitSequence = sequence.substring(1, sequence.lastIndex).split("\\s".toRegex())
    return Pair(splitSequence[0], splitSequence.subList(1,splitSequence.lastIndex+1))
}
