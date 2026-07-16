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

package org.w3.qt3tests.test

import io.github.pdvrieze.formats.xpath.XPathExpression
import io.github.pdvrieze.formats.xpath.eval.data.XdmNodeOld
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.EvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.formatters.DecimalFormat
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.dom2.Document
import org.junit.jupiter.api.Named
import org.opentest4j.AssertionFailedError
import org.w3.qt3tests.Qt3Dependency
import org.w3.qt3tests.Qt3DependencyType
import org.w3.qt3tests.Qt3SpecDependency
import org.w3.qt3tests.resolved.ResolvedQt3TestCase
import org.w3.qt3tests.resolved.assertions.AssertionResult
import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName

@OptIn(XPathInternal::class)
abstract class AbstractTestSetSuite {

    @IgnorableReturnValue
    protected fun testEvalTestCaseImpl(testCase: ResolvedQt3TestCase): Result<XdmValue<*>> {
        val environment = testCase.environment?.getOrThrow()
        val contextDoc: Document? = environment?.getDocumentOrNull()

        val context = contextDoc?.let { XdmNodeOld(it.documentElement!!) }

        var decimalFormat = DecimalFormat()
        val namedDecimalFormats = mutableListOf<DecimalFormat.Named>()
        var nsContext: NamespaceContext = SimpleNamespaceContext()
        val vars = mutableMapOf<String, MutableMap<String, XdmValue<*>>>()
        if (environment != null) {
            nsContext = environment.getNsContext()

            for (decFormat in environment.decimalFormats) {
                val name = decFormat.name
                if (name == null) {
                    decimalFormat = decFormat.toDecimalFormat()
                } else if (name.prefix.isEmpty() && name.namespaceURI.isNotEmpty()) {
                    val decFormat = decFormat.toDecimalFormat(QName(name.localPart))
                    namedDecimalFormats.add(decFormat)
                } else {
                    namedDecimalFormats.add(decFormat.toDecimalFormat() as DecimalFormat.Named)
                }
            }

            for (param in environment.params) {
                val select = param.select ?: continue
                val value = XPathExpression(select).eval(context, nsContext, vars)
                val nsUri = param.name.namespaceURI
                if (param.name.prefix.isEmpty()) {
                    (vars.getOrPut("") { mutableMapOf() })[param.name.localPart] = value
                }
                if (nsUri.isNotEmpty() || param.name.prefix.isNotEmpty()) {
                    (vars.getOrPut(nsUri) { mutableMapOf() })[param.name.localPart] = value
                }
            }
        }

        val deterministicState = EvalContext.DeterministicState(
            defaultDecimalFormat = decimalFormat,
            decimalFormats = namedDecimalFormats
        )

        val testExpression = testCase.test.expr.getOrThrow() as XPathExpression

        val evalResult = runCatching {
            testExpression.eval(context, nsContext, vars, deterministicState)
        }

        if (testCase.result != null) {
            for (a in testCase.result.assertions) {
                val verifyResult = a.verify(evalResult, testCase)
                if (verifyResult is AssertionResult.Failure) {
                    if (evalResult.isFailure) throw AssertionFailedError(
                        "Unexpected failure[${testCase.name}]: ${verifyResult.error}",
                        verifyResult.cause
                    ) else throw AssertionFailedError(
                        "Unexpected assertion failure for result[${testCase.name}]: ${verifyResult.error}",
                        verifyResult.cause
                    )
                }
            }
        } else if (evalResult.isFailure) throw evalResult.exceptionOrNull()!!
        return evalResult
    }

    abstract class CompanionBase(val testSetName: String) {
        fun getTestCase(name: String): ResolvedQt3TestCase {
            val cases = getTestCases(testSetName).filter { it.name == name }
            require(cases.size == 1) { "Unexpected test count (#${cases.size}) for name '$name' in '$testSetName'" }
            return cases.single().payload
        }

        abstract fun getTestCases(): List<Named<ResolvedQt3TestCase>>
    }

    companion object {

        val testCases = mutableMapOf<String, List<Named<ResolvedQt3TestCase>>>()

        @JvmStatic
        fun getTestCases(testSetName: String): List<Named<ResolvedQt3TestCase>> {
            val overrides = TestParseCatalog.overrides
            return testCases.getOrPut(testSetName) {
                val testSet = TestParseCatalog.parseTestSetImpl(TestParseCatalog.getTestSetSpec(testSetName))
                testSet.testCases.asSequence()
                    .filter { it.test.expr.getOrNull() is XPathExpression }
                    .filter { tc ->
                        overrides.overrides.none { o ->
                            val p = o.path
                            p.testSet == testSetName && p.group == null || p.group == tc.name || p.test == tc.name
                        }
                    }
                    .filter { it.dependencies.all { d ->  supportsDependency(d) } }
                    .map { Named.named(it.name, it) }
                    .toList()
            }
        }

        fun supportsDependency(dep: Qt3Dependency) : Boolean {
            // spec dependencies are already handled
            when (dep) {
                is Qt3Dependency.Generic -> {
                    return when (dep.type) {
                        Qt3DependencyType.CALENDAR -> when (dep.value){
                            "ISO", "AD" -> dep.satisfied
                            else -> !dep.satisfied
                        }
                        Qt3DependencyType.COLLECTION_STABILITY -> !dep.satisfied
                        Qt3DependencyType.DEFAULT_LANGUAGE -> !dep.satisfied
                        Qt3DependencyType.DIRECTORY_AS_COLLECTION_URI -> !dep.satisfied
                        Qt3DependencyType.FEATURE -> when (dep.value) {
                            "namespace-axis" -> !dep.satisfied
                            else -> !dep.satisfied //
                        }

                        Qt3DependencyType.FORMAT_INTEGER_SEQUENCE -> !dep.satisfied
                        Qt3DependencyType.LANGUAGE -> when {
                            dep.value.lowercase().startsWith("en") -> dep.satisfied
                            else -> !dep.satisfied
                        }

                        Qt3DependencyType.LIMITS -> dep.satisfied
                        Qt3DependencyType.SPEC -> error("Should not be a spec dependency")
                        Qt3DependencyType.SCHEMAAWARE -> !dep.satisfied
                        Qt3DependencyType.UNICODE_NORMALIZATION_FORM -> !dep.satisfied
                        Qt3DependencyType.UNICODE_VERSION -> !dep.satisfied
                        Qt3DependencyType.XML_VERSION -> dep.satisfied
                        Qt3DependencyType.XSD_VERSION -> when (dep.value) {
                            "1.1" -> dep.satisfied // We support 1.1 only
                            else -> ! dep.satisfied
                        }
                    }
                }

                is Qt3SpecDependency -> {
                    if (dep.xpathVersions().isNotEmpty()) return dep.satisfied
                    else return !dep.satisfied
                }
            }
        }

    }
}
