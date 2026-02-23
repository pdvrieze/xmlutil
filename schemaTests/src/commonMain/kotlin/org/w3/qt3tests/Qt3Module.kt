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

/**
 * Defines an XQuery library module imported by a test case. The module element has an `@uri`
 * attribute giving the module URI and an `@file` attribute giving the location of the module
 * (relative to the test set document).
 *
 * If the query contains an "import module" declaration referencing the same module URI, with no
 * "at" location, then the library module is to be found at the location given by the `@file`
 * attribute.
 *
 * If the query metadata contains several module elements giving the same module URI and different
 * file locations, the processor is expected to import all the modules referenced.
 *
 * If the query contains an "import module" declaration containing one or more "at" location hints,
 * then the test metadata should contain for each of these location hints, a `module` element whose
 * `@uri` attribute matches the module namespace, and whose `@location` attribute matches the
 * location hint (the match being done after absolutizing both URIs against their respective base
 * URIs). Each location hint then results in a module being loaded from the corresponding file,
 * identified by the `@file` attribute.
 * 
 * Location hints are used only when testing XQuery 3.0 or higher, since in 1.0 their semantics
 * were almost entirely implementation-defined.
 */
@Serializable
@XmlSerialName("module", QT3TNS)
class Qt3Module(
    val uri: VAnyURI? = null,
    val location: VAnyURI? = null,
    val file: VAnyURI? = null,
)
