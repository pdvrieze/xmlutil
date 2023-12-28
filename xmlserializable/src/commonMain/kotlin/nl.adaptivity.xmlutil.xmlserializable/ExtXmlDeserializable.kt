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

import nl.adaptivity.xmlutil.XmlReader


/**
 * Interface that allows more customization on child deserialization than [SimpleXmlDeserializable].
 * Created by pdvrieze on 04/11/15.
 */
public interface ExtXmlDeserializable : XmlDeserializable {

    /**
     * Called to have all children of the current node deserialized. The attributes have already been parsed. The expected
     * end state is that the streamreader is at the corresponding endElement.
     * @param reader The streamreader that is the source of the events.
     */
    public fun deserializeChildren(reader: XmlReader)
}
