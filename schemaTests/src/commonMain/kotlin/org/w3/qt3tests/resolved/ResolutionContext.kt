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

package org.w3.qt3tests.resolved

import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.serialization.XML

interface ResolutionContext {
    val base: String
    val xml: XML
    val knownEnvironments: MutableMap<String, ResolvedQt3Environment>
    val idMap: MutableMap<String, Any>

    fun parseDocument(relativePath: String): Document
    fun <T> parseFile(deserializer: DeserializationStrategy<T>, relativePath: String): T
}
