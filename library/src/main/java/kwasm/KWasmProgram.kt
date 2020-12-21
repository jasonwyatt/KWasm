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

package kwasm

import kwasm.api.HostFunction
import kwasm.api.MemoryProvider
import kwasm.ast.module.Index
import kwasm.ast.module.WasmFunction
import kwasm.ast.module.WasmModule
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import kwasm.format.text.module.parseModule
import kwasm.runtime.Address
import kwasm.runtime.ExecutionContext
import kwasm.runtime.ImportExtern
import kwasm.runtime.Memory
import kwasm.runtime.ModuleInstance
import kwasm.runtime.Store
import kwasm.runtime.allocate
import kwasm.runtime.collectImportExterns
import kwasm.runtime.instruction.execute
import kwasm.runtime.stack.RuntimeStacks
import kwasm.runtime.toFunctionInstance
import kwasm.validation.module.validate
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader

/**
 *
 */
class KWasmProgram internal constructor(
    private val moduleExports: Map<String, Map<String, Address>>,
    private val allocatedModules: MutableMap<String, ModuleInstance>,
    private val store: Store
) {
    private val exportedMemoryAddresses =
        moduleExports.flatMap { it.value.values }.filter { it is Address.Memory }.toSet()
    private val exportedFunctionAddresses =
        moduleExports.mapValues { (_, moduleExps) ->
            moduleExps.filter { (_, expAddress) -> expAddress is Address.Function }
        }
    private val exportedGlobalAddresses =
        moduleExports.mapValues { (_, moduleExps) ->
            moduleExps.filter { (_, expAddress) -> expAddress is Address.Global }
        }

    val memory: Memory
        get() {
            val address = requireNotNull(exportedMemoryAddresses.firstOrNull()) {
                "No exported memories available."
            }
            return store.memories[address.value]
        }

    class Builder internal constructor(
        private var memoryProvider: MemoryProvider
    ) {
        private val tokenizer = Tokenizer()
        private val hostExports = mutableMapOf<String, MutableMap<String, Address>>()
        private val parsedModules = mutableMapOf<String, WasmModule>()
        private val modulesInOrder = mutableListOf<Pair<String, WasmModule>>()
        private var store = Store()

        /**
         * Returns a [KWasmProgram] ready-to-use.
         *
         * Allocates and Instantiates all provided modules before returning.
         */
        fun build(): KWasmProgram {
            val allocatedModules = mutableMapOf<String, ModuleInstance>()
            val moduleImports = mutableMapOf<String, List<ImportExtern<out Address>>>()
            val moduleExports = mutableMapOf<String, MutableMap<String, Address>>()

            // Allocate the modules, and gather their exports.
            // Currently this is done strictly in the order in which the modules were added to the
            // builder. It would be nice to support automatically figuring out the correct order so
            // that dependencies don't need to be added in the correct order by the caller at all
            // times.
            modulesInOrder.forEach { (name, module) ->
                val myImports = module.collectImportExterns()
                val myExports = mutableMapOf<String, Address>()

                val validationContext = module.validate()
                val (newStore, allocation) = module.allocate(
                    validationContext,
                    memoryProvider,
                    myImports,
                    store
                )

                allocation.exports.forEach { export ->
                    myExports[export.name] = export.address
                }

                store = newStore
                moduleImports[name] = myImports
                moduleExports[name] = myExports
                allocatedModules[name] = allocation
            }

            // Link up the exports to the imports by updating ImportExtern addresses with their
            // proper values.
            moduleImports.forEach { (moduleName, imports) ->
                imports.forEach { import ->
                    val exportCandidate = moduleExports[import.moduleName]?.get(import.name)
                        ?: hostExports[import.moduleName]?.get(import.name)
                        ?: throw ImportNotFoundException(moduleName, import)

                    import.addressPlaceholder.value = when (import.addressPlaceholder) {
                        is Address.Function -> exportCandidate as? Address.Function
                        is Address.Table -> exportCandidate as? Address.Table
                        is Address.Memory -> exportCandidate as? Address.Memory
                        is Address.Global -> exportCandidate as? Address.Global
                    }?.value ?: throw ImportMismatchException(moduleName, exportCandidate)

                    import.addressPlaceholder.value = exportCandidate.value
                }
            }

            // Initialize globals for modules.
            modulesInOrder.forEach { (name, moduleNode) ->
                val importedGlobals =
                    moduleImports[name]?.mapNotNull {
                        it.addressPlaceholder as? Address.Global
                    } ?: emptyList()
                val allocatedModule = allocatedModules[name] ?: return@forEach
                val auxiliaryInstance = allocatedModule.toGlobalInitInstance(importedGlobals)
                val context = ExecutionContext(store, auxiliaryInstance, RuntimeStacks())

                moduleNode.globals.forEach { global ->
                    val updatedContext = global.initExpression.execute(context)
                    val address = requireNotNull(
                        allocatedModule.globalAddresses[Index.ByIdentifier(global.id)]
                    ) { "No address found for $global" }
                    val globalValue = store.globals[address.value]
                    globalValue.update(updatedContext.stacks.operands.pop().value)
                }
            }

            // Initialize the Element Segments and Data Segments.
            modulesInOrder.forEach { (name, moduleNode) ->
                val allocatedModule = checkNotNull(allocatedModules[name])
                val executionContext = ExecutionContext(store, allocatedModule, RuntimeStacks())

                // Check the element segments will fit in the tables, then place them there.
                moduleNode.elements.forEach { elementSegment ->
                    val offsetValue = elementSegment.offset.expression.execute(executionContext)
                            .stacks.operands.pop()
                    val offsetInt = checkNotNull(offsetValue.value as? Int)
                    val tableAddress =
                        checkNotNull(allocatedModule.tableAddresses[elementSegment.tableIndex])
                    val table = store.tables[tableAddress.value]
                    val elementEnd = offsetInt + elementSegment.init.size
                    check(table.maxSize >= elementEnd) {
                        "Element segment is too long for table"
                    }

                    elementSegment.init.forEachIndexed { loc, fnIndex ->
                        val addr = checkNotNull(allocatedModule.functionAddresses[fnIndex])
                        table.elements[offsetInt + loc] = addr
                    }
                }

                // Check the data segments will fit in the memories, then update the memory.
                moduleNode.data.forEach { dataSegment ->
                    val offsetValue = dataSegment.offset.expression.execute(executionContext)
                        .stacks.operands.pop()
                    val offsetInt = checkNotNull(offsetValue.value as? Int)
                    val memoryAddress =
                        checkNotNull(allocatedModule.memoryAddresses[dataSegment.memoryIndex])
                    val memory = store.memories[memoryAddress.value]
                    val dataEnd = offsetInt + dataSegment.init.size

                    memory.writeBytes(dataSegment.init, offsetInt)
                }
            }

            // Invoke the start functions.
            modulesInOrder.forEach { (name, module) ->
                val startFunction = module.start ?: return@forEach
                val allocatedModule = allocatedModules[name] ?: return@forEach
                val index = startFunction.funcIndex
                val addr = checkNotNull(allocatedModule.functionAddresses[index])
                val startContext = ExecutionContext(store, allocatedModule, RuntimeStacks())
                store.functions[addr.value].execute(startContext)
            }

            return KWasmProgram(moduleExports, allocatedModules, store)
        }

        /**
         * Provides a [HostFunction] for importing from the [WasmModule]s in the [KWasmProgram]
         * returned from [build].
         */
        fun withHostFunction(
            namespace: String,
            name: String,
            hostFunction: HostFunction<*>
        ) = apply {
            val allocation = store.allocateFunction(hostFunction.toFunctionInstance())
            hostExports.compute(namespace) { _, map ->
                val result = map ?: mutableMapOf()
                result[name] = allocation.allocatedAddress
                result
            }
            store = allocation.updatedStore
        }

        /**
         * Parses a [WasmModule] from the provided [source] and associates it with the
         * given [name] for the [KWasmProgram] returned from [build].
         */
        fun withModule(name: String, source: String) = apply {
            val moduleTree =
                tokenizer.tokenize(source, ParseContext(name)).parseModule(0)?.astNode
                    ?: throw ParseException("No module found in source for $name")

            withModule(name, moduleTree)
        }

        /**
         * Parses a text-format [WasmModule] from the provided [file] and associates
         * it with the given [name] for the [KWasmProgram] returned from [build].
         */
        fun withTextFormatModule(name: String, file: File) = apply {
            val moduleTree = parseTextModule(
                name,
                BufferedInputStream(FileInputStream(file)),
                ParseContext(file.path)
            )

            withModule(name, moduleTree)
        }

        /**
         * Parses a text-format [WasmModule] from the provided [sourceStream] and
         * associates it with the given [name] for the [KWasmProgram] returned from [build].
         *
         * The provided [InputStream] will be closed after being parsed-from.
         */
        fun withTextFormatModule(name: String, sourceStream: InputStream) = apply {
            val moduleTree = parseTextModule(name, sourceStream, ParseContext(name))
            parsedModules[name] = moduleTree
        }

        /**
         * Parses a binary-format [WasmModule] from the provided [file] and
         * associates it with the given [name] for the [KWasmProgram] returned from [build].
         *
         * The provided [InputStream] will be closed after being parsed-from.
         */
        fun withBinaryModule(name: String, file: File) = apply {
            TODO("Binary-formatted WebAssembly parsing is not yet supported")
        }

        /**
         * Parses a binary-format [WasmModule] from the provided [binaryStream] and
         * associates it with the given [name] for the [KWasmProgram] returned from [build].
         *
         * The provided [InputStream] will be closed after being parsed-from.
         */
        fun withBinaryModule(name: String, binaryStream: InputStream) = apply {
            TODO("Binary-formatted WebAssembly parsing is not yet supported")
        }

        private fun withModule(name: String, module: WasmModule) {
            parsedModules[name] = module
            modulesInOrder.add(name to module)
        }

        private fun parseTextModule(
            name: String,
            stream: InputStream,
            parseContext: ParseContext
        ): WasmModule = stream.use {
            tokenizer.tokenize(InputStreamReader(it), parseContext)
                .parseModule(0)?.astNode
                ?: throw ParseException("No module found in sourceStream for $name")
        }
    }

    /**
     * Thrown at linkage-time, when one of a module's imports couldn't be found.
     */
    class ImportNotFoundException(
        importingModule: String,
        extern: ImportExtern<*>
    ) : IllegalStateException("Module $importingModule's import not found: $extern")

    /**
     * Thrown at linkage-time, when an import was found, but the value was a mismatch for what was
     * required by the import.
     */
    class ImportMismatchException(
        importingModule: String,
        foundAddress: Address
    ) : IllegalStateException(
        "Import for $importingModule found, but was wrong type: $foundAddress"
    )

    companion object {
        fun builder(memoryProvider: MemoryProvider): Builder = Builder(memoryProvider)
    }
}
