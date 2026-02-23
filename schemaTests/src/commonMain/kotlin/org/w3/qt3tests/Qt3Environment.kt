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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.w3.qt3tests.attrGroups.Qt3NameAttr
import org.w3.qt3tests.attrGroups.Qt3RefAttr

/**
 * Denotes an element which defines an assorted list of schemas and sources documents available to test cases.
 * For definition of schemas the test drivers should assume a dependency on schema-awareness.
 *
 * In addition the environment may set further information about the static context for running the test.
 *
 * An environment can be made globally available across all test-sets,
 * or within a particular test-set or within a test-case.
 * Locally-defined environments are considered to have precedence over
 * environments defined at some ancestor node, therefore conflicts are avoided.
 *
 * An environment that is shared between test cases always has a name (given by its `name` attribute).
 * An environment element within a test case may either be a reference to a shared environment (identified
 * by its `ref` attribute) or a locally-defined environment (with no `name` or `ref`
 * attributes.
 */
@Serializable
@XmlSerialName("environment", QT3TNS)
class Qt3Environment : Qt3BaseType, Qt3TestSet.Element, Qt3NameAttr, Qt3RefAttr {

    override val name: String?
    override val ref: String?
    val elements: List<Element>

    constructor(name: String? = null, ref: String? = null, elements: List<Element> = emptyList(), id: VID? = null) : super(id) {
        this.name = name
        this.ref = ref
        this.elements = elements
    }

    @Serializable
    sealed interface Element {}


}
