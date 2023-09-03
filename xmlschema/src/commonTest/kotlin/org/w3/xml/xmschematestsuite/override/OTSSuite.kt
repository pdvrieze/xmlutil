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

package org.w3.xml.xmschematestsuite.override

import kotlinx.serialization.Serializable
import org.w3.xml.xmschematestsuite.*

@Serializable
data class OTSSuite(val testSetOverrides: List<OTSTestSet>) {
    fun applyTo(original: TSTestSet): TSTestSet {
        return testSetOverrides
            .firstOrNull { it.name == original.name }
            ?.applyTo(original)
            ?: return original
    }
}

@Serializable
data class OTSTestSet(val name: String, val groups: List<OTSTestGroup> = emptyList()) {
    fun applyTo(original: TSTestSet): TSTestSet {
        val associations = groups.associateBy { it.name }
        val newGroups = original.testGroups.map {
            associations.get(it.name)?.applyTo(it) ?: it
        }
        return TSTestSet(
            original.contributor,
            original.name,
            original.version,
            original.annotation,
            newGroups
        )
    }
}

@Serializable
data class OTSTestGroup(
    val name: String,
    val schemaTest: OTSSchemaTest? = null,
    val instanceTests: List<OTSInstanceTest> = emptyList()
) {
    fun applyTo(original: TSTestGroup): TSTestGroup {
        val newSchemaTest = original.schemaTest?.let { ot -> schemaTest?.applyTo(ot) ?: ot }
        val instanceMap = instanceTests.associateBy { it.name }
        val newInstanceTests = original.instanceTests.map { instanceMap[it.name]?.applyTo(it) ?: it }

        return original.copy(
            schemaTest = newSchemaTest,
            instanceTests = newInstanceTests,
        )
    }
}

private fun mergeExpecteds(originalExpected: List<TSExpected>, overridden: List<TSExpected>): List<TSExpected> {
    val newExpected: List<TSExpected> = when {
        overridden.all { it.version == null } &&
                originalExpected.all { it.version == null } -> overridden // just override
        else -> {
            val ex = arrayOfNulls<TSExpected>(2)
            for (e in originalExpected) {
                when (e.version) {
                    null -> {
                        if (ex[0] == null) ex[0] = e.copy(version = "1.0")
                        if (ex[1] == null) ex[1] = e.copy(version = "1.1")
                    }

                    "1.0" -> ex[0] = e
                    "1.1" -> ex[1] = e
                }
            }
            for (e in overridden) {
                when (e.version) {
                    null -> {
                        if (overridden.none { it.version == "1.0" }) ex[0] = e.copy(version = "1.0")
                        if (overridden.none { it.version == "1.1" }) ex[1] = e.copy(version = "1.1")
                    }

                    "1.0" -> ex[0] = e
                    "1.1" -> ex[1] = e
                }
            }
            ex.filterNotNull()
        }
    }
    return newExpected
}


@Serializable
data class OTSSchemaTest(val name: String, val expecteds: List<TSExpected> = emptyList()) {
    fun applyTo(original: TSSchemaTest): TSSchemaTest {
        val newExpected: List<TSExpected> = mergeExpecteds(original.expected, expecteds)
        return original.copy(expected = newExpected)
    }

}

@Serializable
data class OTSInstanceTest(val name: String, val expecteds: List<TSExpected> = emptyList()) {
    fun applyTo(original: TSInstanceTest): TSInstanceTest {
        val newExpected: List<TSExpected> = mergeExpecteds(original.expected, expecteds)
        return original.copy(expected = newExpected)
    }

}
