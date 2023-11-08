/*
 * Copyright (c) 2021.
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

package nl.adaptivity.xmlutil.xmlserializable

import nl.adaptivity.xmlutil.IXmlStreaming
import nl.adaptivity.xmlutil.XmlSerializable
import nl.adaptivity.xmlutil.newWriter
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.xmlStreaming

actual fun CompactFragment(content: XmlSerializable): CompactFragment {
    return CompactFragment(emptyList(), content.toString())
}

actual fun IXmlStreaming.toString(value: XmlSerializable): String {
    val w = xmlStreaming.newWriter()
    try {
        value.serialize(w)
    } finally {
        w.close()
    }
    return w.target.toString()
}
