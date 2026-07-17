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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.type.XdmEmptySequenceType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSequenceType
import io.github.pdvrieze.formats.xpath.eval.type.XdmType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.Expr
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(XPathInternal::class)
open class XdmSequence<out T : XdmSingleValue<T>> internal constructor(
    internal val elements: List<T> = emptyList(),
    override val staticType: XdmType = XdmSequenceType.ANYSEQ
) : XdmAtomicOrSequence<T>, List<T> {
    init {
        when (staticType) {
            XdmEmptySequenceType -> check(elements.isEmpty())
            else -> require(elements.size > 1) { "Sequence must have at least two elements. Found: ${elements.size} -> $elements" }
        }
    }

    override val size: Int get() = elements.size

    override fun get(index: Int): T {
        return elements[index]
    }


    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean = when {
        isEmpty() -> false
        elements[0] is XdmNodeBase<*> -> true
        elements.size == 1 -> elements[0].toBoolean() // should not happen, but be sure
        else -> throw EvaluationException(ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE, "Only sequences starting with a node can be cast to boolean")
    }

    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmValue<*> {
        // TODO actually perform checks
        return XdmSequence(elements, type.toValueType(staticType.single))
    }

    override fun isValEqual(expected: XdmValue<*>, collation: Collation?): Boolean {
        return expected is XdmSequence<*> && elements == expected.elements
    }

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(
        other: XdmValue<*>,
        collation: Collation?
    ): Boolean {
        if (size != other.size) return false
        return 0.until(size).all { i -> elements[i].isDeepEqual(other[i], collation) }
    }

    context(ctx: ExprEvalContext)
    override fun evalPredicates(predicates: Iterable<Expr>): XdmValue<*> {
        return flatMap { it.evalPredicates(predicates) }
    }

    fun <R: XdmSingleValue<R>> flatMap(transform: (T) -> XdmValue<R>): XdmValue<R> {
        return build {
            for (e in elements) add(transform(e))
        }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun atomizeTo(receiver: XdmSequenceBuilder<XdmAtomic<*>>) {
        for (e in elements) {
            e.atomizeTo(receiver)
        }
    }

    override fun contains(element: @UnsafeVariance T): Boolean = elements.contains(element)

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean = elements.containsAll(elements)

    override fun indexOf(element: @UnsafeVariance T): Int = elements.indexOf(element)

    override fun isEmpty(): Boolean = elements.isEmpty()

    override fun iterator(): Iterator<T> = elements.iterator()

    override fun lastIndexOf(element: @UnsafeVariance T): Int = elements.lastIndexOf(element)

    override fun listIterator(): ListIterator<T> = elements.listIterator()

    override fun listIterator(index: Int): ListIterator<T> = elements.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<T> = elements.subList(fromIndex, toIndex)

    override fun toString(): String {
        return elements.joinToString(prefix = "(", postfix = ")")
    }


    object EMPTY : XdmSequence<Nothing>(staticType = XdmEmptySequenceType),
        XdmAtomicOrEmpty<Nothing>, XdmSingleOrEmpty<Nothing> {

        override fun isEmpty(): Boolean = true

        override fun contains(element: Nothing): Boolean {
            return false
        }

        override fun containsAll(elements: Collection<Nothing>): Boolean {
            return elements.isEmpty()
        }
    }

    interface XdmSequenceBuilder<in T : XdmSingleValue<@UnsafeVariance T>> {
        fun add(value: T)

        fun addAll(values: Iterable<T>)

        fun addAll(values: Sequence<T>)

        fun add(value: XdmSequence<T>) = addAll(value.elements)

        fun add(value: XdmValue<T>) = when (value) {
            is XdmSequence<T> -> add(value)
            is XdmSingleValue<T> -> add(value.asT())
        }
    }

    internal class XdmSequenceBuilderImpl<T: XdmSingleValue<T>> : XdmSequenceBuilder<T> {
        private val elements = mutableListOf<T>()
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
                1 -> elements.single().asT()
                else -> XdmSequence(elements, type)
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
            result: List<T>,
            staticType: XdmType = XdmSequenceType.ANYSEQ
        ): XdmValue<T> = when (result.size) {
            0 -> EMPTY
            1 -> result.single().asT()
            else -> XdmSequence(result, staticType)
        }

    }
}

@OptIn(XPathInternal::class)
sealed interface XdmAtomicOrEmpty<out E : XdmSingleValue<E>> : XdmAtomicOrSequence<E>, XdmSingleOrEmpty<E>

@OptIn(XPathInternal::class)
sealed interface XdmAtomicOrSequence<out E : XdmSingleValue<E>> : XdmValue<E>

@OptIn(XPathInternal::class)
sealed interface XdmSingleOrEmpty<out E : XdmSingleValue<E>> : XdmValue<E>

