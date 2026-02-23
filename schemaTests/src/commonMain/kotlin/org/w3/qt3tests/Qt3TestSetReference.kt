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

package org.w3.qt3tests

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.w3.qt3tests.attrGroups.Qt3FileAttr
import org.w3.qt3tests.attrGroups.Qt3NameAttr
import org.w3.qt3tests.resolved.ResolutionContext
import org.w3.qt3tests.resolved.ResolvedQt3TestSet
import org.w3.qt3tests.resolved.subContext

@Serializable
@XmlSerialName("test-set", QT3TNS)
class Qt3TestSetReference : Qt3BaseType, Qt3NameAttr, Qt3FileAttr {
    override val name: String
    override val file: VAnyURI

    constructor(name: String, file: VAnyURI, id: VID? = null) : super(id) {
        this.name = name
        this.file = file
    }

    context(ctx: ResolutionContext)
    fun resolve() : ResolvedQt3TestSet {
        val testSet = ctx.parseFile(Qt3TestSet.serializer(), file.value)
        check(name == testSet.name) { "Name mismatch in testSet: $name != ${testSet.name}" }
        return ctx.subContext(file.value) {
            testSet.resolve()
        }
    }
}
