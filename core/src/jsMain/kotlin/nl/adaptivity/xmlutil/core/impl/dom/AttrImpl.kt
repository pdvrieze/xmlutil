/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.IAttr
import nl.adaptivity.xmlutil.core.impl.idom.IElement
import org.w3c.dom.Attr as DomAttr
import org.w3c.dom.Node as DomNode

internal class AttrImpl(delegate: DomAttr) : NodeImpl<DomAttr>(delegate), IAttr {
    override var value: String
        get() = delegate.value
        set(value) {
            delegate.value = value
        }

    override val namespaceURI: String? get() = delegate.namespaceURI

    override val prefix: String? get() = delegate.prefix

    override val localName: String
        get() = delegate.localName

    override val name: String get() = delegate.name

    override val ownerElement: IElement?
        get() = delegate.ownerElement?.wrap()

}

internal fun DomNode.wrapAttr(): IAttr {
    return (this as DomAttr).wrap()
}
