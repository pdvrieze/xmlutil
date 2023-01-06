/*
 * Copyright (c) 2021.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import io.github.pdvrieze.formats.xmlschema.resolved.SimpleResolver
import kotlinx.serialization.SerializationException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.w3.xml.xmschematestsuite.*
import java.net.URI
import java.net.URL
import kotlin.experimental.ExperimentalTypeInference
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertIs

class TestXSTestSuite {

    val xml = XML { autoPolymorphic = true }

    @DisplayName("Test suites: suite.xml")
    @TestFactory
    fun testFromTestSetRef(): List<DynamicNode> {
        val suiteURL: URL = javaClass.getResource("/xsts/suite.xml")

        suiteURL.withXmlReader { xmlReader ->
            val suite = xml.decodeFromReader<TSTestSuite>(xmlReader)
            return suite.testSetRefs
                .filter { false || it.href.contains("sunMeta/suntest.testSet") }
                .map { setRef ->

                val setBaseUrl: URI = javaClass.getResource("/xsts/${setRef.href}").toURI()
                val testSet = setBaseUrl.withXmlReader { r -> xml.decodeFromReader<TSTestSet>(r) }

                val folderName = setRef.href.substring(0, setRef.href.indexOf('/')).removeSuffix("Meta")

                val tsName = "$folderName - ${testSet.name}"

                buildDynamicContainer("Test set $tsName") {
                    for (group in testSet.testGroups) {
                        if (true || group.name.contains("xsd003b.e")) {
                            dynamicContainer("Group ${group.name}") {
                                addSchemaTests(setBaseUrl, group)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun SequenceScope<DynamicNode>.addSchemaTests(
        setBaseUrl: URI,
        group: TSTestGroup
    ) {
        group.schemaTest?.let { schemaTest ->
            val documentation = group.documentationString()
            if (schemaTest.schemaDocuments.size == 1) {
                val schemaDoc = schemaTest.schemaDocuments.single()
                addSchemaDocTest(setBaseUrl, schemaTest, schemaDoc, documentation)
            } else {
                dynamicContainer("Schema documents") {
                    for (schemaDoc in schemaTest.schemaDocuments) {
                        addSchemaDocTest(setBaseUrl, schemaTest, schemaDoc, documentation)
                    }
                }
            }
        }
    }

    private suspend fun SequenceScope<DynamicNode>.addSchemaDocTest(
        setBaseUrl: URI,
        schemaTest: TSSchemaTest,
        schemaDoc: TSSchemaDocument,
        documentation: String
    ) {
        val resolver = SimpleResolver(setBaseUrl)

        val expected = schemaTest.expected
        if (expected?.validity == TSValidityOutcome.INVALID) {
            dynamicTest("Schema document ${schemaDoc.href} exists") {
                assertNotNull(setBaseUrl.resolve(schemaDoc.href).toURL().openStream())
            }
            dynamicTest("Schema document ${schemaDoc.href} should not parse or be found invalid") {
                val e = assertFails(documentation) {
                    val schemaLocation = VAnyURI(schemaDoc.href)
                    val schema = resolver.readSchema(schemaLocation)
                    val resolvedSchema = schema.resolve(resolver.delegate(schemaLocation))
                    resolvedSchema.check()
                }
                if (e is Error) throw e

                val exName = expected.exception
                if (exName != null) {
                    if (exName.contains('.')) {
                        assertEquals(exName, e.javaClass.name)
                    } else {
                        assertEquals(exName, e.javaClass.name.substringAfterLast('.'))
                    }
                }

                val exMsg = expected.message?.let { Regex(it.pattern, setOf(RegexOption.UNIX_LINES))}
                if (exMsg!=null) {
                    if (! exMsg.containsMatchIn(e.message ?: "")) {
                        val match = exMsg.find(e.message ?: "")?.value
                        if (match != null) {
                            assertEquals("${exMsg.pattern}\n$match", "${exMsg.pattern}\n${e.message ?: ""}")
                        } else {
                            assertEquals(exMsg.pattern, e.message)
                        }
                    }
                } else {
                    System.err.println("Expected error: \n")
                    System.err.println(documentation.prependIndent("        "))
                    System.err.println("    Exception thrown:")
                    System.err.println(e.message?.prependIndent("        "))
                }
            }
        } else {
            dynamicTest("Schema document ${schemaDoc.href} parses") {
                val schema = resolver.readSchema(VAnyURI(schemaDoc.href))
                assertNotNull(schema)
            }
        }
    }


}

@OptIn(ExperimentalTypeInference::class)
internal fun buildDynamicContainer(
    displayName: String,
    @BuilderInference block: suspend SequenceScope<DynamicNode>.() -> Unit
): DynamicContainer {
    return DynamicContainer.dynamicContainer(displayName, sequence(block).asIterable())
}

internal suspend fun SequenceScope<in DynamicTest>.dynamicTest(displayName: String, testBody: () -> Unit) {
    yield(DynamicTest.dynamicTest(displayName, testBody))
}

@OptIn(ExperimentalTypeInference::class)
internal suspend fun SequenceScope<in DynamicContainer>.dynamicContainer(displayName: String, @BuilderInference block: suspend SequenceScope<DynamicNode>.() -> Unit) {
    yield(DynamicContainer.dynamicContainer(displayName, sequence(block).asIterable()))
}

inline fun <R> URI.withXmlReader(body: (XmlReader)->R): R {
    return toURL().withXmlReader(body)
}

inline fun <R> URL.withXmlReader(body: (XmlReader) -> R): R {
    return openStream().use { inStream ->
        XmlStreaming.newReader(inStream, "UTF-8").use(body)
    }
}
