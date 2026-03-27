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

package io.github.pdvrieze.formats.xpath.data

import io.github.pdvrieze.formats.xpath.impl.Expr
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(XPathInternal::class)
class XdmSequence<out T : XdmSingleValue<T>>(
    internal val elements: List<T> = emptyList(),
    override val type: XdmType = XdmSequenceType.ANYSEQ
) : XdmValue(), List<T> {
    override val size: Int get() = elements.size

    override fun get(index: Int): T {
        return elements[index]
    }


    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean = when {
        isEmpty() -> false
        else -> elements[0] is XdmNode
    }

    context(ctx: ExprEvalContext)
    override fun withType(type: XdmType): XdmValue {
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
    override fun atomizeTo(receiver: MutableList<in XdmSingleValue<*>>) {
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

    companion object {
        val EMPTY: XdmSequence<Nothing> = XdmSequence()

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
                    0 -> EMPTY
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
    }
}
