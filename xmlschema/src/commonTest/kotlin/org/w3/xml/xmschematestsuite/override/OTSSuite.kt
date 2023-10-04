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

    constructor(compact: CompactOverride) : this(toTestSets(compact))

    fun applyTo(original: TSTestSet): TSTestSet {
        return testSetOverrides
            .firstOrNull { it.name == original.name }
            ?.applyTo(original)
            ?: return original
    }

    fun findIgnoredPaths(): List<TestPath> {
        return testSetOverrides.flatMap { it.findIgnoredPaths() }
    }

    fun findOverrides(): List<ExpectedOverride> {
        return testSetOverrides.flatMap { it.findOverrides() }
    }

    companion object {
        private fun toTestSets(compact: CompactOverride): List<OTSTestSet> {
            val ignoreMap = compact.ignores.groupBy { it.testSet }
            val overrideMap = compact.overrides.groupBy { it.path.testSet }

            val setNames = (ignoreMap.keys + overrideMap.keys).sorted()

            return setNames.map { tsName ->
                val ignores = ignoreMap[tsName]?: emptyList()
                val isIgnored = ignores.any { it.group==null }
                val overrides = overrideMap[tsName] ?: emptyList()

                val newOverrides = toGroups(overrides, ignores)

                OTSTestSet(tsName, newOverrides, isIgnored)
            }
        }

        private fun toGroups(overrides: List<ExpectedOverride>, ignores: List<TestPath>): List<OTSTestGroup> {
            val ignoreMap = ignores.filter { it.group != null }.groupBy { it.group!! }
            val overrideMap = overrides.filter { it.path.group != null }.groupBy { it.path.group!! }
            val groupNames = (ignoreMap.keys + overrideMap.keys).sorted()

            return groupNames.map { groupName ->
                val grOverrides = overrideMap[groupName] ?: emptyList()
                val grIgnores = ignoreMap[groupName] ?: emptyList()
                val isIgnored = grIgnores.any { it.test == null }

                val schemaTest = grOverrides.singleOrNull { it.path.test!=null && !it.isInstance }?.let {
                    val testName = it.path.test!!
                    OTSSchemaTest(testName, expecteds = it.expecteds, isIgnored = grIgnores.any { it.test == testName })
                }

                val instanceTests = grOverrides.filter { it.path.test!=null && it.isInstance }.map {
                    val testName = it.path.test!!
                    OTSInstanceTest(testName, expecteds = it.expecteds, isIgnored = grIgnores.any { it.test == testName })
                }

                OTSTestGroup(groupName, schemaTest, instanceTests, isIgnored)
            }

            return TODO()
        }
    }
}

@Serializable
data class OTSTestSet(val name: String, val groups: List<OTSTestGroup> = emptyList(), val isIgnored: Boolean = false) {
    fun applyTo(original: TSTestSet): TSTestSet {
        val newGroups: List<TSTestGroup> = when {
            isIgnored -> emptyList()
            else -> {
                val associations = groups.associateBy { it.name }
                original.testGroups.mapNotNull {
                    when(val a = associations.get(it.name)) {
                        null -> it
                        else -> if (a.isIgnored) null else a.applyTo(it)
                    }
                }
            }
        }

        return TSTestSet(
            original.contributor,
            original.name,
            original.version,
            original.annotation,
            newGroups
        )
    }

    fun findIgnoredPaths(): Sequence<TestPath> {
        return when {
            isIgnored -> sequenceOf(TestPath(name))
            else -> groups.asSequence().flatMap { it.findIgnoredPaths(TestPath(name)) }
        }
    }

    fun findOverrides(): Sequence<ExpectedOverride> {
        return groups.asSequence().flatMap { it.findOverrides(TestPath(name)) }
    }
}

@Serializable
data class OTSTestGroup(
    val name: String,
    val schemaTest: OTSSchemaTest? = null,
    val instanceTests: List<OTSInstanceTest> = emptyList(),
    val isIgnored: Boolean = false
) {
    fun applyTo(original: TSTestGroup): TSTestGroup {
        val newSchemaTest = original.schemaTest?.let { ot ->
            when (val st = schemaTest) {
                null -> ot
                else -> if (st.isIgnored) null else st.applyTo(ot)
            }
        }
        val instanceMap = instanceTests.associateBy { it.name }
        val newInstanceTests = original.instanceTests.mapNotNull {
            when (val map = instanceMap[it.name]) {
                null -> it
                else -> if (map.isIgnored) null else map.applyTo(it)
            }
        }

        return original.copy(
            schemaTest = newSchemaTest,
            instanceTests = newInstanceTests,
        )
    }

    fun findIgnoredPaths(base: TestPath): Sequence<TestPath> {
        val newBase = base.copy(group = name)
        return when {
            isIgnored -> sequenceOf(newBase)
            else -> sequence {
                if (schemaTest != null && schemaTest.isIgnored) yield(newBase.copy(test=schemaTest.name, isSchema = true))
                yieldAll(instanceTests.asSequence().filter { it.isIgnored }.map { newBase.copy(test=it.name, isSchema = false)})
            }
        }
    }

    fun findOverrides(base: TestPath): Sequence<ExpectedOverride> {
        val newBase = base.copy(group=name)
        return sequence {
            if (schemaTest != null) yield(
                ExpectedOverride(newBase.copy(test = schemaTest.name, isSchema = true), schemaTest.expecteds)
            )
            yieldAll(instanceTests.asSequence().map {
                ExpectedOverride(newBase.copy(test=it.name, isSchema = false), it.expecteds)
            })
        }
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
data class OTSSchemaTest(
    val name: String,
    val version: String? = null,
    val expecteds: List<TSExpected> = emptyList(),
    val isIgnored: Boolean = false,
) {

    fun applyTo(original: TSSchemaTest): TSSchemaTest {
        val newExpected: List<TSExpected> = mergeExpecteds(original.expected, expecteds)
        return original.copy(version = version ?: original.version, expected = newExpected)
    }

}

@Serializable
data class OTSInstanceTest(
    val name: String,
    val version: String? = null,
    val expecteds: List<TSExpected> = emptyList(),
    val isIgnored: Boolean = false,
) {
    fun applyTo(original: TSInstanceTest): TSInstanceTest {
        val newExpected: List<TSExpected> = mergeExpecteds(original.expected, expecteds)
        return original.copy(version = version ?: original.version, expected = newExpected)
    }

}
