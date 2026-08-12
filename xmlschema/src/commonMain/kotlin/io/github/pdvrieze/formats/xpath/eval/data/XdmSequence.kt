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

package io.github.pdvrieze.formats.xpath.eval.data

import io.github.pdvrieze.formats.xmlschema.types.isContentEqual
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.type.XdmEmptySequenceType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSequenceType
import io.github.pdvrieze.formats.xpath.eval.type.XdmType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.Expr
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.RangeException
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import io.github.pdvrieze.xml.schematypes.values.XsdIntegerProgression
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(XPathInternal::class)
abstract class XdmSequence<out T : XdmSingleValue<T>> internal constructor(
    override val staticType: XdmType = XdmSequenceType.ANYSEQ
) : XdmAtomicOrSequence<T>, List<T> {

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean {
        if (isEmpty()) return false
        val it = iterator()
        val firstElem = it.next()
        if (firstElem is XdmNodeBase<*>) return true
        if (!it.hasNext()) return firstElem.toBoolean()

        throw EvaluationException(
            ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE,
            "Only sequences starting with a node can be cast to boolean"
        )
    }

    override fun isValEqual(expected: XdmValue<*>, collation: Collation?): Boolean {
        val rightIt = expected.iterator()
        for (left in this) {
            if (!rightIt.hasNext()) return false
            if (left != rightIt.next()) return false
        }
        return !rightIt.hasNext()
    }

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(
        other: XdmValue<*>,
        collation: Collation?
    ): Boolean {
        val rightIt = other.iterator()
        for (left in this) {
            if (!rightIt.hasNext()) return false
            if (!left.isDeepEqual(rightIt.next(), collation)) return false
        }
        return !rightIt.hasNext()
    }

    context(ctx: ExprEvalContext)
    override fun evalPredicates(predicates: Iterable<Expr>): XdmValue<*> {
        return flatMap { it.evalPredicates(predicates) }
    }

    fun <R: XdmSingleValue<R>> flatMap(transform: (T) -> XdmValue<R>): XdmValue<R> {
        return build {
            for (e in iterator()) add(transform(e))
        }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun atomizeTo(receiver: XdmSequenceBuilder<XdmAtomic<*>>) {
        for (e in iterator()) {
            e.atomizeTo(receiver)
        }
    }

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean {
        val pending = elements.toHashSet()
        for (e in iterator()) {
            pending.remove(e)
        }
        return pending.isEmpty()
    }

    override fun indexOf(element: @UnsafeVariance T): Int =
        indexOfFirst { it == element }

    override fun isEmpty(): Boolean = ! iterator().hasNext()

    override fun toString(): String {
        return joinToString(prefix = "(", postfix = ")")
    }

    abstract override fun subList(fromIndex: Int, toIndex: Int): XdmSequence<T>

    override fun <R : XdmSingleValue<R>> map(operation: (T) -> R): XdmSequence<R> {
        return Map(this, operation)
    }

    object EMPTY : XdmSequence<Nothing>(staticType = XdmEmptySequenceType),
        XdmAtomicOrEmpty<Nothing>, XdmSingleOrEmpty<Nothing> {
        override val size: Int get() = 0

        override fun get(index: Int): Nothing {
            throw RangeException("Index $index out of sequence range")
        }

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun treatAsNonEmpty(type: XdmTypeTest): Nothing {
            throw EvaluationException("The empty sequence cannot be treated as non-empty")
        }

        override fun isEmpty(): Boolean = true

        override fun contains(element: Nothing): Boolean {
            return false
        }

        override fun containsAll(elements: Collection<Nothing>): Boolean {
            return elements.isEmpty()
        }

        override fun indexOf(element: Nothing): Int = -1

        override fun lastIndexOf(element: Nothing): Int = -1

        override fun listIterator(): ListIterator<Nothing> = emptyList<Nothing>().listIterator()

        override fun listIterator(index: Int): ListIterator<Nothing> {
            if (index != 0) throw IndexOutOfBoundsException()
            return listIterator()
        }

        override fun <R : XdmSingleValue<R>> map(operation: (Nothing) -> R): EMPTY = EMPTY

        override fun subList(fromIndex: Int, toIndex: Int): EMPTY {
            if (fromIndex != 0 || toIndex != 0) throw IndexOutOfBoundsException()
            return this
        }

        override fun toList(): List<Nothing> = emptyList()
    }

    internal class Impl<T : XdmSingleValue<T>>(val elements: List<T>, staticType: XdmType) :
        XdmSequence<T>(staticType) {
        init {
            when (staticType) {
                XdmEmptySequenceType -> check(elements.isEmpty())
                else -> require(elements.size > 1) { "Sequence must have at least two elements. Found: ${elements.size} -> $elements" }
            }
        }

        override val size: Int get() = elements.size

        override fun isEmpty(): Boolean = size == 0

        override fun iterator(): Iterator<T> = elements.iterator()

        override fun get(index: Int): T {
            return elements[index]
        }

        context(ctx: ExprEvalContext)
        override fun treatAsNonEmpty(type: XdmTypeTest): XdmValue<*> {
            require(! isEmpty())
            // TODO actually perform checks
            return Impl(elements,type.toValueType(staticType.single))
        }

        override fun isValEqual(expected: XdmValue<*>, collation: Collation?): Boolean {
            if (expected is Impl<*>) return elements.isContentEqual(expected.elements)
            return super.isValEqual(expected, collation)
        }

        context(ctx: ExprEvalContext)
        override fun isDeepEqual(
            other: XdmValue<*>,
            collation: Collation?
        ): Boolean {
            if (other is Impl<*>) {
                if (size != other.size) return false
                return 0.until(size).all { i -> elements[i].isDeepEqual(other.elements[i], collation) }
            } else {
                return super.isDeepEqual(other, collation)
            }
        }

        override fun containsAll(elements: Collection<T>): Boolean {
            return this.elements.containsAll(elements)
        }

        override fun listIterator(): ListIterator<T> = elements.listIterator()

        override fun listIterator(index: Int): ListIterator<T> = elements.listIterator(index)

        override fun subList(fromIndex: Int, toIndex: Int): XdmSequence<T> =
            Impl(elements.subList(fromIndex, toIndex), staticType)

        override fun lastIndexOf(element: T): Int = elements.lastIndexOf(element)

        override fun toList(): List<T> = elements
    }

    interface XdmSequenceBuilder<in T : XdmSingleValue<@UnsafeVariance T>> {
        fun add(value: T)

        fun addAll(values: Iterable<T>)

        fun addAll(values: Sequence<T>)

        fun add(value: XdmSequence<T>) = when (value) {
            is Impl<T> -> addAll(value.elements)
            else -> for (e in value) add(e)
        }

        fun add(value: XdmValue<T>) = when (value) {
            is XdmSequence<T> -> add(value)
            is XdmSingleValue<T> -> add(value.asT())
        }
    }

    internal class XdmSequenceBuilderImpl<T: XdmSingleValue<T>> : XdmSequenceBuilder<T> {
        private val elements = mutableListOf<XdmValue<T>>()

        override fun add(value: XdmSequence<T>) {
            if (value.isEmpty()) return

            when (elements.size) {
                0 if value is RangeSequence<*> -> elements.add(value)
                1 -> {
                    val firstElem = elements[0]
                    if (firstElem is RangeSequence<*>) elements.run { clear(); addAll(firstElem) }
                    elements.addAll(value)
                }

                else -> elements.addAll(value)
            }
        }

        override fun add(value: T) {
            elements.add(value)
        }

        override fun addAll(values: Iterable<T>) {
            elements.addAll(values)
        }

        override fun addAll(values: Sequence<T>) {
            elements.addAll(values)
        }

        fun build(type: XdmType): XdmValue<T> {
            return when (elements.size) {
                0 -> EMPTY
                1 -> elements[0]

                else -> @Suppress("UNCHECKED_CAST")
                    Impl(elements as List<T>, type)
            }
        }
    }

    companion object {

        @OptIn(ExperimentalContracts::class)
        internal inline fun buildSingle(
            type: XdmType = XdmSequenceType.ANYSEQ,
            builderAction: XdmSequenceBuilder<XdmSingleValue<*>>.() -> Unit
        ): XdmValue<XdmSingleValue<*>> {
            contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }

            return XdmSequenceBuilderImpl<XdmSingleValue<*>>().apply(builderAction).build(type)
        }

        @OptIn(ExperimentalContracts::class)
        internal inline fun <T: XsdAtomic> buildAtomic(
            type: XdmType = XdmSequenceType.ANYSEQ,
            builderAction: XdmSequenceBuilder<XdmAtomic<T>>.() -> Unit
        ): XdmAtomicOrSequence<XdmAtomic<T>> {
            contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }

            return XdmSequenceBuilderImpl<XdmAtomic<T>>().apply(builderAction).build(type) as XdmAtomicOrSequence<XdmAtomic<T>>
        }

        @OptIn(ExperimentalContracts::class)
        internal inline fun <T: XdmSingleValue<T>> build(
            type: XdmType = XdmSequenceType.ANYSEQ,
            builderAction: XdmSequenceBuilder<T>.() -> Unit
        ): XdmValue<T> {
            contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }

            return XdmSequenceBuilderImpl<T>().apply(builderAction).build(type)
        }

        fun <T : XdmSingleValue<T>> fromList(
            result: Collection<T>,
            staticType: XdmType = XdmSequenceType.ANYSEQ
        ): XdmValue<T> = when (result.size) {
            0 -> EMPTY
            1 -> result.single().asT()
            else if result is List -> Impl(result, staticType)
            else -> Impl(result.toList(), staticType)
        }

        fun <T : XsdAtomic> fromList(
            result: Collection<XdmAtomic<T>>,
            staticType: XdmType = XdmSequenceType.ANYSEQ
        ): XdmAtomicOrSequence<XdmAtomic<T>> = when (result.size) {
            0 -> EMPTY
            1 -> result.single().asT()
            else if result is List -> Impl(result, staticType)
            else -> Impl(result.toList(), staticType)
        }

        operator fun <T : XdmSingleValue<T>> invoke(
            elements: List<T> = emptyList(),
            staticType: XdmType = XdmSequenceType.ANYSEQ
        ): XdmSequence<T> {
            return Impl(elements, staticType)
        }

    }

    class RangeSequence<T: XsdInteger>(private val range: XsdIntegerProgression<T>): XdmSequence<XdmAtomic<T>>(
        staticType = XdmSequenceType(XdmSchemaType(range.first.schemaType), SequenceType.OccurrenceType.ANY)
    ) {
        override val size: Int
            get() = (range.last - range.first).toInt() + 1

        override fun iterator(): Iterator<XdmAtomic<T>> {
            return RangeIterator(range)
        }

        override fun isEmpty(): Boolean {
            return super.isEmpty()
        }

        override fun get(index: Int): XdmAtomic<T> {
            if (index < 0 || range.last - index < range.first) throw RangeException("Index $index out of range $range")

            @Suppress("UNCHECKED_CAST")
            val value: T = range.first.plus(index) as T
            return XdmAtomic(value)
        }

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun treatAsNonEmpty(type: XdmTypeTest): RangeSequence<T> {
            if (isEmpty()) throw EvaluationException("Empty range is not non-empty")
            return this // do some type check
        }

        override fun listIterator(): ListIterator<XdmAtomic<T>> {
            return RangeIterator(range)
        }

        override fun listIterator(index: Int): ListIterator<XdmAtomic<T>> {
            if (index < 0 || range.last - index < range.first) throw RangeException("Index $index out of range $range")

            @Suppress("UNCHECKED_CAST")
            return RangeIterator(range, (range.first + index) as T)
        }

        override fun subList(fromIndex: Int, toIndex: Int): RangeSequence<T> {
            if (fromIndex !in 0..toIndex || range.last - toIndex < range.first)
                throw RangeException("Indeces $fromIndex..$toIndex out of range $range")

            val newRange = (range.first + fromIndex).rangeTo(range.first + toIndex)
            @Suppress("UNCHECKED_CAST")
            return RangeSequence(newRange) as RangeSequence<T>
        }

        override fun toList(): List<XdmAtomic<T>> = this

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun atomizeTo(receiver: XdmSequenceBuilder<XdmAtomic<*>>) {
            receiver.add(this)
        }

        override fun toString(): String {
            return "(${range.first}..${range.last})"
        }

        override fun lastIndexOf(element: XdmAtomic<T>): Int =
            indexOfLast { it == element }


        private class RangeIterator<T: XsdInteger>(range: XsdIntegerProgression<T>, private var pos: T = range.first): ListIterator<XdmAtomic<T>> {
            private val first = range.first
            private val last = range.last

            override fun hasNext(): Boolean = pos <= last

            override fun hasPrevious(): Boolean = pos > first

            override fun next(): XdmAtomic<T> {
                return XdmAtomic(pos).also {
                    if (pos > last) throw RangeException("Next value is out of range $first..$last")
                    @Suppress("UNCHECKED_CAST")
                    pos = (pos + 1) as T
                }
            }

            override fun nextIndex(): Int {
                return (pos - first).toInt()
            }

            override fun previous(): XdmAtomic<T> {
                if (pos <= first) throw RangeException("Previous value is out of range $first..$last")
                @Suppress("UNCHECKED_CAST")
                return XdmAtomic(((pos - 1) as T).also { pos = it })
            }

            override fun previousIndex(): Int {
                return (pos - first).toInt() - 1
            }
        }

    }

    inner class Map<T: XdmSingleValue<T>, R : XdmSingleValue<R>>(
        val base: XdmSequence<T>,
        val operation: (T) -> R
    ): XdmSequence<R>(operation(base.first()).staticType) {
        override val size: Int get() = this@XdmSequence.size

        override fun get(index: Int): R {
            return operation(base[index])
        }

        override fun lastIndexOf(element: R): Int {
            return indexOfLast { it == element }
        }

        override fun iterator(): Iterator<R> {
            return listIterator()
        }

        context(ctx: ExprEvalContext)
        override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
            return Map(base) { t -> operation(t).atomize() as XdmAtomic<XsdAtomic> }
        }

        override fun listIterator(): ListIterator<R> {
            return MapIterator(base.listIterator())
        }

        override fun listIterator(index: Int): ListIterator<R> {
            return MapIterator(base.listIterator(index))
        }

        override fun subList(fromIndex: Int, toIndex: Int): XdmSequence<R> {
            return Map(base.subList(fromIndex, toIndex), operation)
        }

        override fun toList(): List<R> = this

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun treatAsNonEmpty(type: XdmTypeTest): XdmValue<*> {
            return this
        }

        override fun toString(): String {
            return "Map($base, ${operation(base.first())}..${operation(base.last())})"
        }

        private inner class MapIterator(private val baseIterator: ListIterator<T>): ListIterator<R> {
            override fun hasNext(): Boolean = baseIterator.hasNext()

            override fun hasPrevious(): Boolean = baseIterator.hasPrevious()

            override fun next(): R = operation(baseIterator.next())

            override fun nextIndex(): Int = baseIterator.nextIndex()

            override fun previous(): R = operation(baseIterator.previous())

            override fun previousIndex(): Int = baseIterator.previousIndex()
        }

    }

}

@OptIn(XPathInternal::class)
sealed interface XdmAtomicOrEmpty<out E : XdmSingleValue<E>> : XdmAtomicOrSequence<E>, XdmSingleOrEmpty<E>

@OptIn(XPathInternal::class)
sealed interface XdmAtomicOrSequence<out E : XdmSingleValue<E>> : XdmValue<E>

@OptIn(XPathInternal::class)
sealed interface XdmSingleOrEmpty<out E : XdmSingleValue<E>> : XdmValue<E>

