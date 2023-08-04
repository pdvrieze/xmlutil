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

import io.github.pdvrieze.formats.xmlschema.types.VScopeVariety

interface IScope {
    val mdlVariety: VScopeVariety

    interface Global : IScope {
        override val mdlVariety: VScopeVariety get() = VScopeVariety.GLOBAL
    }

    interface Local : IScope {
        override val mdlVariety: VScopeVariety get() = VScopeVariety.LOCAL
        val parent: Any
    }
}

sealed class VAttributeScope : IScope {
    object Global : VAttributeScope(), IScope.Global

    class Local(override val parent: Member) : VAttributeScope(), IScope.Local

    interface Member
}

sealed class VElementScope : IScope {
    object Global : VElementScope(), IScope.Global

    class Local(override val parent: Member) : VElementScope(), IScope.Local

    interface Member
}

sealed interface VTypeScope : IScope {
    interface Global : VTypeScope, IScope.Global

    interface Local : VTypeScope, IScope.Local

    sealed interface MemberBase
    interface Member : VComplexTypeScope.Member, VSimpleTypeScope.Member
}

sealed class VComplexTypeScope : VTypeScope {
    object Global : VComplexTypeScope(), VTypeScope.Global

    class Local(override val parent: Member) : VComplexTypeScope(), VTypeScope.Local

    interface Member: VTypeScope.MemberBase
}

sealed class VSimpleTypeScope : VTypeScope {
    object Global : VSimpleTypeScope(), VTypeScope.Global

    class Local(override val parent: Member) : VSimpleTypeScope(), VTypeScope.Local

    interface Member : VTypeScope.MemberBase
}
