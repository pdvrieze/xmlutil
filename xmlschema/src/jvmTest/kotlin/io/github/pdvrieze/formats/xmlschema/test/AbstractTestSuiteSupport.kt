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

package io.github.pdvrieze.formats.xmlschema.test

import io.github.pdvrieze.formats.xmlschemaTests.Resource
import io.github.pdvrieze.formats.xmlschemaTests.getResource
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.DynamicNode
import org.w3.xml.xmschematestsuite.*
import org.w3.xml.xmschematestsuite.override.CompactOverride
import org.w3.xml.xmschematestsuite.override.OTSSuite

abstract class AbstractTestSuiteSupport {

    abstract val xml: XML

    val testSuite: TSTestSuite by lazy {
        val suiteResource: Resource = getResource("/xsts/suite.xml")
        suiteResource.withXmlReader { xmlReader -> xml.decodeFromReader(xmlReader) }
    }

    val overrides: OTSSuite by lazy {
        val compact = getResource("/override.xml").withXmlReader { xml.decodeFromReader<CompactOverride>(it) }
        OTSSuite(compact)
    }

    private val testSetCache = HashMap<String, TestSetInfo>()

    @OptIn(ExperimentalStdlibApi::class)
    fun getTestSets(filter: (TSTestSetRef) -> Boolean): List<TestSetInfo> {
        return testSuite.testSetRefs
            .asSequence()
            .filter(filter)
            .map { testSetRef ->
                testSetCache.getOrPutIfMissing(testSetRef.href) {
                    val resource = getResource("/xsts/${testSetRef.href}")
                    val ts = overrides.applyTo(resource.withXmlReader {
                        xml.decodeFromReader<TSTestSet>(it)
                    })
                    TestSetInfo(resource, testSetRef, ts)
                }
            }.toList()
    }

    data class TestSetInfo(val resource: Resource, val ref: TSTestSetRef, val testSet: TSTestSet) {
        internal suspend inline fun SequenceScope<DynamicNode>.groupTest(
            groupFilter: (TSTestGroup) -> Boolean = { true },

        ) {
            for (group in testSet.testGroups) {
                if (groupFilter(group)) {
                    dynamicContainer("Group '${group.name}'") {

                        val testSetVersion = testSet.schemaVersion?.let(::listOf)
                        var targetSchemaDoc: TSSchemaDocument? = null

                        val schemaTest = group.schemaTest
                        if (schemaTest != null) {
                            val documentation = group.documentationString()
                            if (schemaTest.schemaDocuments.size == 1) {
                                val schemaDoc = schemaTest.schemaDocuments.single()
                                addSchemaDocTest(
                                    resource,
                                    schemaTest,
                                    schemaDoc,
                                    documentation,
                                    group.version?.let(::listOf) ?: testSetVersion
                                )
                                targetSchemaDoc = schemaDoc
                            } else {
                                dynamicContainer("Schema documents") {
                                    for (schemaDoc in schemaTest.schemaDocuments) {
                                        if (true || schemaDoc.href.contains("ipo.xsd")) {
                                            addSchemaDocTest(
                                                resource,
                                                schemaTest,
                                                schemaDoc,
                                                documentation,
                                                group.version?.let(::listOf) ?: testSetVersion
                                            )
                                            targetSchemaDoc = schemaDoc
                                        }
                                    }
                                }
                            }
                        }
                        if (false && targetSchemaDoc != null && group.instanceTests.isNotEmpty()) {

                            for (instanceTest in group.instanceTests) {
                                addInstanceTest(resource, instanceTest, targetSchemaDoc!!, group.documentationString())
                            }
                        }
                    }
                }
            }


        }

        val displayName: String
            get() {
                return "${testSet.name} (${ref.href})"
            }
    }

}
