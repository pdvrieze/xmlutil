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

package io.github.pdvrieze.formats.xpath

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@Serializable(XPathExpression.Serializer::class)
class XPathExpression(
    val test: String,
    private val rooted: Boolean,
    private val steps: List<Step>
) {

    class Predicate

    class Step(
        val axis: Axis,
        val test: QName,
        val predicates: List<Predicate>
    )

    sealed class NodeTest {
        class NameTest(): NodeTest()
        class NodeTypeTest(): NodeTest()
        class ProcessingInstructionTest(): NodeTest()
    }

    companion object Serializer: KSerializer<XPathExpression> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.XPathExpression",
            PrimitiveKind.STRING
        )

        override fun serialize(encoder: Encoder, value: XPathExpression) {
            if (encoder is XML.XmlOutput) {
                // todo ensure prefixes exist encoder.target
            }
            return encoder.encodeString(value.test) // TODO use xml aware writing
        }

        override fun deserialize(decoder: Decoder): XPathExpression {
            val nsContext = (decoder as? XML.XmlInput)?.input?.namespaceContext ?: SimpleNamespaceContext()
            return invoke(xmlCollapseWhitespace(decoder.decodeString()), nsContext)
        }

        operator fun invoke(path: String, namespaceContext: NamespaceContext = SimpleNamespaceContext() ): XPathExpression {
            var rooted = false
            val steps = mutableListOf<Step>()
            val s = path
            var i = 0
            var start = 0
            var currentAxis: Axis? = null
            var currentTest: NodeTest? = null
            var currentPrefix: String? = null
            while (i<s.length) {
                when(val c = s.get(i)) {
                    '/' -> when (i) {
                        0 -> rooted = true
                        else -> {
                            //TODO("Add a new step")
                        }
                    }

                    ':' -> when {
                        i + 1 < s.length && s.get(i + 1) == ':' -> {
                            require(currentAxis == null) { "Multiple axes in xpath" }
                            currentAxis = Axis.from(s.substring(start, i))
                            ++i // skip extra character
                            start = i + 1
                        }
                        else -> {
                            require(currentPrefix == null) { "QName can only have 1 prefix" }
                            currentPrefix = s.substring(start, i)
                            start = i+1
                        }
                    }

                    '[' -> {

                    }
                }
                ++i
            }

            TODO("not implemented")

        }

    }



    enum class Axis(val repr: String) {
        CHILD("child"),
        DESCENDANT("descendant"),
        PARENT("parent"),
        ANCESTOR("ancestor"),
        FOLLOWING_SIBLING("following-sibling"),
        PRECEDING_SIBLING("preceding-sibling"),
        FOLLOWING("following"),
        PRECEDING("preceding"),
        ATTRIBUTE("attribute"),
        NAMESPACE("namespace"),
        SELF("self"),
        DESCENDANT_OR_SELF("descendant-or-self"),
        ANCESTOR_OR_SELF("ancestor-or-self"),
        ;

        companion object {
            private val lookup = Axis.values().associateBy { it.repr }

            fun from(value: String): Axis {
                return requireNotNull(lookup[value]) { "$value is not a valid path axis" }
            }
        }


    }
}
