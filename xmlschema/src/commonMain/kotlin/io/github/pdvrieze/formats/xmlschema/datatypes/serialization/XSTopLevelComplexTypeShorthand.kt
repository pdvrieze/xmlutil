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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_DerivationSet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_TopLevelComplexType_Shorthand
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer

class XSTopLevelComplexTypeShorthand(
    name: NCName,
    mixed: Boolean?,
    abstract: Boolean,
    final: T_DerivationSet,
    block: T_DerivationSet,
    defaultAttributesApply: Boolean?,
    override val groups: List<XSGroupRef>,
    override val alls: List<XSAll>,
    override val choices: List<XSChoice>,
    override val sequences: List<XSSequence>,
    override val asserts: List<XSAssert>,
    override val attributes: List<XSLocalAttribute>,
    override val attributeGroups: List<XSAttributeGroupRef>,
    override val anyAttribute: XSAnyAttribute?,
    override val openContents: List<XSOpenContent>,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSTopLevelComplexType(
    name,
    mixed,
    abstract,
    final,
    block,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_TopLevelComplexType_Shorthand {
    override val content: G_ComplexTypeModel.Shorthand get() = this

    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed,
            abstract = abstract,
            final = final,
            block = block,
            defaultAttributesApply = defaultAttributesApply,
            groups = groups,
            alls = alls,
            choices = choices,
            sequences = sequences,
            asserts = asserts,
            atributes = attributes,
            atributeGroups = attributeGroups,
            anyAttribute = anyAttribute,
            openContents = openContents,
            id = id,
            annotations = annotations,
            otherAttrs = otherAttrs
        )
    }

}
