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

package io.github.pdvrieze.xml.schematypes

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.toCName

object AnyTypeInst: IAnyType {
    override val name: QName get() = T_ANY
    val baseType: IAnyType get() = this

    override fun toString(): String = name.toCName()
}


interface IAnyType {
    val name: QName get() = AnyTypeInst.name
}

sealed interface ISimpleType : IAnyType {
    interface Atomic : ISimpleType
    interface List : ISimpleType
    interface Union : ISimpleType
}

interface IComplexType: IAnyType

interface IUntypedType: IComplexType {
    override val name: QName get() = T_UNTYPED

}

internal val T_UNTYPED = QName(XMLConstants.XSD_NS_URI, "untyped", "xs")
internal val T_ANY = QName(XMLConstants.XSD_NS_URI, "any", "xs")
