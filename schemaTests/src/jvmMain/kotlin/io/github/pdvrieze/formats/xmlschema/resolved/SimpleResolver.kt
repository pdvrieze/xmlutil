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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI
import java.net.URL

class SimpleResolver(internal val xml: XML, private val baseURI: URI, val isNetworkResolvingAllowed: Boolean = false) :
    ResolvedSchema.Resolver {

        constructor(xml: XML, baseUrl: URL, isNetworkResolvingAllowed: Boolean = false) :
                this(xml, baseUrl.toURI(), isNetworkResolvingAllowed)

    constructor(baseURI: URI, isNetworkResolvingAllowed: Boolean = false) : this(
        XML {
            defaultPolicy {
                autoPolymorphic = true
                throwOnRepeatedElement = true
                verifyElementOrder = true
                isStrictAttributeNames = true
            }
        },
        baseURI,
        isNetworkResolvingAllowed
    )

    constructor(baseURI: URL, isNetworkResolvingAllowed: Boolean = false) :
            this(baseURI.toURI(), isNetworkResolvingAllowed)

    init {
        require(baseURI.isAbsolute) {
            "URI ${baseURI} is not absolute"
        }
    }

    override val baseUri: VAnyURI
        get() = baseURI.toASCIIString().toAnyUri()

    override fun readSchema(schemaLocation: VAnyURI): XSSchema {
        val schemaUri = URI(schemaLocation.value)
        if (!isNetworkResolvingAllowed &&
            schemaUri.isAbsolute &&
            (schemaUri.scheme != baseURI.scheme ||
                    schemaUri.host != baseURI.host)
        ) {
            when (schemaLocation.value) {
                "http://www.w3.org/XML/2008/06/xlink.xsd" -> return baseURI.resolve2("/xlink.xsd").withXmlReader { reader ->
                    xml.decodeFromReader<XSSchema>(reader)
                }
                else -> throw FileNotFoundException("Absolute uri references are not supported ${schemaLocation}")
            }
        }
        return baseURI.resolve2(schemaUri).withXmlReader { reader ->
            xml.decodeFromReader<XSSchema>(reader)
        }
    }

    override fun tryReadSchema(schemaLocation: VAnyURI): XSSchema? {
        val schemaUri = URI(schemaLocation.value)
        if (!isNetworkResolvingAllowed &&
            schemaUri.isAbsolute &&
            (schemaUri.scheme != baseURI.scheme ||
                    schemaUri.host != baseURI.host)
        ) {
            if (schemaUri.scheme == "file") throw FileNotFoundException("Absolute file uri references are not supported")
            when (schemaLocation.value) {
                "http://www.w3.org/XML/2008/06/xlink.xsd" -> return javaClass.classLoader.getResourceAsStream("xlink.xsd").withXmlReader { reader ->
                    xml.decodeFromReader<XSSchema>(reader)
                }
                else -> return null
            }
        }
        val stream = try {
            baseURI.resolve2(schemaUri).toURL().openStream()
        } catch (e: FileNotFoundException) {
            return null
        }

        return stream.withXmlReader { reader ->
            xml.decodeFromReader<XSSchema>(reader)
        }
    }

    override fun delegate(schemaLocation: VAnyURI): ResolvedSchema.Resolver {
        return SimpleResolver(xml, baseURI.resolve2(schemaLocation.value))
    }

    override fun resolve(relativeUri: VAnyURI): VAnyURI {
        return baseURI.resolve2(relativeUri.xmlString).toASCIIString().toAnyUri()
    }
}

internal fun URI.resolve2(other: URI): URI = when {
    other.isAbsolute -> other
    else -> URL(toURL(), other.toASCIIString()).toURI()
}

internal fun URI.resolve2(other: String): URI = resolve2(URI.create(other))

private inline fun <R> URI.withXmlReader(body: (XmlReader) -> R): R {
    return toURL().withXmlReader(body)
}

private inline fun <R> URL.withXmlReader(body: (XmlReader) -> R): R {
    return openStream().use { it.withXmlReader(body) }
}

private inline fun <R> InputStream.withXmlReader(body: (XmlReader) -> R): R {
    return use { inStream ->
        val reader = KtXmlReader(inStream)
        val r = reader.use(body)
        if (reader.eventType != EventType.END_DOCUMENT) {
            var e: EventType
            do {
                e = reader.next()
            } while (e.isIgnorable && e != EventType.END_DOCUMENT)
            require(e == EventType.END_DOCUMENT) {
                "Trailing content in document $reader"
            }
        }
        r
    }
}
