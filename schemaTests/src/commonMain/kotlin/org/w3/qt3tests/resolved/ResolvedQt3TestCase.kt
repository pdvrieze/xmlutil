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

package org.w3.qt3tests.resolved

import io.github.pdvrieze.formats.xpath.XPathExpression
import io.github.pdvrieze.xml.schematypes.values.XsdNCName
import io.github.pdvrieze.xml.schematypes.values.XsdToken
import org.w3.qt3tests.*

class ResolvedQt3TestCase(
    val description: Qt3Description? = null,
    val created: Qt3Created? = null,
    val modified: List<Qt3Modified> = emptyList(),
    val environment: Result<ResolvedQt3Environment>? = null,
    val modules: List<Qt3Module> = emptyList(),
    val dependencies: List<Qt3Dependency> = emptyList(),
    val test: ResolvedQt3Test/*? = null*/,
    val result: ResolvedQt3Result? = null,
    val name: String,
    val covers: List<XsdToken>? = emptyList(),
    val covers30: List<XsdNCName>? = emptyList(),
) {
    fun tryVerify() {
        val errorAssertions = buildList {
            if (result != null) {
                for(r in result.assertions) r.expectedErrors(this)
            }
        }

        when (errorAssertions.size) {
            0 -> {
                val t= checkNotNull (test) { "No test found for test case $name" }
                if (!t.expr.isSuccess) {
                    throw IllegalStateException("Test case $name should have passed", t.expr.exceptionOrNull())
                }
            }

            // XPST0003 is a parser error. We should be able to handle those (only)
            else if (errorAssertions.any { it.code?.startsWith("XPST0003") == true }) -> {
                val expr = test.expr
                if (expr.isSuccess) {
                    val e = expr.getOrThrow()
                    if (e is XPathExpression) {
                        throw IllegalStateException("${name}: Expression '${e.xmlString}' should fail. with code ${errorAssertions.map { it.code }}")
                    }
                }
            }
        }
    }

    fun neededFeatures(): List<String> {
        return dependencies.asSequence()
            .filter { it.type == Qt3DependencyType.FEATURE && it.satisfied }
            .mapNotNullTo(ArrayList()) { it.value }
    }

}
