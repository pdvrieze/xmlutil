/*
 * Copyright (c) 2026.
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

package nl.adaptivity.xmlutil.serialization

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import io.github.pdvrieze.formats.xmlschemaTests.getResource
import io.github.pdvrieze.formats.xmlschemaTests.openStream
import nl.adaptivity.xmlutil.core.KtXmlReader
import kotlin.test.Test

class TestParseSchema {
    @Test
    fun testParseSchema() {
        val r = getResource("/xsts/msData/additional/test73722.xsd")
        val reader = KtXmlReader(r.openStream())
        val _ = XML.v1.decodeFromReader<XSSchema>(reader)
    }
}
