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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttrUse
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalSimpleType
import nl.adaptivity.xmlutil.QName

interface T_LocalAttribute: T_AttributeBase

interface T_AttributeBase: XSI_Annotated, I_OptNamed {
    val default: CharSequence?
    val fixed: CharSequence?
    val form: T_FormChoice?
    val ref: QName?
    val type: QName?
    val use: XSAttrUse?
    val inheritable: Boolean?
    val simpleType: XSLocalSimpleType?
}
