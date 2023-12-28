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
import org.w3c.dom.Node
import org.w3c.dom.TypeInfo
import org.w3c.dom.Attr as DomAttr

internal class AttrImpl(delegate: DomAttr) : NodeImpl<DomAttr>(delegate), IAttr {
    override fun getOwnerElement(): IElement? = delegate.ownerElement?.wrap()

    override fun getName(): String = delegate.name

    override fun getSpecified(): Boolean = delegate.specified

    override fun getValue(): String = delegate.value

    override fun setValue(value: String?) {
        delegate.value = value
    }

    override fun getSchemaTypeInfo(): TypeInfo = delegate.schemaTypeInfo

    override fun isId(): Boolean = delegate.isId
}

internal fun Node.wrapAttr(): IAttr {
    return (this as DomAttr).wrap()
}
