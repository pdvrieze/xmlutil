/*
 * Copyright (c) 2024-2026.
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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import io.github.pdvrieze.formats.xmlschemaTests.Resource
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.newReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.xmlStreaming
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI
import java.net.URL

class SimpleResolver private constructor(internal val xml: XML, private val baseLocation: Reference, val isNetworkResolvingAllowed: Boolean = false) :
    ResolvedSchema.Resolver {

    constructor(xml: XML, baseURI: URI, isNetworkResolvingAllowed: Boolean = false):
            this(xml, Reference.Remote(baseURI), isNetworkResolvingAllowed)

    constructor(xml: XML, baseUrl: Resource, isNetworkResolvingAllowed: Boolean = false) :
            this(xml, Reference.Local(baseUrl), isNetworkResolvingAllowed)

    constructor(baseURI: URI, isNetworkResolvingAllowed: Boolean = false) : this(
        XML.v1 {
            policy {
                throwOnRepeatedElement = true
                verifyElementOrder = true
            }
        },
        Reference.Remote(baseURI),
        isNetworkResolvingAllowed
    )

    constructor(resource: Resource, isNetworkResolvingAllowed: Boolean = false) : this(
        XML.v1 {
            policy {
                throwOnRepeatedElement = true
                verifyElementOrder = true
            }
        },
        Reference.Local(resource),
        isNetworkResolvingAllowed
    )

    constructor(baseURI: URL, isNetworkResolvingAllowed: Boolean = false) :
            this(baseURI.toURI(), isNetworkResolvingAllowed)

    init {
        if (baseLocation is Reference.Remote) {
            require(baseLocation.uri.isAbsolute) {
                "URI ${baseLocation.uri} is not absolute"
            }

        }
    }

    override val baseUri: VAnyURI
        get() = when (baseLocation) {
            is Reference.Local -> "local:${baseLocation.resource.path}"
            is Reference.Remote -> baseLocation.uri.toASCIIString()
        }.toAnyUri()

    override fun readSchema(schemaLocation: VAnyURI): XSSchema {
        val schemaUri = URI(schemaLocation.value)
        if (!isNetworkResolvingAllowed &&
            schemaUri.isAbsolute && (
                    baseLocation is Reference.Local ||
                            (baseLocation is Reference.Remote &&
                                    (schemaUri.scheme != baseLocation.uri.scheme ||
                                            schemaUri.host != baseLocation.uri.host)))
        ) {
            when (schemaLocation.value) {
                "http://www.w3.org/XML/2008/06/xlink.xsd" -> return baseLocation.resolve("/xlink.xsd").withXmlReader { reader ->
                    xml.decodeFromReader<XSSchema>(reader)
                }
                else -> throw FileNotFoundException("Absolute uri references are not supported ${schemaLocation}")
            }
        }
        return baseLocation.resolve(schemaUri).withXmlReader { reader ->
            xml.decodeFromReader<XSSchema>(reader)
        }
    }

    override fun tryReadSchema(schemaLocation: VAnyURI): XSSchema? {
        val schemaUri = URI(schemaLocation.value)
        if (!isNetworkResolvingAllowed &&
            schemaUri.isAbsolute &&
            (baseLocation is Reference.Local || ( baseLocation is Reference.Remote && (schemaUri.scheme != baseLocation.uri.scheme ||
                    schemaUri.host != baseLocation.uri.host)))
        ) {
            if (schemaUri.scheme == "file") throw FileNotFoundException("Absolute file uri references are not supported")
            return when (schemaLocation.value) {
                "http://www.w3.org/XML/2008/06/xlink.xsd" ->
                    javaClass.classLoader.getResourceAsStream("xlink.xsd")!!
                        .withXmlReader { reader ->
                            xml.decodeFromReader<XSSchema>(reader)
                        }

                else -> null
            }
        }

        return baseLocation.resolve(schemaUri).withXmlReader { reader ->
            xml.decodeFromReader<XSSchema>(reader)
        }
    }

    override fun delegate(schemaLocation: VAnyURI): ResolvedSchema.Resolver {
        return SimpleResolver(xml, baseLocation.resolve(schemaLocation.value))
    }

    override fun resolve(relativeUri: VAnyURI): VAnyURI {
        return URI(baseUri.value).resolve2(relativeUri.toString()).toASCIIString().toAnyUri()
    }

    internal sealed class Reference {
        class Local(val resource: Resource) : Reference() {
            override fun resolve(other: String): Local {
                return Local(resource.resolve(other))
            }

            override fun resolve(other: URI): Reference {
                return resolve(other.toString())
            }
        }
        class Remote(val uri: URI) : Reference() {
            override fun resolve(other: String): Remote {
                return Remote(uri.resolve2(other))
            }

            override fun resolve(other: URI): Reference {
                return Remote(other.resolve2(other))
            }
        }

        abstract fun resolve(other: String): Reference
        abstract fun resolve(other: URI): Reference

        fun <R> withXmlReader(body: (XmlReader) -> R): R {
            when (this) {
                is Local -> return resource.withXmlReader(body = body)
                is Remote -> return uri.toURL().withXmlReader(body)
            }
        }
    }
}

@Suppress("DEPRECATION")
internal fun URI.resolve2(other: URI): URI = when {
    other.isAbsolute -> other
    else -> URL(toURL(), other.toASCIIString()).toURI()
}

internal fun URI.resolve2(other: String): URI = resolve2(URI.create(other))

internal fun Resource.resolve2(other: String): Resource = resolve(other)

private inline fun <R> URI.withXmlReader(body: (XmlReader) -> R): R {
    return toURL().withXmlReader(body)
}

private inline fun <R> URL.withXmlReader(body: (XmlReader) -> R): R {
    return openStream().use { it.withXmlReader(body) }
}

private inline fun <R> InputStream.withXmlReader(body: (XmlReader) -> R): R {
    return use { inStream ->
        val reader = xmlStreaming.newReader(inStream)
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
