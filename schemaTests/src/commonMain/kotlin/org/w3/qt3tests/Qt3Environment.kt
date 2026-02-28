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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.w3.qt3tests.attrGroups.Qt3NameAttr
import org.w3.qt3tests.attrGroups.Qt3RefAttr
import org.w3.qt3tests.resolved.ResolutionContext
import org.w3.qt3tests.resolved.ResolvedQt3Environment

/**
 * Denotes an element which defines an assorted list of schemas and sources documents available to test cases.
 * For definition of schemas the test drivers should assume a dependency on schema-awareness.
 *
 * In addition the environment may set further information about the static context for running the test.
 *
 * An environment can be made globally available across all test-sets,
 * or within a particular test-set or within a test-case.
 * Locally-defined environments are considered to have precedence over
 * environments defined at some ancestor node, therefore conflicts are avoided.
 *
 * An environment that is shared between test cases always has a name (given by its `name` attribute).
 * An environment element within a test case may either be a reference to a shared environment (identified
 * by its `ref` attribute) or a locally-defined environment (with no `name` or `ref`
 * attributes.
 */
@Serializable
@XmlSerialName("environment", QT3TNS)
class Qt3Environment : Qt3BaseType, Qt3NameAttr, Qt3RefAttr {

    override val name: String?
    override val ref: String?
    val schemas: List<Qt3Schema>
    val sources: List<Qt3Source>
    val resource: List<Qt3Resource>
    val params: List<Qt3Param>
    val contextItems: List<Qt3ContextItem>
    val decimalFormats: List<Qt3DecimalFormat>
    val namespaces: List<Qt3Namespace>
    val functionLibraries: List<Qt3FunctionLibrary>
    val collections: List<Qt3Collection>
    val staticBaseUris: List<Qt3StaticBaseUri>
    val collations: List<Qt3Collation>

    constructor(
        name: String? = null,
        id: VID? = null,
        ref: String? = null,
        schemas: List<Qt3Schema>,
        sources: List<Qt3Source>,
        resource: List<Qt3Resource>,
        params: List<Qt3Param>,
        contextItems: List<Qt3ContextItem>,
        decimalFormats: List<Qt3DecimalFormat>,
        namespaces: List<Qt3Namespace>,
        functionLibraries: List<Qt3FunctionLibrary>,
        collections: List<Qt3Collection>,
        staticBaseUris: List<Qt3StaticBaseUri>,
        collations: List<Qt3Collation>
    ) : super(id) {
        require(ref!=null || name!=null) {
            "Environment must have a name, or be a reference"
        }

        this.name = name
        this.ref = ref
        this.schemas = schemas
        this.sources = sources
        this.resource = resource
        this.params = params
        this.contextItems = contextItems
        this.decimalFormats = decimalFormats
        this.namespaces = namespaces
        this.functionLibraries = functionLibraries
        this.collections = collections
        this.staticBaseUris = staticBaseUris
        this.collations = collations
    }

    context(ctx: ResolutionContext)
    fun resolve(): ResolvedQt3Environment = when (ref) {
        null -> ResolvedQt3Environment(
            name, // note that anonymous environments are allowed
            schemas,
            sources.map { it.resolve() },
            resource,
            params,
            contextItems,
            decimalFormats,
            namespaces,
            functionLibraries,
            collections,
            staticBaseUris,
            collations,
        ).also { if (name != null) ctx.knownEnvironments[name] = it }

        else -> checkNotNull(ctx.knownEnvironments[ref]) { "Unknown environment $ref" }
    }

    @Serializable
    sealed interface Element {}


}
