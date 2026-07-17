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

package io.github.pdvrieze.formats.xpath.eval.data.dom

import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom2.NamedNodeMap
import nl.adaptivity.xmlutil.dom2.impl.IAbstractParentNode

@XPathInternal
public interface XdmParentNode<out T: XdmParentNode<T>> : XdmNode<T>, IAbstractParentNode<XdmNode<*>, XdmParentNode<*>> {
    override fun getParentElement(): XdmElement?

    @ExperimentalXmlUtilApi
    override fun getAttributes(): NamedNodeMap<XdmAttr>?

    override fun cloneNode(deep: Boolean): XdmParentNode<T>

    override fun descendantsSequence(): Sequence<XdmNode<*>> {
        return sequence {
            for (value in getChildNodes().iterator()) {
                yield(value)
                yieldAll(value.descendantsSequence())
            }
        }
    }

}
