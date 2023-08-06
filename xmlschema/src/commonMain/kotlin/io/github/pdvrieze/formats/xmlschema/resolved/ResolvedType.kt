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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSIType
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl

sealed interface ResolvedType : ResolvedAnnotated {
    val rawPart: Any?
    val mdlBaseTypeDefinition: ResolvedType
    val mdlFinal: Set<VDerivationControl.Type>
    val mdlScope: VTypeScope

    fun checkType(checkHelper: CheckHelper)

    fun validate(representation: VString)

    fun validateValue(representation: Any) {}

    /**
     * Defined in 3.3.4.2 last paragraph
     */
    fun isValidRestrictionOf(other: ResolvedType): Boolean {
        // subject to blocking keywords
        return isValidSubtitutionFor(other)
    }

    /**
     * Defined in 3.3.4.2
     */
    fun isValidSubtitutionFor(other: ResolvedType): Boolean

    /**
     * Defined by 3.4.6.5
     */
    fun isValidlyDerivedFrom(simpleBase: ResolvedSimpleType): Boolean
}

