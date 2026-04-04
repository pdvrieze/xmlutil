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
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(XPathInternal::class)
open class XdmSequence<out T : XdmSingleValue<T>> internal constructor(
    internal val elements: List<T> = emptyList(),
    override val staticType: XdmType = XdmSequenceType.ANYSEQ
) : XdmAtomicOrSequence<T>, List<T> {
    override val size: Int get() = elements.size

    override fun get(index: Int): T {
        return elements[index]
    }


    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean = when {
        isEmpty() -> false
        elements[0] is XdmNode -> true
        elements.size == 1 -> elements[0].toBoolean() // should not happen, but be sure
        else -> throw EvaluationException(ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE, "Only sequences starting with a node can be cast to boolean")
    }

    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmValue {
        TODO("not implemented")
    }

    override fun isValEqual(expected: XdmValue): Boolean {
        return expected is XdmSequence<*> && elements == expected.elements
    }

    context(ctx: ExprEvalContext)
    override fun evalPredicates(predicates: Iterable<Expr>): XdmValue {
        return flatMap { it.evalPredicates(predicates) }
    }

    fun flatMap(transform: (T) -> XdmValue): XdmValue {
        return build {
            for (e in elements) add(transform(e))
        }
    }

    context(ctx: ExprEvalContext)
    override fun atomizeTo(receiver: MutableList<in XdmAtomic<XsdAtomic>>) {
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

    class Empty(type: XdmType = XdmEmptySequenceType) : XdmSequence<Nothing>(staticType = type),
        XdmAtomicOrEmpty<Nothing>, XdmSingleOrEmpty<Nothing>

    companion object {
        val EMPTY: Empty = empty(XdmEmptySequenceType)

        interface XdmSequenceBuilder {
            fun add(value: XdmSingleValue<*>)

            fun addAll(values: Iterable<XdmSingleValue<*>>)

            fun add(value: XdmSequence<*>) = addAll(value.elements)

            fun add(value: XdmValue) = when (value) {
                is XdmSequence<*> -> add(value)
                is XdmSingleValue<*> -> add(value)
            }
        }

        internal class XdmSequenceBuilderImpl : XdmSequenceBuilder {
            private val elements = mutableListOf<XdmSingleValue<*>>()
            override fun add(value: XdmSingleValue<*>) {
                elements.add(value)
            }

            override fun addAll(values: Iterable<XdmSingleValue<*>>) {
                elements.addAll(values)
            }

            fun build(type: XdmSequenceType): XdmValue {
                return when (elements.size) {
                    0 -> empty(type)
                    1 -> elements.single()
                    else -> XdmSequence(elements, type)
                }
            }
        }

        @OptIn(ExperimentalContracts::class)
        internal inline fun build(
            type: XdmSequenceType = XdmSequenceType.ANYSEQ,
            builderAction: XdmSequenceBuilder.() -> Unit
        ): XdmValue {
            contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }

            return XdmSequenceBuilderImpl().apply(builderAction).build(type)
        }

        fun empty(type: XdmType): Empty = Empty(type)
    }
}

@OptIn(XPathInternal::class)
sealed interface XdmAtomicOrEmpty<out E : XdmSingleValue<E>> : XdmAtomicOrSequence<E>, XdmSingleOrEmpty<E>

@OptIn(XPathInternal::class)
sealed interface XdmAtomicOrSequence<out E : XdmSingleValue<E>> : XdmValue

@OptIn(XPathInternal::class)
sealed interface XdmSingleOrEmpty<out E : XdmSingleValue<E>> : XdmValue

