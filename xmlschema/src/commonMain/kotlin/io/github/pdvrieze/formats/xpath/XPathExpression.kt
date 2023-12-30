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
) : VToken {

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
                        current = parseExpr()
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
            while (i < str.length) {
                val c = str[i]
                when {
                    c=='(' && peekCurrent("(:")-> parseComment()

                    !isXmlWhitespace(c) -> return
                    else -> ++i
                }
            }
        }

        private fun parseComment() {
            val start = i
            check(tryCurrent("(:"))
            while (i < str.length) {
                val c = str[i]
                when {
                    c == ':' && tryCurrent(":)") -> return
                    c == '(' && peekCurrent("(:") -> parseComment()
                    else -> ++i
                }
            }
            throw IllegalArgumentException("@$i> Comment not closed: ${str.substring(start)}")
        }

        /**
         * Parse a word (not allowing ':' letters)
         */
        private fun parseNCName(): String = buildString {
            if (i >= str.length || !isNameStartChar(str[i])) return@buildString

            append(str[i++])
            while (i < str.length && (str[i]!=':' && isNameChar(str[i]))) {
                append(str[i++])
            }
        }

        private fun parseQName(): QName {
            val prefixOrLocal = parseNCName()
            return when {
                tryCurrent(':') ->
                    QName(lookupNamespace(prefixOrLocal), parseNCName(), prefixOrLocal)

                else -> QName(lookupNamespace(""), prefixOrLocal, "")
            }
        }

        private fun parseItemType(): ItemType {
            val localOrPrefix = parseNCName()
            if (localOrPrefix=="item") {
                skipWhitespace()
                require(tryCurrent('('))

                skipWhitespace()
                require(tryCurrent(')'))

                return ItemType.ItemTest
            }
            val nodeType = NodeType.maybeValueOf(localOrPrefix)
            if (nodeType != null) {
                return NodeTest.NodeTypeTest(nodeType, parseArgs())
            }
            if (tryCurrent(':')) {
                val localName = parseNCName()
                return ItemType.AtomicType(QName(lookupNamespace(localOrPrefix), localName, localOrPrefix))
            } else {
                return ItemType.AtomicType(QName(lookupNamespace(""), localOrPrefix, ""))
            }
        }

        private fun parseSequenceType(): SequenceType {
            if (tryCurrentWord("empty-sequence")) {
                skipWhitespace()
                require(tryCurrent('('))
                skipWhitespace()
                require(tryCurrent(')'))
                return SequenceType.EmptySequence
            }
            val itemType = parseItemType()
            skipWhitespace()
            val occurrence = when(peekCurrent()) {
                '?' -> {
                    ++i
                    SequenceType.OccurrenceType.OPTIONAL
                }
                '*' -> {
                    ++i
                    SequenceType.OccurrenceType.ANY
                }
                '+' -> {
                    ++i
                    SequenceType.OccurrenceType.AT_LEAST_ONE
                }
                else -> SequenceType.OccurrenceType.SINGLE
            }
            return SequenceType.ItemSequence(itemType, occurrence)
        }

        private fun parseVariableReference(): VariableRef {
            require(tryCurrent('$'))
            return VariableRef(parseNCName())
        }

        private fun parseExpr(): Expr {
            skipWhitespace()

            var current: Expr

            require(i < str.length) { "Empty expression" }
            val c = str[i]
            when {
                c == '/' || c == '@' || c == '.' || c == '*' || c == '$' ->
                    current = parseLocationPath()

                c == '(' -> current = parseSequenceOrParen()

                c == '\'' || c=='"' -> current = parseLiteral()

                c == '-' || c.isDigit() -> current = parseNumber()

                isNameStartChar(c) -> {
                    when {
                        tryCurrentWord("every") -> {
                            skipWhitespace()
                            current = parseQuantifiedExpr(QuantifiedExpr.Kind.EVERY)
                        }

                        tryCurrentWord("some") -> {
                            skipWhitespace()
                            current = parseQuantifiedExpr(QuantifiedExpr.Kind.SOME)
                        }

                        else -> {
                            val p = parseLocationPath()
                            current = when {
                                p.steps.isEmpty() -> checkNotNull(p.primaryExpr)
                                else -> p
                            }
                        }
                    }

                }

                else -> throw IllegalArgumentException(
                    "@$i> Unexpected character '${str[i]}' in expression - '${str.substring(i)}'"
                )
            }

            skipWhitespace()
            while (i < str.length) {
//            if (i >= str.length) return current
                when (str[i]) {
                    '|' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.UNION, current, parseExpr())
                    }

                    '=' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.EQ, current, parseExpr())
                    }

                    '!' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.NEQ, current, parseExpr())
                    }

                    '<' -> current = when {
                        tryCurrent("<=") -> BinaryExpr.priority(Operator.LE, current, parseExpr())

                        tryCurrent("<<") -> BinaryExpr.priority(Operator.PRECEDES, current, parseExpr())

                        else -> {
                            ++i
                            BinaryExpr.priority(Operator.LT, current, parseExpr())
                        }
                    }

                    '>' -> current = when {
                        tryCurrent(">=") -> BinaryExpr.priority(Operator.GE, current, parseExpr())

                        tryCurrent(">>") -> BinaryExpr.priority(Operator.FOLLOWS, current, parseExpr())

                        else -> {
                            ++i
                            BinaryExpr.priority(Operator.GT, current, parseExpr())
                        }
                    }

                    '+' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.ADD, current, parseExpr())
                    }

                    '-' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.SUB, current, parseExpr())
                    }

                    'a' -> {
                        if (!tryCurrentWord("and")) return current

                        current = BinaryExpr.priority(Operator.AND, current, parseExpr())
                    }

                    'c' -> {
                        when {
                            tryCurrentWord("cast") -> {
                                skipWhitespace()
                                require(tryCurrentWord("as"))
                                skipWhitespace()
                                current = CastExpr(current, parseQName())
                            }

                            tryCurrentWord("castable") -> {
                                skipWhitespace()
                                require(tryCurrentWord("as"))
                                skipWhitespace()
                                val typeName = parseQName()
                                skipWhitespace()
                                val allowsEmpty = tryCurrent('?')
                                current = CastableExpr(current, typeName, allowsEmpty)
                            }
                            else -> return current
                        }
                    }

                    'd' -> {
                        if (!tryCurrentWord("div")) return current

                        current = BinaryExpr.priority(Operator.DIV, current, parseExpr())
                    }

                    'e' -> {
                        if (!tryCurrentWord("eq")) return current

                        current = BinaryExpr.priority(Operator.VAL_EQ, current, parseExpr())
                    }

                    'g' -> {
                        when {
                            tryCurrentWord("ge") ->
                                current = BinaryExpr.priority(Operator.VAL_GE, current, parseExpr())

                            tryCurrentWord("gt") ->
                                current = BinaryExpr.priority(Operator.VAL_GT, current, parseExpr())

                            else -> return current
                        }
                    }

                    'i' -> {
                        when {
                            tryCurrentWord("instance") -> {
                                skipWhitespace()
                                tryCurrentWord("of")
                                skipWhitespace()
                                current = InstanceOfExpr(current, parseSequenceType())
                            }

                            tryCurrentWord("idiv") ->
                                current = BinaryExpr.priority(Operator.IDIV, current, parseExpr())

                            tryCurrent("is") ->
                                current = BinaryExpr.priority(Operator.IS, current, parseExpr())

                            else -> return current
                        }
                    }

                    'l' -> {
                        when {
                            tryCurrentWord("le") ->
                                current = BinaryExpr.priority(Operator.VAL_LE, current, parseExpr())

                            tryCurrentWord("lt") ->
                                current = BinaryExpr.priority(Operator.VAL_LT, current, parseExpr())

                            else -> return current
                        }
                    }

                    'm' -> {
                        if(!tryCurrentWord("mod")) return current
                        current = BinaryExpr.priority(Operator.MOD, current, parseExpr())
                    }

                    'n' -> {
                        if (!tryCurrentWord("ne")) return current
                        current = BinaryExpr.priority(Operator.VAL_NEQ, current, parseExpr())
                    }

                    'o' -> {
                        if (!tryCurrentWord("or")) return current
                        current = BinaryExpr.priority(Operator.OR, current, parseExpr())
                    }

                    't' -> {
                        if (!tryCurrentWord("to")) return current
                        current = BinaryExpr.priority(Operator.RANGE, current, parseExpr())
                    }

                    else -> return current //no expression elements

                }
                skipWhitespace()
            }
            return current
        }

        private fun parseSequenceOrParen(): Expr {
            require(str[i] == '(')
            val c: Expr
            ++i
            val expr = parseExpr()
            skipWhitespace()
            require(i < str.length) { "@$i> missing closing )" }
            if (str[i] != ')') {
                val elements = mutableListOf(expr)
                do {
                    require(str[i] == ',') { "@$i> Invalid character '${str[i]}' in range expression: '${str.substring(i)}'" }
                    ++i
                    skipWhitespace()
                    elements.add(parseExpr())
                    require(i < str.length) { "@$i> missing closing )" }
                } while (str[i] != ')')
                c = SequenceExpr(elements)
            } else {
                c = expr as? ParenExpr ?: ParenExpr(expr)
            }
            ++i
            return c
        }

        private fun parseQuantifiedExpr(kind: QuantifiedExpr.Kind): Expr {
            val oldStart = i - kind.literal.length
            skipWhitespace()

            // Allow for matching a location part with quantifier name
            if (!peekCurrent('$')) {
                i = oldStart; return parseLocationPath()
            }

            val varName = parseVariableReference()
            skipWhitespace()

            require(tryCurrentWord("in")) { "@$i> Missing 'in' in quantified expression '${str.substring(i)}'" }
            skipWhitespace()

            val exprs = mutableListOf<Expr>()
            exprs.add(parseExpr())
            while (tryCurrent(',')) {
                skipWhitespace()
                exprs.add(parseExpr())
            }
            val source = exprs.singleOrNull() ?: SequenceExpr(exprs)
            skipWhitespace()

            require(tryCurrentWord("satisfies")) { "@$i> Missing satisfies in quantified expression: ${str.substring(i)}" }

            skipWhitespace()

            val condition = parseExpr()
            skipWhitespace()

            return QuantifiedExpr(kind, varName.varName, source, condition)
        }

        fun parse(): Expr {
            val e = parseExpr()
            skipWhitespace()
            if (i < str.length) throw IllegalArgumentException("@$i> Trailing content in expression: '${str.substring(i)}'")
            return e
        }

        private fun parseLocationPath(): LocationPath {
            val start = i
            var rooted = false
            val steps = mutableListOf<Step>()
            var primaryExpr: Expr? = null
            skipWhitespace()
            while (i < str.length) {
                val c = str[i]
                when {
//                    c == ' ' -> ++i // ignore

                    c == '.' -> { // TODO can use step parsing
                        require(primaryExpr == null)
                        ++i
                        val axis = when {
                            tryCurrent('.') -> Axis.PARENT
                            else -> Axis.SELF
                        }
                        steps.add(Step(axis, NodeTest.NodeTypeTest(NodeType.NODE)))
                    }

                    c == '$' -> {
                        require(primaryExpr == null)
                        primaryExpr = parseVariableReference()
                    }

                    c == '/' -> {
                        if (start == i) rooted = true

                        ++i
                        if (tryCurrent('/')) { // shortcut
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
                        primaryExpr = parseExpr()
                        skipWhitespace()
                        require(tryCurrent(')')) { "@$i> Expression not ended by ')'" }
                    }

                    c.isDigit() -> {
                        require(primaryExpr == null && steps.isEmpty()) { "Primary expression in invalid point" }
                        primaryExpr = parseNumber()
                    }

                    isNameStartChar(c) ||
                            c == '*' ||
                            c == '@' -> { //attribute
                        val exprStart = i
                        when (val s = parseStep(steps.size)) {
                            is Step -> steps.add(s)
                            is Primary -> {
                                require(primaryExpr == null) {
                                    "@$exprStart, expression as path step is not valid (primary expression already set: $primaryExpr) - '${
                                        str.substring(
                                            exprStart
                                        )
                                    }'"
                                }
                                require(steps.isEmpty()) {
                                    "@$exprStart, expression as path step is not valid (steps already provided: " +
                                            "${steps.joinToString()}) - '${str.substring(exprStart)}'"
                                }
                                primaryExpr = s.expr
                            }
                        }
                    }

                    else -> break
                }
                skipWhitespace()
                if (! tryCurrent('/')) break

                skipWhitespace()
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
            skipWhitespace()
            if (i>=str.length) { "@$i> Empty expression" }

            val c = str[i]
            when {
                c == '/' -> { // special case for "empty" expression
//                    ++i
                    return Step(Axis.DESCENDANT_OR_SELF, NodeTest.NodeTypeTest(NodeType.NODE))
                }

                c == '.' && tryCurrent("..") -> {
                    skipWhitespace()
                    return Step(Axis.PARENT, NodeTest.NodeTypeTest(NodeType.NODE), parsePredicates())
                }

                c == '.' -> {
                    ++i
                    skipWhitespace()
                    return Step(Axis.SELF, NodeTest.NodeTypeTest(NodeType.NODE), parsePredicates())
                }

                c == '@' -> { //attribute
                    return parseAttribute()
                }

                c == '*' -> {
                    ++i
                    skipWhitespace()
                    return Step(Axis.CHILD, NodeTest.AnyNameTest, parsePredicates())
                }

                c == '(' -> {
                    TODO("Sequences are not part of expressions")
//                    finaliseName()?.let { return it }
                    // will have created the function call if relevant
                }

                isNameStartChar(c) -> {
                    val startPos = i
                    val word = parseNCName()

//                    val curName: QName
                    val axis: Axis
                    val nodeTest: NodeTest

                    skipWhitespace()
                    if (tryCurrent("::")) {
                        axis = Axis.from(word)
                        skipWhitespace()
                        nodeTest = requireNotNull(parseNodeTest()) { "@$i> Missing node test in step: '${str.substring(i)}'" }
                    } else {
                        axis = Axis.CHILD
                        nodeTest = requireNotNull(parseNodeTest(word)) { "@$i> Missing node test in step: '${str.substring(i)}'" }
                    }

                    val currentTest: NodeTest
                    if (nodeTest is NodeTest.QNameTest && peekCurrent('(')) {
                        val curName = nodeTest.qName
                        val args = parseArgs()

                        val nodeType =
                            if (curName.namespaceURI.isEmpty()) NodeType.maybeValueOf(curName.localPart) else null
                        when(nodeType) {
                            NodeType.PROCESSING_INSTRUCTION -> {
                                if (args.isEmpty()) {
                                    currentTest = NodeTest.ProcessingInstructionTest()
                                } else if (args.size == 1) {
                                    var arg = args.first()
                                    require(arg is LiteralExpr<*>)
                                    currentTest = NodeTest.ProcessingInstructionTest(arg.value as String)
                                } else throw IllegalArgumentException("Unexpected arguments to processing instruction test")
                            }

                            is NodeType -> {
                                currentTest = NodeTest.NodeTypeTest(nodeType, args)
                            }

                            else -> {
                                return Primary(FunctionCall(curName, args))
                            }
                        }

                    } else {
                        currentTest = nodeTest
                    }
                    skipWhitespace()

                    return Step(axis, currentTest, parsePredicates())
                }

                else -> { // finish the step, but don't throw an exception
                    TODO("Not valid")
//                    break;
//                        throw IllegalArgumentException("@$i> Unexpected token '${c}' in '$str'")
                }
            }
        }

        private fun parseArgs(): List<Expr> {
            require(tryCurrent('('))

            val args = mutableListOf<Expr>()
            if (!tryCurrent(')')) {
                while (true) {
                    skipWhitespace()
                    args.add(parseExpr())
                    require(i < str.length) { "@$i> Missing closing parenthesis" }
                    if (tryCurrent(')')) break
                    require(tryCurrent(',')) { "@$i> parameters should be separated by ',': '${str.substring(i - 1)}" }
                }
            }

            return args
        }

        private fun parseAttribute(): Step {
            check(tryCurrent('@'))
            skipWhitespace()
            val test: NodeTest = requireNotNull(parseNodeTest()) { "@$i> Missing node test for attribute: '${str.substring(i)}'"}
            return Step(Axis.ATTRIBUTE, test, parsePredicates())
        }

        private fun parseNodeTest(firstWord: String? = null): NodeTest? {
            if (firstWord == null && tryCurrent('*')) return NodeTest.AnyNameTest

            if (firstWord == null && (i >= str.length || !isNameStartChar(str[i]))) return null

            val prefixOrLocal = firstWord ?: parseNCName()
            skipWhitespace()
            if (tryCurrent(':')) {
                skipWhitespace()
                val ns = lookupNamespace(prefixOrLocal)
                return when {
                    tryCurrent('*') -> NodeTest.NSTest(ns.toAnyUri(), VNCName(prefixOrLocal))
                    else -> NodeTest.QNameTest(QName(ns, parseNCName(), prefixOrLocal))
                }
            } else {
                val ns = namespaceContext.getNamespaceURI("") ?: ""
                return NodeTest.QNameTest(QName(ns, prefixOrLocal))
            }
        }

        private fun parsePredicates(): List<Expr> = buildList {
            while (tryCurrent('[')) {

                skipWhitespace()
                add(parseExpr())
                skipWhitespace()
                require(tryCurrent(']')) { "@$i> Predicate not closed by ']': '${str.substring(i)}'" }

                skipWhitespace()
            }
        }

        private fun peekCurrent(): Char? {
            if (i>=str.length) return null
            return str[i]
        }

        private fun peekCurrent(char: Char): Boolean {
            if (i >= str.length) return false
            return str[i] == char
        }

        private fun tryCurrent(char: Char): Boolean {
            if (i >= str.length) return false
            if(str[i] == char) {
                ++i
                return true
            }
            return false
        }

        private fun peekCurrent(check: String): Boolean {
            val end = i + check.length
            if ((end - 1) >= str.length) return false
            return str.substring(i, end) == check
        }

        private fun tryCurrent(check: String): Boolean {
            val end = i + check.length
            if ((end - 1) >= str.length) return false
            if(str.substring(i, end) == check) {
                i = end
                return true
            }
            return false
        }

        private fun peekCurrentWord(check: String): Boolean {
            if (!peekCurrent(check)) return false
            val j = i + check.length
            return j >= str.length || !isNameChar(str[j])
        }

        private fun tryCurrentWord(check: String): Boolean {
            if (!peekCurrent(check)) return false
            val j = i + check.length
            if(j >= str.length || !isNameChar(str[j])) {
                i = j
                return true
            }
            return false
        }

        fun lookupNamespace(prefix: String?): String = when {
            prefix.isNullOrEmpty() -> namespaceContext.getNamespaceURI("") ?: ""
            else -> requireNotNull(namespaceContext.getNamespaceURI(prefix)) {
                "Missing namespace for prefix '$prefix'"
            }
        }

    }

}