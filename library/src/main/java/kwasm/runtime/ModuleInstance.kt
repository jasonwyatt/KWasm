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

package kwasm.runtime

import kwasm.ast.type.FunctionType

/**
 * Represents an instantiated [kwasm.ast.module.WasmModule].
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#module-instances):
 * 
 * A module instance is the runtime representation of a module. It is created by instantiating a
 * module, and collects runtime representations of all entities that are imported, defined, or
 * exported by the module.
 *
 * ```
 *   moduleinst ::= {
 *                      types functype*,
 *                      funcaddrs funcaddr*,
 *                      tableaddrs tableaddr*,
 *                      memaddrs memaddr*,
 *                      globaladdrs globaladdr*,
 *                      exports exportinst*
 *                  }
 * ```
 * 
 * Each component references runtime instances corresponding to respective declarations from the
 * original module – whether imported or defined – in the order of their static indices. Function
 * instances, table instances, memory instances, and global instances are referenced with an
 * indirection through their respective addresses in the store.
 * 
 * It is an invariant of the semantics that all export instances in a given module instance have
 * different names.
 */
data class ModuleInstance(
    val types: List<FunctionType>,
    val functionAddresses: List<Address.Function>,
    val tableAddresses: List<Address.Table>,
    val memoryAddresses: List<Address.Memory>,
    val globalAddresses: List<Address.Global>,
    val exports: List<Export>
)
