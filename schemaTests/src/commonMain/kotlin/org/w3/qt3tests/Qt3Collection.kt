/*
 * Copyright (c) 2026.
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

package org.w3.qt3tests

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.w3.qt3tests.attrGroups.Qt3UriAttr

/**
 * Represents a collection accessible to the collection() function.
 * 
 * The `uri` attribute identifies the collection URI. If this is absent or zero-length,
 * the collection acts as the default collection, used when no URI is supplied to the
 * `collection()` function.
 *    
 * The contained `source` elements identify the documents making up the collection.
 */
@Serializable
@XmlSerialName("collection", QT3TNS)
class Qt3Collection(
    val sources: List<Qt3Source>,
    val resources: List<Qt3Resource>,
    val queries: List<Qt3Query>,
    override val uri: VAnyURI? = null,
): Qt3Environment.Element, Qt3UriAttr
