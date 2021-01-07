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

import com.google.common.truth.Truth.assertThat
import kwasm.ast.type.ElementType
import kwasm.ast.type.Limits
import kwasm.ast.type.TableType
import kwasm.format.binary.BinaryParser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class TableSectionTest {
    @Test
    fun empty() {
        val section = TableSection(emptyList())
        val bytes = sequenceOf<Byte>(0x04) + section.toBytesNoHeader()
        val parser = BinaryParser(ByteArrayInputStream(bytes.toList().toByteArray()))
        assertThat(parser.readSection()).isEqualTo(section)
    }

    @Test
    fun nonEmpty() {
        val section = TableSection(
            listOf(
                TableType(Limits(0, 256), ElementType.FunctionReference),
                TableType(Limits(3, 1337), ElementType.FunctionReference),
            )
        )
        val bytes = sequenceOf<Byte>(0x04) + section.toBytesNoHeader()
        val parser = BinaryParser(ByteArrayInputStream(bytes.toList().toByteArray()))
        assertThat(parser.readSection()).isEqualTo(section)
    }
}
