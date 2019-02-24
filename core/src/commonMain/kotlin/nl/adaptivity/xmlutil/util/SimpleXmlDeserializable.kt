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
@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.XmlDeserializable
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader


/**
 * Despite the name it is reasonably sophisticated, but it structures the parsing of the children and
 * provides them individually. This in contrast to [ExtXmlDeserializable] that provides full access
 * to parse the content whatever way desired.
 * Created by pdvrieze on 04/11/15.
 */
interface SimpleXmlDeserializable : XmlDeserializable {


    /**
     * Handle the current child element
     * @param reader The reader to read from. It is at the relevant start node.
     *
     * @return `true`, if processed, `false` if not (will trigger an error)
     *
     * @throws XmlException If something else failed.
     */
    fun deserializeChild(reader: XmlReader): Boolean = false

    /**
     * Handle text content in the node. This may be called multiple times in a single element if there are tags in between
     * or the parser isn't coalescing.
     * @param elementText The read text
     *
     * @return true if handled, false if not (whitespace will be ignored later on though, other text will trigger a failure)
     */
    fun deserializeChildText(elementText: CharSequence): Boolean = false
}
