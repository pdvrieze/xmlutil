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
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.util.CompactFragment

expect fun CompactFragment(content: XmlSerializable): CompactFragment

expect fun IXmlStreaming.toString(value: XmlSerializable): String

@Suppress("DEPRECATION")
@Deprecated("Use IXmlStreaming.toString", level = DeprecationLevel.HIDDEN)
fun XmlStreaming.toString(value: XmlSerializable): String = (this as IXmlStreaming).toString(value)


fun XmlWriter.writeChild(child: XmlSerializable?) {
    child?.serialize(this)
}

fun XmlWriter.writeChildren(children: Iterable<XmlSerializable>?) {
    children?.forEach { writeChild(it) }
}
