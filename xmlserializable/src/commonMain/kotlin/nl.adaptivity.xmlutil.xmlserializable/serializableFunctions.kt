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

import nl.adaptivity.xmlutil.IXmlStreaming
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.jvm.JvmName
import nl.adaptivity.xmlutil.XmlSerializable as XmlSerializableCompat

public expect fun CompactFragment(content: XmlSerializable): CompactFragment

@Deprecated("Use the version using the new serializable")
public fun  CompactFragment(content: XmlSerializableCompat): CompactFragment {
    return CompactFragment(content.wrap())
}

public expect fun IXmlStreaming.toString(value: XmlSerializable): String

@Suppress("DEPRECATION")
@Deprecated("Use IXmlStreaming.toString", level = DeprecationLevel.HIDDEN)
public fun XmlStreaming.toString(value: XmlSerializable): String = (this as IXmlStreaming).toString(value)

@Deprecated("Use the version using the new serializable")
public fun XmlStreaming.toString(value: XmlSerializableCompat): String {
    return (this as IXmlStreaming).toString(value.wrap())
}

public fun XmlWriter.writeChild(child: XmlSerializable?) {
    child?.serialize(this)
}

@Deprecated("Use the version using the new serializable")
public fun XmlWriter.writeChild(child: XmlSerializableCompat?) {
    writeChild(child?.wrap())
}

@JvmName("writeChildren2")
public fun XmlWriter.writeChildren(children: Iterable<XmlSerializable>?) {
    children?.forEach { writeChild(it) }
}

@Deprecated("Use the version using the new serializable")
public fun XmlWriter.writeChildren(children: Iterable<XmlSerializableCompat>?) {
    children?.forEach { writeChild(it.wrap()) }
}


