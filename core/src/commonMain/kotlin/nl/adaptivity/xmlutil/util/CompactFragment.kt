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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlDeserializerFactory
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializable

/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.2
 */
public expect class CompactFragment : ICompactFragment {
    public constructor(content: String)
    public constructor(orig: ICompactFragment)
    public constructor(namespaces: Iterable<Namespace>, content: CharArray?)
    public constructor(namespaces: Iterable<Namespace>, content: String)

    public class Factory() : XmlDeserializerFactory<CompactFragment>

    public companion object {
        public fun deserialize(reader: XmlReader): CompactFragment
    }
}
