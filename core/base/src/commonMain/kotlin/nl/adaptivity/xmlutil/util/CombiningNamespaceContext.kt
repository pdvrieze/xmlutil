/*
 * Copyright (c) 2024.
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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.util.impl.CombiningNamespaceContext as ImplCombiningNamespaceContext


/**
 * A namespace context that combines two namespace contexts. Resolution will first attempt to use
 * the `primary` context, The secondary namespace is a fallback.
 *
 * @property primary The context to first use for looking up
 * @property secondary The fallback context if the name cannot be resolved on the primary.
 */
@XmlUtilInternal
@Deprecated(
    "This type is really only for internal use. It will be moved to a better location"
)
public typealias CombiningNamespaceContext = ImplCombiningNamespaceContext

