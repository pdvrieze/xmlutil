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
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmDocument
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmElement
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmParentNode
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmNodeKindTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.impl.IAbstractNode
import nl.adaptivity.xmlutil.dom2.previousSibling

@OptIn(XPathInternal::class)
interface XdmNode<out T: XdmNode<T>>: XdmNodeBase<T>,
    IAbstractNode<XdmNode<*>, XdmParentNode<*>>,
    Node {

    var posInParent: Int
        @XdmNodeFriend set

    abstract override fun getOwnerDocument(): XdmDocument?

    @XdmNodeFriend
    fun setOwnerDocument(ownerDocument: XdmDocument)

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmNodeAlias<@UnsafeVariance T> {
        if (type !is XdmNodeKindTest) throw EvaluationException(ErrorCodes.XPDY0050_INVALID_TYPE_IN_TREAT_AS, "Cannot cast node to $type")
        if (! type.nodeKind.matches(this)) throw EvaluationException(ErrorCodes.XPDY0050_INVALID_TYPE_IN_TREAT_AS, "Cannot cast $this to (${type.nodeKind})")
        // TODO do some checks
        return XdmNodeAlias(this, type.toValueType(staticType).single)
    }

    context(ctx: ExprEvalContext)
    fun isNodeEqual(rightNode: Node, collation: Collation): Boolean

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(other: XdmNode<*>, collation: Collation): Boolean

    override fun cloneNode(deep: Boolean): XdmNode<T>

    fun descendantsSequence(): Sequence<XdmNode<*>> {
        return emptySequence()
    }

    companion object {
        val DOCUMENT_ORDER: Comparator<XdmNode<*>> = object : Comparator<XdmNode<*>> {
            override fun compare(a: XdmNode<*>, b: XdmNode<*>): Int {
                if (a === b) return 0

                // we include the node itself
                val ancestorsA = generateSequence(a) { it.getParentNode() as? XdmElement }.toList().reversed()
                val ancestorsB = generateSequence(b) { it.getParentNode() as? XdmElement }.toList().reversed()
                if (a in ancestorsB) return -1 // must be before
                if (b in ancestorsA) return 1 // must be after

                if (ancestorsA[0] != ancestorsB[0])
                    throw IllegalArgumentException("Nodes have no common ancestor, and thus no document order")

                // loop through all ancestors (not considering the actual node)
                for (i in 1 until minOf(ancestorsA.size, ancestorsB.size)) {
                    if (ancestorsA[i] != ancestorsB[i]) {
                        // We look in the previous siblings of the ancestors to optimize for already sorted
                        var x = ancestorsB[i].previousSibling
                        while (x != null) {
                            if (x === ancestorsA[i]) return -1
                            x = x.previousSibling
                        }
                        return 1
                    }
                }
                // Must have all shared ancestors
                //return a.posInParent - b.posInParent
                // should not happen as this implies the list is a subset
                return ancestorsB.size - ancestorsA.size
            }
        }

    }
}

@RequiresOptIn("Friend for XdmNode")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
internal annotation class XdmNodeFriend
