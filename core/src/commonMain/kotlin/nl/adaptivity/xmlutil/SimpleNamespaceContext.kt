/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XMLConstants.DEFAULT_NS_PREFIX
import nl.adaptivity.xmlutil.XMLConstants.NULL_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XML_NS_PREFIX
import nl.adaptivity.xmlutil.XMLConstants.XML_NS_URI
import nl.adaptivity.xmlutil.core.impl.multiplatform.name
import kotlin.collections.set
import kotlin.jvm.JvmName


/**
 * A simple namespace context that stores namespaces in a single array
 * Created by pdvrieze on 24/08/15.
 */
@Serializable(SimpleNamespaceContext.Companion::class)
@XmlUtilInternal
public open class SimpleNamespaceContext internal constructor(public val buffer: Array<out String>) :
    IterableNamespaceContext {

    public val indices: IntRange get() = 0 until size

    @get:JvmName("size")
    public val size: Int
        get() = buffer.size / 2

    private inner class SimpleIterator : Iterator<Namespace> {
        private var pos = 0

        override fun hasNext() = pos < size

        override fun next(): Namespace = SimpleNamespace(pos++)
    }

    private inner class SimpleNamespace(private val pos: Int) : Namespace {

        override val prefix: String
            get() = getPrefix(pos)

        override val namespaceURI: String
            get() = getNamespaceURI(pos)

        override fun hashCode(): Int {
            return prefix.hashCode() * 31 + namespaceURI.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Namespace) return false

            return prefix == other.prefix && namespaceURI == other.namespaceURI
        }

        override fun toString(): String {
            return "{$prefix:$namespaceURI}"
        }
    }

    public constructor() : this(emptyArray())

    public constructor(prefixMap: Map<out CharSequence, CharSequence>) :
            this(flatten(prefixMap.entries, { key.toString() }, { value.toString() }))

    public constructor(prefixes: Array<out CharSequence>, namespaces: Array<out CharSequence>) :
            this(Array(prefixes.size * 2) { (if (it % 2 == 0) prefixes[it / 2] else namespaces[it / 2]).toString() })

    public constructor(prefix: CharSequence, namespace: CharSequence) :
            this(arrayOf(prefix.toString(), namespace.toString()))

    public constructor(namespaces: Collection<Namespace>) :
            this(flatten(namespaces, { prefix }, { namespaceURI }))

    public constructor(namespaces: Iterable<Namespace>) :
            this(namespaces as? Collection<Namespace> ?: namespaces.toList())

    public constructor(original: SimpleNamespaceContext) : this(original.buffer)

    override fun freeze(): SimpleNamespaceContext = this

    /**
     * Create a context that combines both. This will "forget" overlapped prefixes.
     */
    @Deprecated("Use operator", ReplaceWith("this + other"))
    public fun combine(other: SimpleNamespaceContext): SimpleNamespaceContext {
        return this + other
    }

    public operator fun plus(other: SimpleNamespaceContext): SimpleNamespaceContext {
        val result = mutableMapOf<String, String>()
        for (i in indices.reversed()) {
            result[getPrefix(i)] = getNamespaceURI(i)
        }
        for (i in other.indices.reversed()) {
            result[other.getPrefix(i)] = other.getNamespaceURI(i)
        }
        return SimpleNamespaceContext(result)
    }

    /**
     * Combine this context with the additional context. The prefixes already in this context prevail over the added ones.
     * @param other The namespaces to add
     *
     * @return the new context
     */
    public fun combine(other: Iterable<Namespace>): SimpleNamespaceContext {
        return plus(other)
    }

    public operator fun plus(other: Iterable<Namespace>): SimpleNamespaceContext {
        if (size == 0) {
            return from(other)
        }
        if (other is SimpleNamespaceContext) {
            return this + other
        } else if (!other.iterator().hasNext()) {
            return this
        }
        val result = mutableMapOf<String, String>()
        for (i in indices.reversed()) {
            result[getPrefix(i)] = getNamespaceURI(i)
        }
        for (ns in other) {
            result[ns.prefix] = ns.namespaceURI
        }
        return SimpleNamespaceContext(result)
    }

    override fun getNamespaceURI(prefix: String): String? {
        return when (prefix) {
            XML_NS_PREFIX -> XML_NS_URI
            XMLNS_ATTRIBUTE -> XMLNS_ATTRIBUTE_NS_URI
            else -> indices.reversed()
                .filter { getPrefix(it) == prefix }
                .firstOrNull()
                ?.let { getNamespaceURI(it) }
        }
    }

    override fun getPrefix(namespaceURI: String): String? = getPrefixSequence(namespaceURI).firstOrNull()

    /**
     * Get all prefixes for this particular namespace in the namespace context.
     */
    public fun getPrefixSequence(namespaceURI: String): Sequence<String> {
        return when (namespaceURI) {
            XML_NS_URI -> sequenceOf(XML_NS_PREFIX)
            NULL_NS_URI -> sequenceOf(DEFAULT_NS_PREFIX)
            XMLNS_ATTRIBUTE_NS_URI -> sequenceOf(XMLNS_ATTRIBUTE)
            else -> {
                indices.reversed().asSequence()
                    .filter { getNamespaceURI(it) == namespaceURI }
                    .map { getPrefix(it) }
            }
        }
    }

    @Suppress("OverridingDeprecatedMember")
    override fun getPrefixesCompat(namespaceURI: String): Iterator<String> = getPrefixSequence(namespaceURI).iterator()

    public fun getPrefix(index: Int): String {
        try {
            return buffer[index * 2]
        } catch (e: IndexOutOfBoundsException) {
            throw IndexOutOfBoundsException("Index out of range: $index")
        }
    }

    public fun getNamespaceURI(index: Int): String {
        try {
            return buffer[index * 2 + 1]
        } catch (e: IndexOutOfBoundsException) {
            throw IndexOutOfBoundsException("Index out of range: $index")
        }
    }

    override fun iterator(): Iterator<Namespace> {
        return SimpleIterator()
    }

    override fun plus(secondary: IterableNamespaceContext): IterableNamespaceContext = when {
        secondary is SimpleNamespaceContext &&
                secondary.size == 0 -> this

        secondary is SimpleNamespaceContext &&
                size == 0 -> secondary

        else -> super.plus(secondary)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleNamespaceContext) return false

        if (!buffer.contentEquals(other.buffer)) return false

        return true
    }

    override fun hashCode(): Int {
        return buffer.contentHashCode()
    }

    @ExperimentalSerializationApi
    private class RenameDesc(val delegate: SerialDescriptor, override val serialName: String) :
        SerialDescriptor by delegate

    @XmlUtilInternal
    public companion object : KSerializer<SimpleNamespaceContext> {

        private val actualSerializer = ListSerializer(Namespace)

        @ExperimentalSerializationApi
        override val descriptor: SerialDescriptor =
            RenameDesc(actualSerializer.descriptor, SimpleNamespaceContext::class.name)

        public fun from(originalNSContext: Iterable<Namespace>): SimpleNamespaceContext = when (originalNSContext) {
            is SimpleNamespaceContext -> originalNSContext
            else -> SimpleNamespaceContext(
                originalNSContext
            )
        }

        override fun deserialize(decoder: Decoder): SimpleNamespaceContext {
            return SimpleNamespaceContext(actualSerializer.deserialize(decoder))
        }

        override fun serialize(encoder: Encoder, value: SimpleNamespaceContext) {
            actualSerializer.serialize(encoder, value.toList())
        }

        private inline fun <T> flatten(
            namespaces: Collection<T>,
            crossinline prefix: T.() -> String,
            crossinline namespace: T.() -> String
        ): Array<String> {
            val filler: Iterator<String> = namespaces.asSequence().flatMap {
                sequenceOf(it.prefix(), it.namespace())
            }.iterator()
            return Array(namespaces.size * 2) { filler.next() }
        }

    }
}

