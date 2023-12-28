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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.xmlserializable

import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.XmlSerializable as SerializableCompat

public interface XmlSerializable : SerializableCompat {

    /**
     * Write the object to an xml stream. The object is expected to write itself and its children.
     * @param out The stream to write to.
     */
    public override fun serialize(out: XmlWriter)

}

internal fun SerializableCompat.wrap(): XmlSerializable {
    return this as? XmlSerializable ?: CompatXmlSerializableWrapper(this)
}

private class CompatXmlSerializableWrapper(private val delegate: SerializableCompat) : XmlSerializable {
    override fun serialize(out: XmlWriter) = delegate.serialize(out)
}
