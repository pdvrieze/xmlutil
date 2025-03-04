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

package net.devrieze.serialization.examples.webdav

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

fun main() {
    val xml = XML { recommended_0_90_2() }

    val p = WebDavMultiStatus::class.java.getResourceAsStream("/net/devrieze/serialization/examples/webdav/multiresponse.xml").use { s ->
        xml.decodeFromReader(WebDavMultiStatus.serializer(), xmlStreaming.newReader(s))
    }

    println(p)
}


private const val PREFIX_WILDCARD = ""

@Serializable
@XmlSerialName("multistatus", "DAV:", PREFIX_WILDCARD)
data class WebDavMultiStatus(

    @XmlElement
    @XmlSerialName("response", "DAV:", PREFIX_WILDCARD)
    val responses: List<WebDavResponse>
)

@Serializable
@XmlSerialName("response", "DAV:", PREFIX_WILDCARD)
data class WebDavResponse(

    @XmlElement
    @XmlSerialName("href", "DAV:", PREFIX_WILDCARD)
    val href: String,

    @XmlElement
    @XmlSerialName("propstat", "DAV:", PREFIX_WILDCARD)
    val propstat: List<WebDavPropStat>
)

@Serializable
@XmlSerialName("propstat", "DAV:", PREFIX_WILDCARD)
data class WebDavPropStat(

    @XmlElement
    @XmlSerialName("prop", "DAV:", PREFIX_WILDCARD)
    val prop: WebDavProp,

    @XmlElement
    @XmlSerialName("status", "DAV:", PREFIX_WILDCARD)
    val status: String

) {

    fun isDirectory() =
        prop.resourceType?.collection == ""
}

@Serializable
@XmlSerialName("prop", "DAV:", PREFIX_WILDCARD)
data class WebDavProp(

    @XmlElement
    @XmlSerialName("getlastmodified", "DAV:", PREFIX_WILDCARD)
    val lastModified: String? = null,

    @XmlElement
    @XmlSerialName("getcontentlength", "DAV:", PREFIX_WILDCARD)
    @Serializable(LongOrNullSerializer::class)
    val contentLength: Long? = null,

    @XmlElement
    @XmlSerialName("resourcetype", "DAV:", PREFIX_WILDCARD)
    val resourceType: WebDavResourceType? = null,

    @XmlElement
    @XmlSerialName("getcontenttype", "DAV:", PREFIX_WILDCARD)
    val contentType: String? = null,

    @XmlElement
    @XmlSerialName("quota-used-bytes", "DAV:", PREFIX_WILDCARD)
    @Serializable(LongOrNullSerializer::class)
    val quotaUsedBytes: Long? = null,

    @XmlElement
    @XmlSerialName("quota-available-bytes", "DAV:", PREFIX_WILDCARD)
    @Serializable(LongOrNullSerializer::class)
    val quotaAvailableBytes: Long? = null,

    @XmlElement
    @XmlSerialName("getetag", "DAV:", PREFIX_WILDCARD)
    val getETag: String? = null

)

@Serializable
@XmlSerialName("resourcetype", "DAV:", PREFIX_WILDCARD)
data class WebDavResourceType(

    @XmlElement
    @XmlSerialName("collection", "DAV:", PREFIX_WILDCARD)
    val collection: String? = null
)

object LongOrNullSerializer: XmlSerializer<Long?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("longOrNull", PrimitiveKind.LONG).nullable
            .xml(PrimitiveSerialDescriptor("longOrNull", PrimitiveKind.STRING).nullable)

    override fun serialize(encoder: Encoder, value: Long?) {
        when (value) {
            null -> encoder.encodeNull()
            else -> {
                encoder.encodeNotNullMark()
                encoder.encodeLong(value)
            }
        }
    }

    override fun deserialize(decoder: Decoder): Long? = when {
        decoder.decodeNotNullMark() -> decoder.decodeLong()
        else -> decoder.decodeNull()
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: Long?,
        isValueChild: Boolean
    ): Long? {
        if (!decoder.decodeNotNullMark()) return decoder.decodeNull()
        return when (val s = xmlCollapseWhitespace(decoder.decodeString())) {
            "" -> null
            else -> s.toLong()
        }
    }

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: Long?, isValueChild: Boolean) {
        encoder.encodeString(value?.toString() ?: "")
    }
}
