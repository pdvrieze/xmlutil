/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xmlutil.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalEntitiesTests() {
    @Serializable
    @XmlSerialName("manifest", "http://example.com/", "")
    data class Manifest(
        @XmlElement(true) val text: String,
    )

    val xml: XML = XML {
        indent = 4
        defaultPolicy {
            pedantic = false
        }
    }

    @Test
    fun testReadFileDefault() {
        val manifest =
            """
       <?xml version="1.0" standalone="no" ?>
       <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
       <manifest xmlns="http://example.com/">
         <text>&xxe;</text>
       </manifest>
   """.trimIndent()
        try {
            val deserialized = xml.decodeFromString<Manifest>(manifest)
            assertEquals(deserialized.text, "")
        } catch (e: Exception) {
            // This is allowed to throw an exception instead of returning empty. Different implementations have different
            // behaviour. Important is that local files are not accessible.
        }
    }

    @Test
    fun testPlatformReadFileReader() {
        testReadFileReaderImpl { xmlStreaming.newReader(it) }
    }

    @Test
    fun testGenericReadFileReader() {
        testReadFileReaderImpl { xmlStreaming.newGenericReader(it) }
    }

    fun testReadFileReaderImpl(readerFactory: (String) -> XmlReader) {
        val manifest =
            """
       <?xml version="1.0" standalone="no" ?>
       <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
       <manifest xmlns="http://example.com/">
         <text>&xxe;</text>
       </manifest>
   """.trimIndent()
        val reader = readerFactory(manifest)
        try {
            val deserialized = xml.decodeFromReader<Manifest>(reader)
            assertEquals(deserialized.text, "")
        } catch (e: Exception) {
            println("Exception on reading external entity allowed: ${e.message}")
            // This is allowed to throw an exception instead of returning empty. Different implementations have different
            // behaviour. Important is that local files are not accessible.
        }
    }
}
