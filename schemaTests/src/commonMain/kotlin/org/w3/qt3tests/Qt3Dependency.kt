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

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.w3.qt3tests.attrGroups.Qt3TypeAttr
import org.w3.qt3tests.attrGroups.Qt3ValueAttr

/**
 * Indicates a dependency which must be satisfied in order for a test to be run.
 * 
 * A dependency may be associated with an individual test case or with a test-set. A dependency at
 * the level of a test-set applies to all test cases in that test-set. 
 *     
 * The attribute setting `satisfied="false"` indicates that the test should only be run if the
 * dependency is NOT satisfied.
 * 
 * The set of recognized values appearing in the `value` attribute depends on the content of the
 * `type` attribute.
 *
 * The most commonly-used dependency is on the version of XPath or XQuery. This is represented by
 * a dependency with `type="spec"` whose corresponding value is, for example `value="XQ10+ XP30+"`
 * which indicates that the test can be run with XQuery 1.0 or later, or XPath 3.0 or later. A
 * test with `value="XQ10"` should be run with an XQuery 1.0 processor only (typically, an
 * XQuery 3.0 processor will produce a different result, described in a separate test case.)
 * 
 * @property satisfied The default value "`true`" indicates that the dependency must be satisified
 * for the test to run.
 *                         
 * The setting "false" indicates that the test should only be run if the dependency is NOT
 * satisfied. For example, this might be used in a test to show what happens if a language
 * (such as `lang="jp"` is requested and the processor does not support that language.
 */
@Serializable
@XmlSerialName("dependency", QT3TNS)
class Qt3Dependency(
    @XmlElement(false) override val type: Qt3DependencyType? = null,
    override val value: String? = null,
    val satisfied: Boolean = true,
): Qt3TypeAttr, Qt3ValueAttr {

}
