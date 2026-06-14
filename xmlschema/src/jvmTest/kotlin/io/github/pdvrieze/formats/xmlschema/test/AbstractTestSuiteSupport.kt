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
import org.w3.xml.xmschematestsuite.TSTestSet
import org.w3.xml.xmschematestsuite.TSTestSetRef
import org.w3.xml.xmschematestsuite.TSTestSuite
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
        val displayName: String
            get() {
                return "${testSet.name} (${ref.href})"
            }
    }

}
