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

package nl.adaptivity.xmlutil.dom2.impl

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.Node

@ExperimentalXmlUtilApi
public interface IAbstractParentNode<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>: IAbstractNode<N, P> {
    override fun getParentNode(): P?

    override fun getOwnerDocument(): AbstractDocument<N, P>?

    override fun getFirstChild(): N?

    override fun getLastChild(): N?

    override fun getPreviousSibling(): N?

    override fun getNextSibling(): N?

    override fun appendChild(node: PlatformNode): N

    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): N

    override fun removeChild(node: PlatformNode): N

    public fun getSiblingBefore(ref: Node): N?

    public fun getSiblingAfter(ref: Node): N?

    public override fun getChildNodes(): AbstractNodeList<N, P>

    override fun cloneNode(deep: Boolean): IAbstractParentNode<N, P>
}
