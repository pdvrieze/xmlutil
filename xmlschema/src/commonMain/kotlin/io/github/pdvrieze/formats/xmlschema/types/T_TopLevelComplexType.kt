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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel

interface T_GlobalComplexType_Base: T_ComplexType, T_GlobalType {
    override val name: VNCName

    /**
     * Default: false
     */
    val abstract: Boolean?
    val final: Set<out ComplexTypeModel.Derivation>?
    val block: Set<out ComplexTypeModel.Derivation>?

}

interface T_TopLevelComplexType_Simple: T_GlobalComplexType_Base,
    T_ComplexType.Simple

interface T_TopLevelComplexType_Complex: T_GlobalComplexType_Base,
    T_ComplexType.Complex

interface T_TopLevelComplexType_Shorthand: T_GlobalComplexType_Base,
    T_ComplexType.Shorthand
