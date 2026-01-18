/*
 * Copyright (c) 2024-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.benchmark.test

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalSimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleRestriction
import nl.adaptivity.xmlutil.newReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SchemaParseTest {
    @Test
    fun parseCompactFragment() {
        val xml = XML.v1()
        val schema = SchemaParseTest::class.java.getResourceAsStream("/xsts/saxonData/VC/vc003.xsd")!!.use {
            xml.decodeFromReader<XSSchema>(xmlStreaming.newReader(it))
        }
        val type = assertIs<XSLocalSimpleType>(schema.elements[0].localType)
        val restriction = assertIs<XSSimpleRestriction>(type.simpleDerivation)
        assertEquals(1, restriction.otherContents.size)
        val cf = restriction.otherContents.single()
        assertEquals(2, cf.namespaces.count())
    }
}
