/*
 * Copyright (c) 2022.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.test

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleRestriction
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.Test

class TestRestriction {

    @Test
    fun testParseSimpleRestriction() {
        val input = "<xs:restriction xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" base=\"xs:decimal\">\n" +
                "      <xs:minExclusive value=\"-999999999999999999\"/>\n" +
                "    </xs:restriction>"

        val data = XML { autoPolymorphic = true }.decodeFromString(XSSimpleRestriction.serializer(), input)
    }
}
