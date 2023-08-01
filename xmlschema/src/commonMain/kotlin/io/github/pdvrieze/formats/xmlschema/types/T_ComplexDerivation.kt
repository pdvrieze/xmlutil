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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttributeGroupRef
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSOpenContent
import io.github.pdvrieze.formats.xmlschema.model.I_Assertions

interface T_ComplexDerivation : T_ComplexType.ParticleProperties,
    I_AttributeContainer, I_Assertions, T_Derivation {
    val openContent: XSOpenContent?
    val attributes: List<XSLocalAttribute>

    /** Name elements AttributeGroup */
    val attributeGroups: List<XSAttributeGroupRef>
    val anyAttribute: XSAnyAttribute?
}

sealed interface T_ComplexDerivationSealedBase : T_ComplexDerivation
