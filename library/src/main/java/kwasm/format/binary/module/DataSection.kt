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

package kwasm.format.binary.module

import kwasm.ast.module.DataSegment
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readVector

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#data-section):
 *
 * The data section has the id 11. It decodes into a vector of data segments that represent the
 * `data` component of a module.
 *
 * ```
 *      datasec ::= seg*:section_11(vec(data))      => seg
 *      data    ::= x:memidx e:expr b*:vec(byte)    => {data x, offset e, init b*}
 * ```
 */
fun BinaryParser.readDataSection(): DataSection = DataSection(readVector { readDataSegment() })

/** Reads a data segment from a [DataSection] in a binary-encoded WebAssembly module. */
internal fun BinaryParser.readDataSegment(): DataSegment =
    DataSegment(readIndex(), readOffset(), readVector().toByteArray())

/** Represents a data section from a binary-encoded WebAssembly module. */
data class DataSection(val data: List<DataSegment>) : Section