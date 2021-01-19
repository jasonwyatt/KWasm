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

package kwasm.script

import kwasm.format.ParseContext
import kwasm.format.text.Tokenizer
import kwasm.format.text.token.Token
import kwasm.script.command.parseAndRunCommand
import kwasm.script.execution.ScriptContext
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.Reader

fun runScript(fileName: String) {
    runScript(BufferedReader(InputStreamReader(FileInputStream(fileName))), ParseContext(fileName))
}

fun runScript(raw: String, parseContext: ParseContext) {
    runScript(Tokenizer().tokenize(raw, parseContext))
}

fun runScript(input: Reader, parseContext: ParseContext) {
    runScript(Tokenizer().tokenize(input, parseContext))
}

fun runScript(tokens: List<Token>) {
    var position = 0
    val scriptContext = ScriptContext()
    while (position < tokens.size) {
        val result = tokens.parseAndRunCommand(position, scriptContext)
        position += result.parseLength
    }
}
