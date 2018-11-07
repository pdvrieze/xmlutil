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
import nl.adaptivity.xmlutil.XMLConstants.DEFAULT_NS_PREFIX
import nl.adaptivity.xmlutil.XMLConstants.NULL_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XML_NS_PREFIX
import nl.adaptivity.xmlutil.XMLConstants.XML_NS_URI
import nl.adaptivity.xmlutil.multiplatform.name
import nl.adaptivity.xmlutil.serialization.withName
import kotlin.jvm.JvmName


/**
 * A simple namespace context that stores namespaces in a single array
 * Created by pdvrieze on 24/08/15.
 */
@Serializable
open class SimpleNamespaceContext internal constructor(val buffer: Array<out String>) : IterableNamespaceContext {

    @Transient
    val indices: IntRange get() = 0..(size - 1)

    @Transient
    @get:JvmName("size")
    val size: Int
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

    constructor() : this(emptyArray())

    constructor(prefixMap: Map<out CharSequence, CharSequence>) :
        this(flatten(prefixMap.entries, { key.toString() }, { value.toString() }))

    constructor(prefixes: Array<out CharSequence>, namespaces: Array<out CharSequence>) :
        this(Array(prefixes.size * 2, { (if (it % 2 == 0) prefixes[it / 2] else namespaces[it / 2]).toString() }))

    constructor(prefix: CharSequence, namespace: CharSequence) :
        this(arrayOf(prefix.toString(), namespace.toString()))

    constructor(namespaces: Collection<Namespace>) :
        this(flatten(namespaces, { prefix }, { namespaceURI }))

    constructor(namespaces: Iterable<Namespace>) :
        this(namespaces as? Collection<Namespace> ?: namespaces.toList())

    /**
     * Create a context that combines both. This will "forget" overlapped prefixes.
     */
    @Deprecated("Use operator", ReplaceWith("this + other"))
    fun combine(other: SimpleNamespaceContext): SimpleNamespaceContext {
        return this + other
    }

    operator fun plus(other: SimpleNamespaceContext): SimpleNamespaceContext {
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
    fun combine(other: Iterable<Namespace>): SimpleNamespaceContext? {
        return plus(other)
    }

    operator fun plus(other: Iterable<Namespace>): SimpleNamespaceContext {
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

    override fun getNamespaceURI(prefix: String): String {
        return when (prefix) {
            XML_NS_PREFIX   -> XML_NS_URI
            XMLNS_ATTRIBUTE -> XMLNS_ATTRIBUTE_NS_URI
            else            -> indices.reversed()
                                   .filter { getPrefix(it) == prefix }
                                   .map { getNamespaceURI(it) }
                                   .firstOrNull() ?: NULL_NS_URI
        }
    }

    override fun getPrefix(namespaceURI: String) = getPrefixSequence(namespaceURI).firstOrNull()

    fun getPrefixSequence(namespaceURI: String): Sequence<String> {
        return when (namespaceURI) {
            XML_NS_URI             -> sequenceOf(XML_NS_PREFIX)
            NULL_NS_URI            -> sequenceOf(DEFAULT_NS_PREFIX)
            XMLNS_ATTRIBUTE_NS_URI -> sequenceOf(XMLNS_ATTRIBUTE)
            else                   -> {
                indices.reversed().asSequence()
                    .filter { getNamespaceURI(it) == namespaceURI }
                    .map { getPrefix(it) }
            }
        }
    }

    @Suppress("OverridingDeprecatedMember")
    override fun getPrefixes(namespaceURI: String): Iterator<String> = getPrefixSequence(namespaceURI).iterator()

    fun getPrefix(index: Int): String {
        try {
            return buffer[index * 2]
        } catch (e: IndexOutOfBoundsException) {
            throw IndexOutOfBoundsException("Index out of range: $index")
        }
    }

    fun getNamespaceURI(index: Int): String {
        try {
            return buffer[index * 2 + 1]
        } catch (e: IndexOutOfBoundsException) {
            throw IndexOutOfBoundsException("Index out of range: $index")
        }
    }

    override fun iterator(): Iterator<Namespace> {
        return SimpleIterator()
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

    @Serializer(forClass = SimpleNamespaceContext::class)
    companion object: KSerializer<SimpleNamespaceContext> {

        private val actualSerializer = Namespace.list

        override val descriptor: SerialDescriptor = actualSerializer.descriptor.
                withName(SimpleNamespaceContext::class.name)

        fun from(originalNSContext: Iterable<Namespace>): SimpleNamespaceContext = when (originalNSContext) {
            is SimpleNamespaceContext -> originalNSContext
            else                                            -> SimpleNamespaceContext(
                originalNSContext)
        }

        override fun deserialize(input: Decoder): SimpleNamespaceContext {
            return SimpleNamespaceContext(
                actualSerializer.deserialize(input))
        }

        override fun serialize(output: Encoder, obj: SimpleNamespaceContext) {
            actualSerializer.serialize(output, obj.toList())
        }

    }
}


private inline fun <T> flatten(namespaces: Collection<T>,
                               crossinline prefix: T.() -> String,
                               crossinline namespace: T.() -> String): Array<String> {
    val filler: Iterator<String> = namespaces.asSequence().flatMap {
        sequenceOf(it.prefix(), it.namespace())
    }.iterator()
    return Array(namespaces.size * 2, { filler.next() })
}
