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

package org.w3.qt3tests

import io.github.pdvrieze.formats.xpath.XPathExpression
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.name
import nl.adaptivity.xmlutil.serialization.XML
import org.w3.qt3tests.context.AssertionResolutionContext

@Serializable(UnresolvedXPathExpr.Companion::class)
interface UnresolvedXPathExpr: UnresolvedXQueryExpr {
    context(ctx: AssertionResolutionContext)
    override fun resolve(): Result<XPathExpression>

    companion object: KSerializer<UnresolvedXPathExpr> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor(UnresolvedXPathExpr::class.name, PrimitiveKind.STRING)

        override fun serialize(
            encoder: Encoder,
            value: UnresolvedXPathExpr
        ) {
            encoder.encodeString(value.expr)
        }

        override fun deserialize(decoder: Decoder): UnresolvedXPathExpr {
            val locationInfo = (decoder as? XML.XmlInput)?.input?.extLocationInfo
            return UnresolvedXPathExprImpl(decoder.decodeString(), locationInfo)
        }
    }

}

class UnresolvedXPathExprImpl(expr: String, locationInfo: XmlReader.LocationInfo?): UnresolvedXQueryExprImpl(expr, locationInfo), UnresolvedXPathExpr {

    context(ctx: AssertionResolutionContext)
    override fun resolve(): Result<XPathExpression> {
        return runCatching { XPathExpression(expr, ctx.namespaceContext, ctx.version, locationInfo) }
    }

}
