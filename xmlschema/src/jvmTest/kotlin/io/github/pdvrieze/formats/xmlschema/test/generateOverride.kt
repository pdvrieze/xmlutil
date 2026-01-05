/*
 * Copyright (c) 2023-2026.
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

package io.github.pdvrieze.formats.xmlschema.test

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import nl.adaptivity.xmlutil.serialization.XML
import org.w3.xml.xmschematestsuite.*
import org.w3.xml.xmschematestsuite.override.*
import java.io.FileOutputStream
import java.net.URI
import java.net.URL

private val xml = XML.v1.recommended()

fun main() {
    val suiteURL: URL = OTSSuite::class.java.getResource("/xsts/suite.xml")!!
    val override: OTSSuite? = suiteURL.withXmlReader { suiteReader ->
        val suite = xml.decodeFromReader<TSTestSuite>(suiteReader) as TSTestSuite
        findOverrides(suite)
    }

    if (override != null) {
        FileOutputStream("override.json").use { out ->
            Json {
                prettyPrint = true
                prettyPrintIndent = "  "
            }.encodeToStream(override, out)
        }
    }


}

private fun findOverrides(suite: TSTestSuite): OTSSuite? {
    val setOverrides = suite.testSetRefs.mapNotNull { setRef ->
        val setBaseUrl: URI = OTSSuite::class.java.getResource("/xsts/${setRef.href}")!!.toURI()
        val testSet = setBaseUrl.withXmlReader { setReader -> xml.decodeFromReader<TSTestSet>(setReader) }
        findOverrides(testSet, setBaseUrl)
    }
    if (setOverrides.isEmpty()) return null
    return OTSSuite(setOverrides)
}

private fun findOverrides(testSet: TSTestSet, baseUrl: URI): OTSTestSet? {
    println("Processing test set: ${testSet.name}")
    val groupOverrides = testSet.testGroups.mapNotNull { group -> findOverrides(group) }

    if (groupOverrides.isEmpty()) return null
    return OTSTestSet(testSet.name, groupOverrides)
}

private fun findOverrides(group: TSTestGroup): OTSTestGroup? {
    println("    Processing test group: ${group.name}")
    val schemaTest = group.schemaTest?.let { findOverrides(it) }
    val instanceTests = group.instanceTests.mapNotNull { findOverrides(it) }
    if (schemaTest == null && instanceTests.isEmpty()) return null
    return OTSTestGroup(group.name, schemaTest, instanceTests)
}

private fun findOverrides(test: TSSchemaTest): OTSSchemaTest? {
    println("        Processing schema test: ${test.name}")
    val newExpected = test.expected.filter { it.exception != null || it.message != null }
    if (newExpected.isEmpty()) return null
    return OTSSchemaTest(test.name, null, newExpected)
}

private fun findOverrides(test: TSInstanceTest): OTSInstanceTest? {
    println("        Processing instance test: ${test.name}")
    val newExpected = test.expected.filter { it.exception != null || it.message != null }
    if (newExpected.isEmpty()) return null
    return OTSInstanceTest(test.name, null, newExpected,)
}
