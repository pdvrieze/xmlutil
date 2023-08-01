/*
 * Copyright (c) 2021.
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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import nl.adaptivity.xmlutil.QName

interface T_GlobalElement: T_Element {
    override val name: VNCName

    override val substitutionGroup: List<QName>?

    override val final: Set<out ComplexTypeModel.Derivation>?

    override val ref: Nothing? get() = null
    override val form: Nothing? get() = null
    override val targetNamespace: VAnyURI? get() = null
    override val minOccurs: Nothing? get() = null
    override val maxOccurs: Nothing? get() = null

    /** Default: false */
    override val abstract: Boolean?
}
