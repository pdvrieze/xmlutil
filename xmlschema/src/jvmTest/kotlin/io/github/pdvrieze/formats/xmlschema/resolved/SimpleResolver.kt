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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.xmlStreaming
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.net.URL

internal class SimpleResolver(private val baseURI: URI, val isNetworkResolvingAllowed: Boolean = false) :
    ResolvedSchema.Resolver {
    private val xml = XML {
        defaultPolicy {
            autoPolymorphic = true
            throwOnRepeatedElement = true
            verifyElementOrder = true
            isStrictAttributeNames = true
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
                "http://www.w3.org/XML/2008/06/xlink.xsd" -> return baseURI.resolve("/xlink.xsd").withXmlReader { reader ->
                    xml.decodeFromReader<XSSchema>(reader)
                }
                else -> throw FileNotFoundException("Absolute uri references are not supported ${schemaLocation}")
            }
        }
        return baseURI.resolve(schemaUri).withXmlReader { reader ->
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
            baseURI.resolve(schemaUri).toURL().openStream()
        } catch (e: FileNotFoundException) {
            return null
        }

        return stream.withXmlReader { reader ->
            xml.decodeFromReader<XSSchema>(reader)
        }
    }

    override fun delegate(schemaLocation: VAnyURI): ResolvedSchema.Resolver {
        return SimpleResolver(baseURI.resolve(schemaLocation.value))
    }

    override fun resolve(relativeUri: VAnyURI): VAnyURI {
        return baseURI.resolve(relativeUri.xmlString).toASCIIString().toAnyUri()
    }
}

private inline fun <R> URI.withXmlReader(body: (XmlReader) -> R): R {
    return toURL().withXmlReader(body)
}

private inline fun <R> URL.withXmlReader(body: (XmlReader) -> R): R {
    return openStream().withXmlReader(body)
}

private inline fun <R> InputStream.withXmlReader(body: (XmlReader) -> R): R {
    val stream = when {
        markSupported() -> this
        else -> BufferedInputStream(this)
    }
    return stream.use { inStream ->
        inStream.mark(4)
        val c = inStream.read()
        val charset: String = when {
            c == 0xef && read() == 0xbb && read() == 0xbf -> "UTF-8"

            c == 0xfe && read() == 0xff -> "UTF-16BE"

            c == 0xff && read() == 0xfe -> "UTF-16LE"

            else -> {
                inStream.reset()
                "UTF-8"
            }
        }

        val reader = xmlStreaming.newGenericReader(InputStreamReader(inStream, charset))
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
