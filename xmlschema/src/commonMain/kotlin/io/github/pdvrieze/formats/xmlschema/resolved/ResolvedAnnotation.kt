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
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix

class ResolvedAnnotation(val rawPart: XSAnnotation) {
    val id: VID? = rawPart.id

    val mdlOtherAttrs: Map<QName, String> get() = rawPart.otherAttrs
    val mdlApplicationInformation: List<XSAppInfo> get() = rawPart.appInfos
    val mdlUserInformation: List<XSDocumentation> get() = rawPart.documentationElements

    fun check(checkedTypes: MutableSet<QName>) {
        val xsAttrs = mdlOtherAttrs.keys.filter { it.prefix=="" || it.namespaceURI== XMLConstants.XSD_NS_URI }
        check(xsAttrs.isEmpty()) { "Open attributes in the empty or xmlschema namespace found: [${xsAttrs.joinToString()}]" }
    }

}
