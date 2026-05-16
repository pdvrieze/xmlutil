/*
 * Copyright (c) 2024-2026.
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

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom.PlatformDocumentFragment
import nl.adaptivity.xmlutil.dom2.DocumentFragment

internal class DocumentFragmentImpl(delegate: PlatformDocumentFragment) :
    AbstractNodeImpl<PlatformDocumentFragment>(delegate), DocumentFragment {
    override fun getNodeValue(): Nothing? = null

    override fun getOwnerDocument(): DocumentImpl {
        return checkNotNull(super.getOwnerDocument())
    }

    override fun setNodeValue(value: String?) {}

    override fun getAttributes(): Nothing? = null

    override fun cloneNode(deep: Boolean): DocumentFragmentImpl {
        return DocumentFragmentImpl(delegate.cloneNode(deep) as PlatformDocumentFragment)
    }
}
