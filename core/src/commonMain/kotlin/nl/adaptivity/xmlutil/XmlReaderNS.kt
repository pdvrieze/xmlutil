/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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


@file:JvmName("XmlReaderUtil")
@file:JvmMultifileClass

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Differs from [.siblingsToFragment] in that it skips the current event.
 *
 * @throws XmlException
 */
public fun XmlReader.elementContentToFragment(): ICompactFragment {
    val r = this
    r.skipPreamble()
    if (r.hasNext()) {
        r.require(EventType.START_ELEMENT, null, null)
        r.next()
        return r.siblingsToFragment()
    }
    return CompactFragment("")
}

public expect fun XmlReader.siblingsToFragment(): CompactFragment

@Suppress("DeprecatedCallableAddReplaceWith", "deprecation")
@Deprecated("This is inefficient in Javascript")
public fun XmlReader.siblingsToCharArray(): CharArray = siblingsToFragment().content

