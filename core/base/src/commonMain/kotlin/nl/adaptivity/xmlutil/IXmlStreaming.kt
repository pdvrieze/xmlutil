/*
 * Copyright (c) 2024-2025.
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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Node

public interface IXmlStreaming {

    public fun setFactory(factory: XmlStreamingFactory?)

    @Deprecated("Should use two parameter version", level = DeprecationLevel.HIDDEN)
    public fun newReader(input: CharSequence): XmlReader =
        newReader(input, false)

    public fun newReader(input: CharSequence, expandEntities: Boolean = false): XmlReader

    @Deprecated("Should use two parameter version", level = DeprecationLevel.HIDDEN)
    public fun newReader(reader: Reader): XmlReader =
        newReader(reader, false)

    public fun newReader(reader: Reader, expandEntities: Boolean = false): XmlReader

    @ExperimentalXmlUtilApi
    public fun newReader(source: Node): XmlReader

    @Deprecated("Should use two parameter version", level = DeprecationLevel.HIDDEN)
    public fun newGenericReader(input: CharSequence): XmlReader =
        newGenericReader(input, false)

    public fun newGenericReader(input: CharSequence, expandEntities: Boolean = false): XmlReader

    @Deprecated("Should use two parameter version", level = DeprecationLevel.HIDDEN)
    public fun newGenericReader(reader: Reader): XmlReader =
        newGenericReader(reader, true)

    public fun newGenericReader(reader: Reader, expandEntities: Boolean = false): XmlReader

    @ExperimentalXmlUtilApi
    public fun newWriter(): DomWriter

    @ExperimentalXmlUtilApi
    public fun newWriter(dest: Node): DomWriter

    public val genericDomImplementation: DOMImplementation
}
