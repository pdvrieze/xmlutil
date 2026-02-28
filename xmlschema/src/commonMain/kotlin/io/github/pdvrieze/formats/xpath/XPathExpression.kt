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

package io.github.pdvrieze.formats.xpath

import io.github.pdvrieze.formats.xpath.impl.Expr
import io.github.pdvrieze.formats.xpath.impl.OutputContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML

interface XPathExpression: XQueryExpression {
    override val xmlString: String
    @OptIn(XPathInternal::class)
    val expr: Expr
    override val version: XPathVersion

    companion object Serializer : KSerializer<XPathExpression> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.XPathExpression",
            PrimitiveKind.STRING
        )

        @OptIn(XPathInternal::class)
        override fun serialize(encoder: Encoder, value: XPathExpression) {
            if (encoder is XML.XmlOutput) {
                // TODO ensure prefixes exist encoder.target
                val str = buildString {
                    context(OutputContext.Companion(encoder.target)) {
                        value.expr.appendToString(this)
                    }
                }
                encoder.encodeString(str)
            } else {
                encoder.encodeString(value.xmlString) // TODO use xml aware writing
            }
        }

        override fun deserialize(decoder: Decoder): XPathExpression {
            val nsContext: IterableNamespaceContext
            val positionInfo: XmlReader.LocationInfo?
            when (decoder) {
                is XML.XmlInput -> {
                    nsContext = decoder.input.namespaceContext
                    positionInfo = decoder.input.extLocationInfo
                }

                else -> {
                    nsContext = SimpleNamespaceContext()
                    positionInfo = null
                }
            }


            return invoke(
                path = xmlTrimWhitespace(original = decoder.decodeString()),
                namespaceContext = nsContext,
                posInfo = positionInfo
            )
        }

        operator fun invoke(
            path: String,
            namespaceContext: NamespaceContext = SimpleNamespaceContext(),
            ver: XPathVersion = XPathVersion.XPath3_1,
            posInfo: XmlReader.LocationInfo? = null,
        ): XPathExpression {
            val parser = XPathExpressionImpl.Parser(xmlTrimWhitespace(path), namespaceContext, ver, posInfo)
            return XPathExpressionImpl(path, parser.parse(), ver)
        }

    }

}
