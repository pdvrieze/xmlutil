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

package io.github.pdvrieze.formats.xpath

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.isNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.isNameChar
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.isNameStartChar
import io.github.pdvrieze.formats.xpath.impl.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML

@OptIn(XPathInternal::class)
@Serializable(XPathExpression.Serializer::class)
class XPathExpression private constructor(
    override val xmlString: String,
    @XPathInternal
    internal val expr: Expr
): VToken {

    companion object Serializer : KSerializer<XPathExpression> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.XPathExpression",
            PrimitiveKind.STRING
        )

        override fun serialize(encoder: Encoder, value: XPathExpression) {
            if (encoder is XML.XmlOutput) {
                // todo ensure prefixes exist encoder.target
            }
            return encoder.encodeString(value.xmlString) // TODO use xml aware writing
        }

        override fun deserialize(decoder: Decoder): XPathExpression {
            val nsContext = (decoder as? XML.XmlInput)?.input?.namespaceContext ?: SimpleNamespaceContext()
            return invoke(xmlTrimWhitespace(decoder.decodeString()), nsContext)
        }

        operator fun invoke(
            path: String,
            namespaceContext: NamespaceContext = SimpleNamespaceContext()
        ): XPathExpression {
            val parser = Parser(xmlTrimWhitespace(path), namespaceContext)
            return XPathExpression(path, parser.parse())
        }


    }

    private class Parser(private val str: String, private val namespaceContext: NamespaceContext) {
        var i: Int = 0

        fun parsePathExpr(): Expr {
            var current: Expr? = null

            skipWhitespace()
            var start = i

            while (i < str.length) {
                when (val c = str[i]) {
                    ' ' -> {} // ignore
                    '$' -> current = parseVariableReference()

                    '|' -> current = BinaryExpr(
                        Operator.UNION,
                        requireNotNull(current) { "@$i> Path expressions can not start with |" },
                        parsePathExpr()
                    )

                    ')' -> {
                        throw IllegalArgumentException("@$i> Unexpected ')' in xpath expression")
                    }

                    '(' -> {
                        ++i
                        current = parseExpr(true)
                        while (isXmlWhitespace(str[i]) && (i + 1 < str.length)) {
                            ++i
                        }
                        require(str[i] == ')') { "@$i> Missing closing parenthesis" }
                    }

                    '.',
                    '/',
                    '@' -> current = parseLocationPath()

                    else -> {
                        if (c.isLetter() && current == null) {
                            current = parseLocationPath()
                        } else {
                            throw IllegalArgumentException("@$i> Unexpected token '$c' in xpath expression")
                        }
                    }
                }


                ++i
            }
            return requireNotNull(current) { "No path found in expression" }
        }

        private fun skipWhitespace() {
            while (i < str.length && isXmlWhitespace(str[i])) {
                ++i
            }
        }

        private fun parseVariableReference(): VariableRef {
            TODO("not implemented")
        }

        private fun parseExpr(doSkipWhitespace: Boolean): Expr {
            if (doSkipWhitespace) skipWhitespace()

            val current: Expr

            require(i < str.length) { "Empty expression" }
            val c = str[i]
            when {
                c == '/' || c == '@' || c == '.' || c == '*' -> {
                    current = parseLocationPath()
                }

                c == '(' -> {
                    ++i
                    current = ParenExpr(parseExpr(true))
                    skipWhitespace()
                    require(checkCurrent(')')) { "@$i> missing closing )" }
                    ++i
                }

                c == '-' || c.isDigit() -> {
                    current = parseNumber()
                }

                isNameStartChar(c) -> {
                    val p = parseLocationPath()
                    current = when {
                        p.steps.isEmpty() -> checkNotNull(p.primaryExpr)
                        else -> p
                    }
                }

                else -> throw IllegalArgumentException("@$i> Unexpected character '${str[i]}' in expression")
            }
            skipWhitespace()
            if (i >= str.length) return current
            when (str[i++]) {
                '|' -> return BinaryExpr.priority(Operator.UNION, current, parseExpr(true))

                '=' -> return BinaryExpr.priority(Operator.EQ, current, parseExpr(true))

                '!' -> return BinaryExpr.priority(Operator.NEQ, current, parseExpr(true))

                '<' -> return when {
                    checkCurrent('=') -> BinaryExpr.priority(Operator.LE, current, parseExpr(true))
                    else -> BinaryExpr.priority(Operator.LT, current, parseExpr(true))
                }

                '>' -> return when {
                    checkCurrent('=') -> BinaryExpr.priority(Operator.GE, current, parseExpr(true))
                    else -> BinaryExpr.priority(Operator.GT, current, parseExpr(true))
                }

                '+' -> return BinaryExpr.priority(Operator.ADD, current, parseExpr(true))

                '-' -> return BinaryExpr.priority(Operator.SUB, current, parseExpr(true))

                'o' -> {
                    require(checkCurrentWord("r"))
                    ++i
                    return BinaryExpr.priority(Operator.OR, current, parseExpr(true))
                }

                'a' -> {
                    require(checkCurrentWord("nd"))
                    i += 2
                    return BinaryExpr.priority(Operator.AND, current, parseExpr(true))
                }

                'm' -> {
                    require(checkCurrentWord("od"))
                    i += 2
                    return BinaryExpr.priority(Operator.MOD, current, parseExpr(true))
                }

                'd' -> {
                    require(checkCurrentWord("div"))
                    i += 2
                    return BinaryExpr.priority(Operator.DIV, current, parseExpr(true))
                }
            }
            --i
            return current
//            throw IllegalArgumentException("@${i}> Trailing content at end of expression: ${str.substring(i)}")
        }

        fun parse(): Expr {
            val e = parseExpr(true)
            skipWhitespace()
            if (i<str.length) throw IllegalArgumentException("@$i> Trailing content in expression: '${str.substring(i)}'")
            return e
        }

        private fun parseLocationPath(): LocationPath {
            val start = i
            var rooted = false
            val steps = mutableListOf<Step>()
            var primaryExpr: Expr? = null

            while (i < str.length) {
                val c = str[i]
                when {
                    c == ' ' -> ++i // ignore

                    c == '.' -> {
                        require(primaryExpr == null)
                        ++i
                        val axis = when {
                            checkCurrent('.') -> Axis.PARENT
                            else -> Axis.SELF
                        }
                        steps.add(Step(axis, NodeTest.NodeTypeTest(NodeType.NODE)))
                    }

                    c == '/' -> {
                        if (start == i) rooted = true

                        ++i
                        if (checkCurrent('/')) { // shortcut
                            ++i
                            steps.add(Step(Axis.DESCENDANT_OR_SELF, NodeTest.NodeTypeTest(NodeType.NODE)))
                        } else {
                            skipWhitespace()
                            val exprStart = i
                            when (val s = parseStep(steps.size)) {
                                is Step -> steps.add(s)
                                is Primary -> {
                                    require(primaryExpr == null && steps.isEmpty()) { "@$exprStart, expression as path step is not valid" }
                                    primaryExpr = s.expr
                                }
                            }
                        }
                    }

                    c == '(' -> {
                        require(primaryExpr == null && steps.isEmpty()) { "Primary expression in invalid point" }
                        ++i
                        primaryExpr = parseExpr(true)
                        skipWhitespace()
                        require(checkCurrent(')')) { "@$i> Expression not ended by ')'" }
                        ++i
                    }

                    c.isDigit() -> {
                        require(primaryExpr == null && steps.isEmpty()) { "Primary expression in invalid point" }
                        primaryExpr = parseNumber()
                        skipWhitespace()
                    }

                    isNameStartChar(c) ||
                            c == '*' ||
                            c == '@' -> { //attribute
                        val exprStart = i
                        when (val s = parseStep(steps.size)) {
                            is Step -> steps.add(s)
                            is Primary -> {
                                require(primaryExpr == null && steps.isEmpty()) { "@$exprStart, expression as path step is not valid" }
                                primaryExpr = s.expr
                            }
                        }
                    }

                    else -> break
                }
            }
            return LocationPath(rooted, primaryExpr, steps)
        }

        private fun parseLiteral(): StringLiteral {
            val start = i
            val delim = when (str[i]) {
                '\'' -> '\''
                '"' -> '"'
                else -> throw IllegalArgumentException("@$i> Literal does not start with quote, but with '${str[i]}'")
            }
            ++i
            while (i < str.length && str[i] != delim) {
                ++i
            }
            require(i < str.length) { "@$i> Literal string not closed" }
            return StringLiteral(str.substring(start, i)).also { ++i } // skip delim
        }

        private fun parseNumber(): NumberLiteral {
            val start = i

            if (str[i] == '-') ++i

            require(i < str.length && str[i].isDigit()) { "@$start> '${str.substring(start, i)}' not a number" }

            var seenPeriod = false
            while (i < str.length) {
                when (str[i]) {
                    '.' -> when {
                        seenPeriod -> return NumberLiteral(str.substring(start, i).toLong())
                        else -> seenPeriod = true
                    }

                    !in '0'..'9' -> return NumberLiteral(str.substring(start, i).toLong())
                }
                ++i
            }
            return NumberLiteral(str.substring(start, i).toLong())
        }

        private fun parseStep(stepCount: Int): PrimaryOrStep {
            var start = i

            var currentAxis: Axis? = null
            var currentTest: NodeTest? = null
            var currentPrefix: String? = null
            var parsePos = -1

            fun finaliseName(): Primary? {
                require(i>start) { "@$start> Empty name in path step"}
                val localName = str.substring(start, i)
                skipWhitespace()
                when {
                    currentAxis == null && parsePos<=0 && checkCurrent("::") -> {
                        currentAxis = Axis.from(localName)
                        i += 2
                        skipWhitespace()
                        start = i
                        parsePos = 1
                        return null
                    }

                    checkCurrent(':') -> {
                        require(currentPrefix==null)
                        require(parsePos<=1)
                        currentPrefix = localName
                        ++i
                        start = i
                        parsePos = 2
                        return null
                    }

                    checkCurrent('(') -> {
                        ++i
                        skipWhitespace()
                        val nodeType = if (currentPrefix == null) NodeType.maybeValueOf(localName) else null
                        if (nodeType!=null) {
                            check(currentAxis==null)
                            currentAxis = Axis.CHILD

                            if (checkCurrent(')')) {
                                currentTest = NodeTest.NodeTypeTest(nodeType)
                            } else {
                                require(nodeType == NodeType.PROCESSING_INSTRUCTION)
                                currentTest = NodeTest.ProcessingInstructionTest(parseLiteral().value)
                                require(checkCurrent(')'))
                            }
                            ++i // closing )
                        } else {
                            require(currentAxis==null) { "Primary functions ($localName) can not be in a path with axis" }
                            val ns = lookupNamespace(currentPrefix)
                            val name = QName(ns, localName, currentPrefix?:"")
                            val args = mutableListOf<Expr>()

                            while (! checkCurrent(')')) {
                                args.add(parseExpr(true))
                                skipWhitespace()
                                if (i >= str.length || str[i].let { it != ',' && it != ')' }) {
                                    throw IllegalArgumentException("@$i> Unexpected token in arguments)")
                                }
                            }
                            ++i // last)
                            return Primary(FunctionCall(name, args))
                        }
                        parsePos = 3
                    }
                    else -> {
                        check(i>=str.length || !isNameChar(str[i])) { "@$i> Not finished name (${str[i]}): $str"}
                        // can be already set
                        if (currentAxis==null) currentAxis = Axis.CHILD

                        val ns = lookupNamespace(currentPrefix)
                        val name = QName(ns, localName, currentPrefix?:"")
                        currentTest = NodeTest.QNameTest(name)
                        parsePos = 3
                    }
                }



                return null
            }

            val currentPredicates: MutableList<Expr> = mutableListOf()

            /*
             * Parse positions:
             * -1   -   Before start
             * 0    -   Reading characters that could be axis selector
             * 1    -   After axis selector
             * 2    -   A prefix has been read
             * 3    -   NodeTest has been read.
             */

            while (i < str.length) {
                val c = str[i]
                when {
                    c == ' ' -> {
                        finaliseName()?.let { return it }
                    } // ignore

                    c == '/' && i == start -> {
                        ++i
                        return Step(Axis.DESCENDANT_OR_SELF, NodeTest.NodeTypeTest(NodeType.NODE))
                    }

                    c == '/' -> { // step finished
                        break
                    }

                    c == '@' -> { //attribute
                        require(parsePos < 1 && i == start) { "@$i> attribute out of scope" }
                        parsePos = 1
                        ++i
                        start = i
                        currentAxis = Axis.ATTRIBUTE
                    }

                    c == ':' -> when {
                        i + 1 < str.length && str.get(i + 1) == ':' -> {
                            require(parsePos < 1) { "Multiple axis selectors" }
                            parsePos = 1
                            require(currentAxis == null) { "Multiple axes in xpath" }
                            currentAxis = Axis.from(str.substring(start, i))
                            i+=2 // skip extra character
                            skipWhitespace()
                            start = i
                        }

                        else -> {
                            require(currentPrefix == null) { "QName can only have 1 prefix" }
                            currentPrefix = str.substring(start, i).also {
                                require(it.isNCName()) { "@$start> '$it' is not a valid NCName/prefix" }
                            }
                            ++i
                            start = i
                        }
                    }

                    c == '*' -> when {
                        parsePos >= 3 ||
                                i != start -> throw IllegalArgumentException("@$i> * in unexpected location")

                        currentTest != null -> throw IllegalArgumentException("Repeated test")

                        parsePos <= 0 -> {
                            currentAxis = Axis.CHILD
                            currentTest = NodeTest.AnyNameTest
                            parsePos = 3
                            ++i
                        }

                        parsePos == 1 -> {
                            currentTest = NodeTest.AnyNameTest
                            parsePos = 3
                            ++i
                        }

                        else -> {
                            check(str[i - 1] == ':') { "Should not happen" }
                            val prefix = VNCName(str.substring(start, i - 1))

                            val ns = requireNotNull(namespaceContext.getNamespaceURI(prefix.xmlString)) { "@$start> Missing namespace for '$prefix'" }
                            currentTest = NodeTest.NSTest(ns.toAnyUri(), prefix)
                            parsePos = 3
                            ++i
                        }
                    }

                    c == '(' -> {
                        finaliseName()?.let { return it }
                        // will have created the function call if relevant
                    }

                    c == '[' -> {
                        finaliseName()?.let { return it } // this will take care of predicates

                        ++i

                        currentPredicates.add(parseExpr(true))
                        skipWhitespace()
                        require(checkCurrent(']')) { "@$i> predicate not closed by ']'" }
                        ++i
                    }

                    isNameStartChar(c) -> {
                        if (parsePos < 0) parsePos = 0
                        ++i
                    }

                    start != i && isNameChar(c) -> ++i
                    c == '|' -> {
                        break
                    }

                    else -> {
                        throw IllegalArgumentException("@$i> Unexpected token '${c}' in '$str'")
                    }
                }
            }
            if (parsePos<3 && currentTest==null) {
                finaliseName()?.let { return it }
            }

            return Step(checkNotNull(currentAxis){ "missing axis" }, checkNotNull(currentTest){ "missing test" }, currentPredicates)
        }

        fun checkCurrent(char: Char): Boolean {
            if (i >= str.length) return false
            return str[i] == char
        }

        fun checkCurrent(check: String): Boolean {
            val end = i + check.length
            if ((end + 1) >= str.length) return false
            return str.substring(i, end) == check
        }

        fun checkCurrentWord(check: String): Boolean {
            checkCurrent(check)
            return i >= str.length || !isNameChar(str[i])
        }

        fun lookupNamespace(prefix: String?): String = when (prefix){
            "",
            null -> namespaceContext.getNamespaceURI("") ?: ""
            else -> requireNotNull(namespaceContext.getNamespaceURI(prefix)) {
                "Missing namespace for prefix $prefix"
            }
        }

    }

}
