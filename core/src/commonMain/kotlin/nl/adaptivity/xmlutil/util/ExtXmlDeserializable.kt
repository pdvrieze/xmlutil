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
import nl.adaptivity.xmlutil.XmlReader


/**
 * Interface that allows more customization on child deserialization than [SimpleXmlDeserializable].
 * Created by pdvrieze on 04/11/15.
 */
interface ExtXmlDeserializable : XmlDeserializable {

    /**
     * Called to have all children of the current node deserialized. The attributes have already been parsed. The expected
     * end state is that the streamreader is at the corresponding endElement.
     * @param `in` The streamreader that is the source of the events.
     */
    fun deserializeChildren(reader: XmlReader)
}
