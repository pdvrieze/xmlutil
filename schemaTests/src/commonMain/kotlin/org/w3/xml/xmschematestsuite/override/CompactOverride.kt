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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.serialization.XmlDefault
import nl.adaptivity.xmlutil.serialization.XmlNamespaceDeclSpecs
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.w3.xml.xmschematestsuite.TSExpected

@Serializable
@XmlSerialName("overrides", namespace = "http://pdvrieze.github.io/xmlutil/testoverride", "")
@XmlNamespaceDeclSpecs("ts=http://www.w3.org/XML/2004/xml-schema-test-suite/")
class CompactOverride(
    @XmlSerialName("ignore", namespace = "http://pdvrieze.github.io/xmlutil/testoverride", "")
    val ignores: List<TestPath>,
    val overrides: List<ExpectedOverride>,
) {
    constructor(ignores: OTSSuite) : this(
        ignores.findIgnoredPaths(),
        ignores.findOverrides()
    )
}

@Serializable
@XmlSerialName("test", namespace = "http://pdvrieze.github.io/xmlutil/testoverride", "")
data class ExpectedOverride(val path: TestPath, val expecteds: List<OTSExpected>, @XmlDefault("false") val isInstance: Boolean = false) {
    constructor(path: TestPath, expecteds: List<TSExpected>) : this(path, expecteds.map { it.copy() }, !path.isSchema)
}

@Serializable(TestPath.Companion::class)
data class TestPath(
    val testSet: String,
    val group: String? = null,
    val test: String? = null,
    val isSchema: Boolean = true
) : Comparable<TestPath> {

    init {
        require(test == null || group != null) { "Group null, but test not ($test)" }
    }

    override fun compareTo(other: TestPath): Int {
        testSet.compareTo(other.testSet).let { if (it != 0) return it }

        when {
            group == null -> return if (other.group == null) 0 else -1
            other.group == null -> return 1
            else -> group.compareTo(other.group).let { if (it != 0) return it }
        }

        return when {
            test == null -> if (other.test == null) 0 else -1
            other.test == null -> 1
            else -> test.compareTo(other.test)
        }

    }

    companion object : KSerializer<TestPath> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("org.w3.xml.xmschematestsuite.override.TestPath", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): TestPath {
            val decoded = decoder.decodeString().split('/')
            return TestPath(
                decoded[0],
                if (decoded.size > 1) decoded[1].takeIf { decoded.size > 2 || it.isNotEmpty() } else null,
                if (decoded.size > 2) decoded[2].takeIf { it.isNotEmpty() } else null,
            )
        }

        override fun serialize(encoder: Encoder, value: TestPath) {
            val s = listOfNotNull(value.testSet, value.group, value.test).joinToString("/")
            encoder.encodeString(s)
        }
    }
}
