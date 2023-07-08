/*
 * Copyright (c) 2022.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAppInfo
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSDocumentation
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.types.XSI_OpenAttrs
import nl.adaptivity.xmlutil.QName

class ResolvedAnnotation(val rawPart: XSAnnotation) : XSI_OpenAttrs, AnnotationModel {
    val id: VID? = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs
    
    override val mdlApplicationInformation: List<XSAppInfo> get() = rawPart.appInfos
    override val mdlUserInformation: List<XSDocumentation> get() = rawPart.documentationElements
    override val mdlAttributes: Map<QName, String> get() = otherAttrs
}
