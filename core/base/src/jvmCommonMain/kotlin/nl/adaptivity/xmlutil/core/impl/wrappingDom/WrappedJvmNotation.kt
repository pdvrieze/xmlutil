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

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom2.Attr
import nl.adaptivity.xmlutil.dom2.EmptyNamedNodeMap
import nl.adaptivity.xmlutil.dom2.NamedNodeMap
import nl.adaptivity.xmlutil.dom2.Notation
import org.w3c.dom.Notation as DomNotation

internal class WrappedJvmNotation(delegate: DomNotation): WrappedJvmNode<DomNotation>(delegate), Notation {
    @ExperimentalXmlUtilApi
    override fun getAttributes(): NamedNodeMap<Nothing> = EmptyNamedNodeMap

    override fun getSystemId(): String? {
        return delegate.systemId
    }

    override fun getPublicId(): String? {
        return delegate.publicId
    }
}
