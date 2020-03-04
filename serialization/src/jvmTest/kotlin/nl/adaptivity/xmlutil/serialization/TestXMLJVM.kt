/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ImplicitReflectionSerializer
import nl.adaptivity.xmlutil.StAXWriter
import nl.adaptivity.xmlutil.XmlStreaming
import org.junit.jupiter.api.Assertions.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.CharArrayWriter

/**
 * This test only tests JVM specific things, everything else is in the common tests.
 */
@OptIn(ImplicitReflectionSerializer::class)
object TestXMLJVM : Spek(
    {
        describe("A simple writer") {
            val writer = XmlStreaming.newWriter(CharArrayWriter())
            it("should be a STaXwriter") {
                assertTrue(writer is StAXWriter)
            }
        }
    })