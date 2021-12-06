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

import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.w3.xml.xmschematestsuite.TSTestSet
import org.w3.xml.xmschematestsuite.TSTestSuite
import java.net.URI
import java.net.URL
import kotlin.experimental.ExperimentalTypeInference

class TestXSTestSuite {

    @DisplayName("Test suites: suite.xml")
    @TestFactory
    fun testFromTestSetRef(): List<DynamicNode> {
        val xml = XML { autoPolymorphic = true }
        val suiteURL: URL = javaClass.getResource("/xsts/suite.xml")
        XmlStreaming.newReader(suiteURL.openStream(), "UTF-8").use { xmlReader ->
            val suite = xml.decodeFromReader<TSTestSuite>(xmlReader)
            return suite.testSetRefs.map { setRef ->

                val setBaseUrl: URI? = javaClass.getResource("/xsts/${setRef.href}")?.toURI()
                val testSet = setBaseUrl?.toURL()?.openStream()
                    ?.let { str -> xml.decodeFromReader<TSTestSet>(XmlStreaming.newReader(str, "UTF-8")) }

                val tsName = testSet?.name?.let{} ?: setRef.href.removeSuffix(".testSet")

                buildDynamicContainer("Test set $tsName") {
                    dynamicTest("Test set file ${setRef.href} set exists") {
                        javaClass.getResourceAsStream("/xsts/${setRef.href}").use { stream ->
                            assertNotNull(stream)
                        }
                    }
                    for (group in testSet!!.testGroups) {
                        dynamicContainer("Group ${group.name} exists") {
                            group.schemaTest?.let { schemaTest ->
                                if (schemaTest.schemaDocuments.size==1) {
                                    val schemaDoc = schemaTest.schemaDocuments.single()
                                    dynamicTest("Schema document ${schemaDoc.href}") {
                                        val docUrl = setBaseUrl.resolve(schemaDoc.href)
                                        assertNotNull(docUrl.toURL().openStream())
                                    }
                                } else {
                                    dynamicContainer("Schema documents") {
                                        for (schemaDoc in schemaTest.schemaDocuments) {
                                            dynamicTest("Schema document ${schemaDoc.href}") {
                                                val docUrl = setBaseUrl.resolve(schemaDoc.href)
                                                assertNotNull(docUrl.toURL().openStream())
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }

                    dynamicTest("Read set") {

                        assertNotNull(testSet)

                    }
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
