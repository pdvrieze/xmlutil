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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
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
                .filter { it.href.contains("NIST") }
                .map { setRef ->

                val setBaseUrl: URI = javaClass.getResource("/xsts/${setRef.href}").toURI()
                val testSet = setBaseUrl.withXmlReader { r -> xml.decodeFromReader<TSTestSet>(r) }

                val folderName = setRef.href.substring(0, setRef.href.indexOf('/')).removeSuffix("Meta")

                val tsName = "$folderName - ${testSet.name}"

                buildDynamicContainer("Test set $tsName") {
                    for (group in testSet.testGroups) {
                        if (group.name.contains("decimal-minExclusive-1")) {
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
        if (schemaTest.expected?.validity == TSValidityOutcome.INVALID) {
            dynamicTest("Schema document ${schemaDoc.href} exists") {
                assertNotNull(setBaseUrl.resolve(schemaDoc.href).toURL().openStream())
            }
            dynamicTest("Schema document ${schemaDoc.href} does not parse or is invalid") {
                val e = assertFails(documentation) {
                    setBaseUrl.resolve(schemaDoc.href).withXmlReader { r ->
                        val schema = xml.decodeFromReader<XSSchema>(r)
                        schema.check()
                    }
                }
                if (e is NotImplementedError) throw e
                System.err.println("Expected error: \n")
                System.err.println(documentation.prependIndent("    "))
            }
        } else {
            dynamicTest("Schema document ${schemaDoc.href} parses") {
                setBaseUrl.resolve(schemaDoc.href).withXmlReader { r ->
                    val schema = xml.decodeFromReader<XSSchema>(r)
                    assertNotNull(schema)
                }
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
