/*
 * Copyright (c) 2024.
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

import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
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
            original.schemaVersions,
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
        overridden.all { it.versions == null } &&
                originalExpected.all { it.versions == null } -> overridden // just override
        else -> {
            val ex = arrayOfNulls<TSExpected>(2)
            for (e in originalExpected) {
                for (v in e.versions ?: listOf(SchemaVersion.entries)) {
                    when (v) {
                        SchemaVersion.V1_0 -> ex[0] = e
                        SchemaVersion.V1_1 -> ex[1] = e
                    }
                }
            }
            for (e in overridden) {
                for (v in e.versions ?: emptyList()) {
                    when (v) {
                        SchemaVersion.V1_0 -> ex[0] = e
                        else -> ex[1] = e
                    }
                }
                when (e.versions) {
                    null -> {
                        val versions = overridden.flatMapTo(HashSet()) { it.versions ?: emptyList() }
                        if (SchemaVersion.V1_0 !in versions) ex[0] = e.copy(versions = listOf(SchemaVersion.V1_0))
                        if (SchemaVersion.V1_1 !in versions) ex[0] = e.copy(versions = listOf(SchemaVersion.V1_1))
                    }
                }
            }
            ex.filterNotNull()
        }
    }
    return newExpected
}


@Serializable
@XmlSerialName("expected")
class OTSExpected : TSExpected {
    override val exception: String?

    @XmlElement(false)
    override val message: @Serializable(RegexSerializer::class) Regex?

    val annotation: String?

    constructor(
        validity: TSValidityOutcome,
        versions: List<SchemaVersion>? = null,
        exception: String? = null,
        message: Regex? = null,
        annotation: String? = null,
        otherAttributes: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
    ) : super(validity, versions, otherAttributes) {
        this.exception = exception
        this.message = message
        this.annotation = annotation
    }

    override fun copy(
        validity: TSValidityOutcome,
        versions: List<SchemaVersion>?,
        exception: String?,
        message: Regex?,
        annotation: String?,
        otherAttributes: Map<QName, String>
    ): OTSExpected = OTSExpected(validity, versions, exception, message, annotation, otherAttributes)
}


@Serializable
data class OTSSchemaTest(
    val name: String,
    @SerialName("version")
    @XmlElement(false)
    val versions: List<SchemaVersion>? = null,
    val expecteds: List<TSExpected> = emptyList(),
    val isIgnored: Boolean = false,
) {

    fun applyTo(original: TSSchemaTest): TSSchemaTest {
        val newExpected: List<TSExpected> = mergeExpecteds(original.expected, expecteds)
        return original.copy(versions = versions ?: original.versions, expected = newExpected)
    }

}

@Serializable
data class OTSInstanceTest(
    val name: String,
    @SerialName("version")
    @XmlElement(false)
    val versions: List<SchemaVersion>? = null,
    val expecteds: List<TSExpected> = emptyList(),
    val isIgnored: Boolean = false,
) {
    fun applyTo(original: TSInstanceTest): TSInstanceTest {
        val newExpected: List<TSExpected> = mergeExpecteds(original.expected, expecteds)
        return original.copy(versions = versions ?: original.versions, expected = newExpected)
    }

}


internal object RegexSerializer : KSerializer<Regex> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.text.Regex", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Regex {
        val pattern = decoder.decodeString()
        return Regex(pattern, setOf(RegexOption.MULTILINE))
    }

    override fun serialize(encoder: Encoder, value: Regex) {
        encoder.encodeString(value.pattern)
    }
}
