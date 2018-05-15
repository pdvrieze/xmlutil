/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import kotlinx.serialization.*
import nl.adaptivity.xml.serialization.readBegin
import nl.adaptivity.xml.serialization.readElements
import nl.adaptivity.xml.serialization.simpleSerialClassDesc

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
        override val serialClassDesc: KSerialClassDesc
            = simpleSerialClassDesc<Namespace>("prefix", "namespaceURI")

        override fun load(input: KInput): Namespace {
            lateinit var prefix: String
            lateinit var namespaceUri: String
            input.readBegin(serialClassDesc) { desc ->
                readElements(this) {
                    when (it) {
                        0 -> prefix = readStringElementValue(desc, it)
                        1 -> namespaceUri = readStringElementValue(desc, it)
                    }
                }
            }
            return XmlEvent.NamespaceImpl(prefix, namespaceUri)
        }

        override fun save(output: KOutput, obj: Namespace) {
            val childOut = output.writeBegin(serialClassDesc)
            childOut.writeStringElementValue(serialClassDesc, 0, obj.prefix)
            childOut.writeStringElementValue(serialClassDesc, 1, obj.namespaceURI)
            childOut.writeEnd(serialClassDesc)
        }

    }
}

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use the property version", ReplaceWith("this.prefix"))
inline fun Namespace.getPrefix() = prefix

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use the property version", ReplaceWith("this.namespaceURI"))
inline fun Namespace.getNamespaceURI() = namespaceURI
