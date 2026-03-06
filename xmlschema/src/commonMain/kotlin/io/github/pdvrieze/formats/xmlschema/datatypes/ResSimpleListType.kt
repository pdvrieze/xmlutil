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

package io.github.pdvrieze.formats.xmlschema.datatypes

import io.github.pdvrieze.formats.xmlschema.resolved.BuiltinListDerivation
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType
import io.github.pdvrieze.xml.schematypes.types.AnySimpleListType
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple

interface ResSimpleListType<out T: XsdAnySimple, out E: XsdAnySimple>: ResolvedSimpleType<T>, AnySimpleListType<T, E> {
    override val baseType: ResolvedSimpleType<*>
    override val itemType: ResolvedSimpleType<E>

    override val mdlItemTypeDefinition: ResolvedSimpleType<E> get() = itemType


}


internal object AnonListBaseType : AnySimpleListType.Instance<XsdAnySimple, XsdAnySimple>(null, ResAnySimpleType),
    ResListDatatype<XsdAnySimple, XsdAnySimple> {
    override val name: Nothing? get() = null

    override val itemType: ResolvedSimpleType<XsdAnySimple> get() = ResAnySimpleType
    override val baseType: ResAnySimpleType get() = ResAnySimpleType
    override val mdlItemTypeDefinition: ResolvedSimpleType<XsdAnySimple> get() = itemType

    override val simpleType: Nothing? get() = null
    override val simpleDerivation: ResolvedSimpleType.Derivation = BuiltinListDerivation(ResAnySimpleType)
}
