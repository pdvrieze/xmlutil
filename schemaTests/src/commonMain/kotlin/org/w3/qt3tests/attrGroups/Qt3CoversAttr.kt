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

package org.w3.qt3tests.attrGroups

import io.github.pdvrieze.xml.schematypes.values.XsdToken

interface Qt3CoversAttr {
    /**
     * An attribute used to cross-reference tests or test-sets to the changes in the specification
     * that the tests are designed to cover.
     *
     * The value is syntactically similar to an xs:IDREFS value, in that it contains a space-separated
     * list of change identifiers; however it is not actually an `xs:IDREFS` value, because the identifiers are
     * in a different XML document, specifically the identifiers of changes appearing in the `changes.xml` file.
     */
    val covers: List<XsdToken>?
}

