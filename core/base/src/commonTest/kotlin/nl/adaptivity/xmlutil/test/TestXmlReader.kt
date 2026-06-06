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

package nl.adaptivity.xmlutil.test

import nl.adaptivity.xmlutil.DomReader
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test

class TestXmlReader : TestCommonReader() {

    override fun createReader(xml: String): XmlReader = xmlStreaming.newReader(xml)

    @Test
    override fun testReadUnknownEntity() {
        val r = createReader("<x/>")

        @Suppress("DEPRECATION")
        if (r is KtXmlReader || (testTarget != Target.Browser) && (r is DomReader)) {
            super.testReadUnknownEntity()
        }
    }
}
