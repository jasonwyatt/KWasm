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
import kwasm.script.command.Command
import kwasm.script.command.ScriptModule
import kwasm.script.execution.ScriptContext

class AssertUnlinkable(
    val action: ScriptModule,
    val messageContains: String
) : AstNode, Command<Unit> {
    override fun execute(context: ScriptContext) {
        TODO("Not yet implemented")
    }
}