/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil

import kotlinx.serialization.*
import nl.adaptivity.xmlutil.serialization.readBegin
import nl.adaptivity.xmlutil.serialization.readElements
import nl.adaptivity.xmlutil.serialization.simpleSerialClassDesc
import nl.adaptivity.xmlutil.serialization.writeStructure

//@Serializable
interface Namespace {

    /**
     * Gets the prefix, returns "" if this is a default
     * namespace declaration.
     */
    val prefix: String

    operator fun component1() = prefix

    /**
     * Gets the uri bound to the prefix of this namespace
     */
    val namespaceURI: String
    operator fun component2() = namespaceURI

    @Serializer(forClass = Namespace::class)
    companion object: KSerializer<Namespace> {
        override val descriptor: SerialDescriptor
            = simpleSerialClassDesc<Namespace>("prefix", "namespaceURI")

        override fun deserialize(decoder: Decoder): Namespace {
            lateinit var prefix: String
            lateinit var namespaceUri: String
            decoder.readBegin(descriptor) { desc ->
                readElements(this) {
                    when (it) {
                        0 -> prefix = decodeStringElement(desc, it)
                        1 -> namespaceUri = decodeStringElement(desc, it)
                    }
                }
            }
            return XmlEvent.NamespaceImpl(prefix, namespaceUri)
        }

        override fun serialize(encoder: Encoder, obj : Namespace) {
            encoder.writeStructure(descriptor) {
                encodeStringElement(descriptor, 0, obj.prefix)
                encodeStringElement(descriptor, 1, obj.namespaceURI)
            }
        }

    }
}

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use the property version", ReplaceWith("this.prefix"))
inline fun Namespace.getPrefix() = prefix

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use the property version", ReplaceWith("this.namespaceURI"))
inline fun Namespace.getNamespaceURI() = namespaceURI
