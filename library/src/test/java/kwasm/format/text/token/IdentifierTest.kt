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

package kwasm.format.text.token

import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IdentifierTest {
    @Test
    fun emptyIdentifier_fails() {
        val actual = Identifier("$")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java).hasMessageContaining("Invalid identifier")
    }

    @Test
    fun identifierWithNoLeadingBling_fails() {
        val actual = Identifier("test")
        assertThatThrownBy { actual.value }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Identifier must begin with $")
    }

    @Test
    fun simpleIdentifier() {
        val idString = "test"
        val actual = Identifier("$$idString")
        assertThat(actual.value).isEqualTo("$$idString")
    }

    @Test
    fun validChars() {
        val idChars =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012345789!#$%&'*+-./:<=>?@\\^_`|~"

        var actual = Identifier("$$idChars")
        assertThat(actual.value).isEqualTo("$$idChars")

        idChars.forEach {
            actual = Identifier("$$it")
            assertThat(actual.value).isEqualTo("$$it")
        }
    }

    @Test
    fun getAstValue_buildsAstIdentifier() {
        val stringValue = "\$test"
        val parseNode = Identifier(stringValue)

        val expectedType = kwasm.ast.Identifier.Type(stringValue)
        assertThat(parseNode.getAstValue<kwasm.ast.Identifier.Type>()).isEqualTo(expectedType)

        val expectedFunction = kwasm.ast.Identifier.Function(stringValue)
        assertThat(parseNode.getAstValue<kwasm.ast.Identifier.Function>())
            .isEqualTo(expectedFunction)

        val expectedGlobal = kwasm.ast.Identifier.Global(stringValue)
        assertThat(parseNode.getAstValue<kwasm.ast.Identifier.Global>()).isEqualTo(expectedGlobal)

        val expectedLabel = kwasm.ast.Identifier.Label(stringValue)
        assertThat(parseNode.getAstValue<kwasm.ast.Identifier.Label>()).isEqualTo(expectedLabel)

        val expectedLocal = kwasm.ast.Identifier.Local(stringValue)
        assertThat(parseNode.getAstValue<kwasm.ast.Identifier.Local>()).isEqualTo(expectedLocal)

        val expectedMemory = kwasm.ast.Identifier.Memory(stringValue)
        assertThat(parseNode.getAstValue<kwasm.ast.Identifier.Memory>()).isEqualTo(expectedMemory)

        val expectedTable = kwasm.ast.Identifier.Table(stringValue)
        assertThat(parseNode.getAstValue<kwasm.ast.Identifier.Table>()).isEqualTo(expectedTable)
    }

    @Test
    fun getAstValue_throwsWhenAskingFor_typeDef() {
        val parseNode =
            Identifier("\$this_should_fail,_later")
        assertThatThrownBy { parseNode.getAstValue<kwasm.ast.Identifier.TypeDef>() }
            .isInstanceOf(ParseException::class.java)
            .hasMessageContaining("Unsupported AST Identifier")
    }
}
