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

package kwasm.example.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kwasm.KWasmProgram
import kwasm.api.ByteBufferMemoryProvider
import kwasm.api.HostFunction
import kwasm.runtime.EmptyValue
import kwasm.runtime.IntValue
import kwasm.runtime.toValue

class MainActivity : AppCompatActivity() {
    lateinit var scope: CoroutineScope
    lateinit var programBuilder: Deferred<KWasmProgram.Builder>
    var clicks = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope = MainScope()
        setContentView(R.layout.activity_main)

        programBuilder = scope.async {
            withContext(Dispatchers.IO) {
                KWasmProgram.builder(ByteBufferMemoryProvider(1024 * 1024))
                    .withTextFormatModule(
                        "module",
                        this@MainActivity.assets.open("toast_on_launch.wat")
                    )
                    .withHostFunction(
                        namespace = "host",
                        name = "toast",
                        hostFunction = HostFunction { offset: IntValue, len: IntValue, context ->
                            val bytes = ByteArray(len.value)
                            context.memory?.readBytes(bytes, offset.value)
                            scope.launch {
                                Toast.makeText(
                                    this@MainActivity,
                                    bytes.toString(Charsets.UTF_8),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            EmptyValue
                        }
                    )
                    .withHostFunction(
                        namespace = "host",
                        name = "getMessageSetting",
                        hostFunction = HostFunction { _ -> clicks.toValue() }
                    )
            }
        }

        findViewById<Button>(R.id.button).setOnClickListener {
            scope.launch {
                withContext(Dispatchers.Default) {
                    programBuilder.await().build() // Runs the start function
                }
                clicks++
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
