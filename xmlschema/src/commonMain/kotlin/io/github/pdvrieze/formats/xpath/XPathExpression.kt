/*
 * Copyright (c) 2023-2026.
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

package io.github.pdvrieze.formats.xpath

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xpath.impl.*
import io.github.pdvrieze.formats.xpath.impl.functions.Fn
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.internal.isNameChar11
import nl.adaptivity.xmlutil.core.internal.isNameStartChar
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.contracts.ExperimentalContracts

@OptIn(XPathInternal::class)
@Serializable(XPathExpression.Serializer::class)
class XPathExpression private constructor(
    override val xmlString: String,
    @XPathInternal
    internal val expr: Expr,
    val version: Version,
) : VToken {

    enum class Version : Comparable<Version> {
        V1_0,
        V2_0,
        V3_0,
        V3_1;
    }


    companion object Serializer : KSerializer<XPathExpression> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.XPathExpression",
            PrimitiveKind.STRING
        )

        override fun serialize(encoder: Encoder, value: XPathExpression) {
            if (encoder is XML.XmlOutput) {
                // TODO ensure prefixes exist encoder.target
                val str = buildString {
                    value.expr.appendToString(this, encoder.target)
                }
                encoder.encodeString(str)
            } else {
                encoder.encodeString(value.xmlString) // TODO use xml aware writing
            }
        }

        override fun deserialize(decoder: Decoder): XPathExpression {
            val nsContext = (decoder as? XML.XmlInput)?.input?.namespaceContext ?: SimpleNamespaceContext()
            return invoke(xmlTrimWhitespace(decoder.decodeString()), nsContext)
        }

        operator fun invoke(
            path: String,
            namespaceContext: NamespaceContext = SimpleNamespaceContext(),
            ver: Version = Version.V3_1
        ): XPathExpression {
            val parser = Parser(xmlTrimWhitespace(path), namespaceContext, ver)
            return XPathExpression(path, parser.parse(), ver)
        }

        // TODO: Make including this configurable
        private val XQUERY_BUILTIN_PREFIX_MAPPINGS = HashMap<String, String>().apply {
            put("xs", XMLConstants.XSD_NS_URI)
            put("fn", XMLConstants.XPATH_FUNCTIONS_NAMESPACE)
            put("map", "${XMLConstants.XPATH_FUNCTIONS_NAMESPACE}/map")
            put("array", "${XMLConstants.XPATH_FUNCTIONS_NAMESPACE}/array")
            put("math", "${XMLConstants.XPATH_FUNCTIONS_NAMESPACE}/math")
            put("err", "http://www.w3.org/2005/xqt-errors")
        }

        internal val STEP_DOC_ROOT = FilterExpr(
            TreatAsExpr(
                StaticFunctionCall(Fn.Root.name, LocationPath(AxisStep(Axis.SELF, NodeTest.node))),
                SequenceType.ItemSequence(ItemType.documentNode, SequenceType.OccurrenceType.ANY)
            ),
            emptyList()
        )

    }

    private class Parser(
        private val str: String,
        private val namespaceContext: NamespaceContext,
        private val version: Version,
    ) {
        var i: Int = 0

        fun parsePathExprOld(): Expr {
            var current: Expr? = null

            skipWhitespace()
            val start = i

            while (i < str.length) {
                when (val c = str[i]) {
                    ' ' -> {} // ignore
                    '$' -> current = parseVariableReference()

                    '|' -> current = BinaryExpr(
                        Operator.UNION,
                        requireNotNull(current) { "@$i> Path expressions can not start with |" },
                        parsePathExprOld()
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
            val l = str.length
            while (i < l) {
                val c = str[i]
                when {
                    c == '(' -> {
                        val j = i + 1
                        if (j >= l || str[i + 1] != ':') return

                        i += 2
                        parseCommentCont()
                    }

                    !isXmlWhitespace(c) -> return
                    else -> ++i
                }
            }
        }

        private fun parseComment() {
            check(tryCurrent("(:"))
            return parseCommentCont()
        }

        private fun parseCommentCont() {
            val start = i - 2
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
            while (i < str.length && (str[i] != ':' && isNameChar11(str[i]))) {
                append(str[i++])
            }
        }

        private fun parseEQName(initialWord: String): QName {
            if (initialWord != "Q" || !tryCurrent("'")) {
                if (tryCurrent(':')) {
                    val localName = parseNCName()
                    return QName(lookupNamespace(initialWord), localName, initialWord)
                } else {
                    return QName(lookupNamespace(""), initialWord, "")
                }
            }

            val namespace = buildString {
                while (i < str.length && str[i] != '}') {
                    append(str[i++])
                }
                require(str[i++] == '}')
            }.trim()

            val localName = parseNCName()
            return QName(namespace, localName)
        }

        private fun parseEQName(): QName {
            skipWhitespace()
            if (!tryCurrent("Q{")) {
                return parseQName()
            }

            val namespace = buildString {
                while (i < str.length && str[i] != '}') {
                    append(str[i++])
                }
                require(str[i++] == '}')
            }.trim()

            val localName = parseNCName()
            return QName(namespace, localName)
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
            skipWhitespace()
            val localOrPrefix = parseNCName()
            if (localOrPrefix == "item") {
                require(tryCurrentToken('('))
                require(tryCurrentToken(')'))

                return ItemType.ItemTest
            }
            val nodeType = NodeType.maybeValueOf(localOrPrefix)
            if (nodeType != null) {
                return NodeTest.NodeTypeTest(nodeType, parseArgs())
            }
            if (tryCurrentToken(':')) {
                val localName = parseNCName()
                return ItemType.AtomicType(QName(lookupNamespace(localOrPrefix), localName, localOrPrefix))
            } else {
                return ItemType.AtomicType(QName(lookupNamespace(""), localOrPrefix, ""))
            }
        }

        private fun parseSequenceType(): SequenceType {
            if (tryCurrentWordToken("empty-sequence")) {
                require(tryCurrentToken('('))
                require(tryCurrentToken(')'))
                return SequenceType.EmptySequence
            }
            val itemType = parseItemType()
            val occurrence = when (peekCurrentToken()) {
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
            val expressions = mutableListOf<ExprSingle>()
            expressions.add(parseExprSingle())
            while (tryCurrentToken(',')) {
                expressions.add(parseExprSingle())
            }
            return expressions.singleOrNull() ?: SequenceExpr(expressions)
        }

        private fun parseExprSingle(): ExprSingle {
            skipWhitespace()
            return when (str[i]) {
                'f' if (tryCurrentWord("for")) -> parseForExpr()
                'l' if (tryCurrentWord("let")) -> parseLetExpr()
                's' if (tryCurrentWord("some")) -> parseQuantifiedExpr(QuantifiedExpr.Kind.SOME)
                'e' if (tryCurrentWord("every")) -> parseQuantifiedExpr(QuantifiedExpr.Kind.EVERY)
                'i' if (tryCurrentWord("if")) -> parseIfExpr()
                else -> parseOrExpr()
            }
        }

        private fun parseForExpr(): ForExpr {
            skipWhitespace()
            val bindings = mutableListOf<ForExpr.Binding>()
            do {
                require(tryCurrent('$'))
                val varName = parseNCName()
                require(tryCurrentWordToken("in"))
                val seqExpr = parseExprSingle()
                bindings.add(ForExpr.Binding(varName, seqExpr))
            } while (tryCurrentToken(','))

            require(tryCurrentWordToken("return"))
            val returned = parseExprSingle()
            return ForExpr(bindings, returned)
        }

        private fun parseLetExpr(): LetExpr {
            skipWhitespace()
            val bindings = mutableListOf<LetExpr.Binding>()
            do {
                require(tryCurrentToken('$'))
                val varName = parseNCName()
                require(tryCurrentToken(":="))
                val rValueExpr = parseExprSingle()
                bindings.add(LetExpr.Binding(varName, rValueExpr))
            } while (tryCurrentToken(','))

            require(tryCurrentWordToken("return"))
            val returned = parseExprSingle()
            return LetExpr(bindings, returned)
        }

        private fun parseIfExpr(): IfExpr {
            require(tryCurrentToken('(')) { "@$i> Missing opening parenthesis in if expression" }

            val condition = parseExpr()

            require(tryCurrentToken(')')) { "@$i> Missing closing parenthesis in if expression" }
            require(tryCurrentWordToken("then")) { "@$i> Missing 'then' in if expression" }

            val thenExpr = parseExprSingle()

            require(tryCurrentWordToken("else"))
            return IfExpr(condition, thenExpr, parseExprSingle())
        }

        private fun parseOrExpr(): ExprSingle {
            val exprs = mutableListOf<ExprSingle>()
            exprs.add(parseAndExpr())
            while (tryCurrentWordToken("or")) {
                exprs.add(parseAndExpr())
            }

            return exprs.singleOrNull() ?: OperatorExpr(Operator.OR, exprs)
        }

        private fun parseAndExpr(): ExprSingle {
            val exprs = mutableListOf<ExprSingle>()
            exprs.add(parseComparisonExpr())

            while (tryCurrentWordToken("and")) {
                exprs.add(parseComparisonExpr())
            }

            return exprs.singleOrNull() ?: OperatorExpr(Operator.AND, exprs)
        }

        private fun parseComparisonExpr(): ExprSingle {
            val current: ExprSingle = parseStringConcatExpr()
            skipWhitespace()

            // there must always be a following expression so testing for second operator character is fine
            if (i + 1 > str.length) return current
            when (str[i]) {
                '=' -> return BinaryExpr.priority(Operator.EQ, current, parseStringConcatExpr())

                '!' if str[i + 1] == '=' ->
                    return BinaryExpr.priority(Operator.NEQ, current, parseStringConcatExpr())

                '<' -> return when {
                    str[i + 1] == '=' -> BinaryExpr.priority(Operator.LE, current, parseStringConcatExpr())
                    str[i + 1] == '<' -> BinaryExpr.priority(Operator.PRECEDES, current, parseStringConcatExpr())
                    else -> BinaryExpr.priority(Operator.LT, current, parseStringConcatExpr())
                }

                '>' -> return when {
                    str[i + 1] == '=' -> BinaryExpr.priority(Operator.GE, current, parseStringConcatExpr())
                    str[i + 1] == '>' -> BinaryExpr.priority(Operator.FOLLOWS, current, parseStringConcatExpr())
                    else -> BinaryExpr.priority(Operator.GT, current, parseStringConcatExpr())
                }

                'e' if tryCurrentWord("eq") ->
                    return BinaryExpr.priority(Operator.VAL_EQ, current, parseStringConcatExpr())

                'n' if tryCurrentWord("ne") ->
                    return BinaryExpr.priority(Operator.VAL_NEQ, current, parseStringConcatExpr())

                'l' -> when {
                    tryCurrentWord("lt") ->
                        return BinaryExpr.priority(Operator.VAL_LT, current, parseStringConcatExpr())

                    tryCurrentWord("le") ->
                        return BinaryExpr.priority(Operator.VAL_LE, current, parseStringConcatExpr())
                }

                'g' -> when {
                    tryCurrentWord("gt") ->
                        return BinaryExpr.priority(Operator.VAL_GT, current, parseStringConcatExpr())

                    tryCurrentWord("ge") ->
                        return BinaryExpr.priority(Operator.VAL_GE, current, parseStringConcatExpr())
                }

                'i' if (tryCurrentWord("is")) ->
                    return BinaryExpr(Operator.IS, current, parseStringConcatExpr())
            }
            return current
        }

        private fun parseStringConcatExpr(): ExprSingle {
            val concats = mutableListOf<ExprSingle>()
            concats.add(parseRangeExpr())

            while (tryCurrentToken("||")) {
                concats.add(parseRangeExpr())
            }

            return concats.singleOrNull() ?: OperatorExpr(Operator.CONCAT, concats)
        }

        private fun parseRangeExpr(): ExprSingle {
            val e = parseAdditiveExpr()

            return when {
                tryCurrentWord("to") -> RangeExpr(e, parseAdditiveExpr())
                else -> e
            }
        }

        private fun parseAdditiveExpr(): ExprSingle {
            var current: ExprSingle = parseMultiplicativeExpr()
            do {

                current = when (peekCurrent()) {
                    '+' -> BinaryExpr.priority(Operator.ADD, current, parseMultiplicativeExpr())
                    '-' -> BinaryExpr.priority(Operator.SUB, current, parseMultiplicativeExpr())
                    else -> return current
                }

            } while (i < str.length)

            return current
        }

        private fun parseMultiplicativeExpr(): ExprSingle {
            var current: ExprSingle = parseUnionExpr()
            do {
                current = when {
                    tryCurrentToken('*') -> BinaryExpr.priority(Operator.MUL, current, parseUnionExpr())
                    tryCurrentWord("div") -> BinaryExpr.priority(Operator.DIV, current, parseUnionExpr())
                    tryCurrentWord("idiv") -> BinaryExpr.priority(Operator.IDIV, current, parseUnionExpr())
                    tryCurrentWord("mod") -> BinaryExpr.priority(Operator.MOD, current, parseUnionExpr())
                    else -> return current
                }

            } while (i < str.length)
            return current
        }

        private fun parseUnionExpr(): ExprSingle {
            val unions = mutableListOf<ExprSingle>()
            unions.add(parseIntersectExceptExpr())

            while (tryCurrentToken('|') || tryCurrentWord("union")) {
                unions.add(parseIntersectExceptExpr())
            }
            return unions.singleOrNull() ?: OperatorExpr(Operator.UNION, unions)
        }

        private fun parseIntersectExceptExpr(): ExprSingle {
            var current: ExprSingle = parseInstanceofExpr()
            do {
                current = when {
                    tryCurrentWordToken("intersect") ->
                        BinaryExpr.priority(Operator.INTERSECT, current, parseInstanceofExpr())

                    tryCurrentWordToken("except") ->
                        BinaryExpr.priority(Operator.EXCEPT, current, parseInstanceofExpr())

                    else -> return current
                }

            } while (i < str.length)
            return current
        }

        private fun parseInstanceofExpr(): ExprSingle {
            val e = parseTreatExpr()

            if (tryCurrentWordToken("instance")) {
                require(tryCurrentWordToken("of")) { "@$i> Missing 'of' in 'instance of' expression: '${str.substring(i)}'" }
                return InstanceOfExpr(e, parseSequenceType())
            }
            return e
        }

        private fun parseTreatExpr(): ExprSingle {
            val e = parseCastableExpr()

            if (tryCurrentWordToken("treat")) {
                require(tryCurrentWordToken("as")) { "@$i> Missing 'as' in 'treat as' expression: '${str.substring(i)}'" }
                return TreatAsExpr(e, parseSequenceType())
            }
            return e
        }

        private fun parseCastableExpr(): ExprSingle {
            val e = parseCastExpr()

            if (tryCurrentWordToken("castable")) {

                require(tryCurrentWordToken("as")) { "@$i> Missing 'as' in 'castable as' expression: '${str.substring(i)}'" }

                val typeName = parseSimpleTypeName()
                val allowsEmpty = tryCurrentToken('?')
                return CastableExpr(e, typeName, allowsEmpty)
            }
            return e
        }

        private fun parseSimpleTypeName(): QName {
            skipWhitespace()
            return parseQName()
        }

        private fun parseCastExpr(): ExprSingle {
            val expr = parseArrowExpr()

            if (!tryCurrentWordToken("castable")) return expr

            require(tryCurrentWordToken("as")) { "@$i> Missing 'as' in 'castable as' expression: '${str.substring(i)}'" }

            val typeName = parseSimpleTypeName()
            val allowsEmpty = tryCurrentToken('?')
            return CastableExpr(expr, typeName, allowsEmpty)
        }

        private fun parseArrowExpr(): ExprSingle {
            var expr = parseUnaryExpr()

            while (tryCurrentToken("=>")) {
                val functionSpecifier = parseArrowFunctionSpecifier()
                skipWhitespace()
                val params = when (val e = parseSequenceOrParen().expr) {
                    is SequenceExpr -> e.elements
                    is ExprSingle -> listOf(e)
                }
                expr = ArrowFunction(expr, functionSpecifier, params)
            }
            return expr
        }

        private fun parseArrowFunctionSpecifier(): ArrowFunctionSpecifier {
            return when (peekCurrentToken()) {
                '$' -> ArrowFunctionSpecifier.VarRefFunc(parseVariableReference().varName)
                '(' -> ArrowFunctionSpecifier.SeqFunc(parseSequenceOrParen())
                else -> ArrowFunctionSpecifier.QNameFunc(parseEQName())
            }

        }

        private fun parseUnaryExpr(): ExprSingle {
            return when (peekCurrentToken()) {
                '+' -> UnaryExpr.Plus(parseValueExpr())
                '-' -> UnaryExpr.Minus(parseValueExpr())
                else -> parseValueExpr()
            }
        }

        private fun parseValueExpr(): ExprSingle {
            val exprs = mutableListOf<ExprSingle>()
            exprs.add(parsePathExpr())

            while (tryCurrentToken('!')) {
                exprs.add(parsePathExpr())
            }
            return exprs.singleOrNull() ?: MapExpr(exprs)
        }

        private fun parsePathExpr(): ExprSingle {
            val steps = mutableListOf<PrimaryOrStep>()
            if (!peekCurrentToken('/')) {
                parseRelativePathExpr(steps)
                return (steps.singleOrNull() as? FilterExpr)?.takeIf { it.predicates.isEmpty() }?.primaryExpr
                    ?: LocationPath(false, steps)
            }

            // starts with '/'
            steps.add(STEP_DOC_ROOT)

            // TODO special leading lone slash
            if (i >= str.length) return LocationPath(true, steps)

            val c2 = str[++i]
            when (c2) {
                '/' -> {
                    steps.add(STEP_DESCENDANT_OR_SELF)
                    ++i
                    parseRelativePathExpr(steps)
                }

    //                        ')' -> return LocationPath(true, emptyList())

                // ALl non-letters that are step starts
                /* Axis steps:
                             *  - `*` wildcard
                             *  - `@` attribute
                             *  - `.` self or parent
                             * Primary Expressions:
                             *  - `0`..'9' Number literals
                             *  - `.` start of decimal or double
                             *  - `$` variable reference
                             *  - `(` parenthesized expression
                             *  - `.` context item
                             *  - `'`, `"` start of string literal
                             *  - `[` square array constructor
                             *  - `?` unary lookup
                             */
                '*', '@', '.', '\'', '"', '[', '?', '$', '(',
                in '0'..'9' -> parseRelativePathExpr(steps)

                // letters are step starts
                else if isNameStartChar(c2) -> parseRelativePathExpr(steps)

                else -> return LocationPath(true, steps)
            }
            return LocationPath(true, steps)
        }

        private fun parseRelativePathExpr(steps: MutableList<PrimaryOrStep>) {

            steps.add(parseStepExpr() ?: return) // no step
            while (tryCurrentToken('/')) {
                when {
                    tryCurrentToken("/") -> steps.apply {
                        add(AxisStep(Axis.DESCENDANT_OR_SELF, NodeTest.NodeTypeTest(NodeType.NODE)))
                        add(requireNotNull(parseStepExpr()) { "Missing step after '//' in relative path expression" })
                    }

                    else -> steps.add(requireNotNull(parseStepExpr()) { "Missing step after '/' in relative path expression" })
                }
            }
        }

        private fun parseStepExpr(): PrimaryOrStep? {
            /* Axis steps:
             *  - `*` wildcard
             *  - `@` attribute
             *  - `.` self or parent
             * Primary Expressions:
             *  - `0`..'9' Number literals
             *  - `.` start of decimal or double
             *  - `$` variable reference
             *  - `(` parenthesized expression
             *  - `.` context item
             *  - `'`, `"` start of string literal
             *  - `[` square array constructor
             *  - `?` unary lookup
             */

            when (val c = peekCurrentToken() ?: return null) {
                '@' -> {
                    val axis = Axis.ATTRIBUTE
                    ++i

                    val nodeTest = requireNotNull(parseNodeTest()) { "Missing node test in attribute shorthand" }
                    val predicates = parsePredicates()
                    return AxisStep(axis, nodeTest, predicates)
                }

                in '0'..'9' -> return parsePostfixExpr(parseNumber())

                '.' -> {
                    ++i
                    when {
                        tryCurrent('.') -> {
                            return AxisStep(Axis.PARENT, NodeTest.node)
                        }

                        i >= str.length -> return AxisStep(Axis.SELF, NodeTest.node)

                        str[i] in '0'..'9' -> return parsePostfixExpr(parseNumber())

                        else -> return AxisStep(Axis.SELF, NodeTest.node)
                    }
                }

                '(' -> return parsePostfixExpr(parseSequenceOrParen())

                '$' -> return parsePostfixExpr(parseVariableReference())

                '\'', '"' -> return FilterExpr(parseStringLiteral())

                '[' -> return parsePostfixExpr(parseSquareArrayConstructor())

                '?' -> return parsePostfixExpr(parseUnaryLookup())

                else if isNameStartChar(c) -> {
                    val start = i
                    val ncName = parseNCName()
                    if (tryCurrentToken("::")) { // found axis
                        val axis = Axis.from(ncName)
                        val nodeTest = requireNotNull(parseNodeTest()) {
                            "@$i> Expected node test after axis name: ${str.substring(start)}"
                        }

                        return AxisStep(axis, nodeTest, parsePredicates())
                    } else {
                        val name = parseEQNameOrWildcard(ncName)
                        if (name is QNameSpec.EQName && peekCurrentToken('(')) { // This is a postFix function expression
                            when (val nt = maybeParseNodeTypeTest(name)) {
                                null -> {
                                    val funcCall = StaticFunctionCall(name.asQName(), parseArgs())

                                    return parsePostfixExpr(funcCall)
                                }

                                else -> return AxisStep(Axis.CHILD, nt, parsePredicates())
                            }

                        } else {
                            return AxisStep(Axis.CHILD, name.asNodeTest(), parsePredicates())
                        }
                    }
                }

                else -> {
                    val nodeTest = maybeParseNodeTest() ?: return null
                    val predicates = parsePredicates()
                    return AxisStep(Axis.CHILD, nodeTest, predicates)
                }
            }
        }

        private fun parseSquareArrayConstructor(): ExprSingle {
            TODO()
        }

        private fun parseUnaryLookup(): ExprSingle {
            TODO()
        }

        private fun parseExprSingleOld(): ExprSingle {
            skipWhitespace()

            var current: ExprSingle

            require(i < str.length) { "Empty expression" }
            val c = str[i]
            when {
                c == '/' || c == '@' || c == '.' || c == '*' || c == '$' ->
                    current = parseLocationPath()

                c == '(' -> current = parseSequenceOrParen()

                c == '\'' || c == '"' -> current = parseStringLiteral()

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
                            current = parseLocationPath()
                        }
                    }

                }

                else -> throw IllegalArgumentException(
                    "@$i> Unexpected character '${str[i]}' in expression - '${str.substring(i)}' from '$str'"
                )
            }

            skipWhitespace()
            while (i < str.length) {
//            if (i >= str.length) return current
                when (str[i]) {
                    '(' if (version >= Version.V3_0) -> {
                        val params = when (val e = parseSequenceOrParen().expr) {
                            is SequenceExpr -> e.elements
                            is ExprSingle -> listOf(e)
                        }
                        current = DynamicFunctionCall(current, params)
                    }

                    '|' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.UNION, current, parseExprSingle())
                    }

                    '=' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.EQ, current, parseExprSingle())
                    }

                    '!' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.NEQ, current, parseExprSingle())
                    }

                    '<' -> current = when {
                        tryCurrent("<=") -> BinaryExpr.priority(Operator.LE, current, parseExprSingle())

                        tryCurrent("<<") -> BinaryExpr.priority(Operator.PRECEDES, current, parseExprSingle())

                        else -> {
                            ++i
                            BinaryExpr.priority(Operator.LT, current, parseExprSingle())
                        }
                    }

                    '>' -> current = when {
                        tryCurrent(">=") -> BinaryExpr.priority(Operator.GE, current, parseExprSingle())

                        tryCurrent(">>") -> BinaryExpr.priority(Operator.FOLLOWS, current, parseExprSingle())

                        else -> {
                            ++i
                            BinaryExpr.priority(Operator.GT, current, parseExprSingle())
                        }
                    }

                    '*' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.MUL, current, parseExprSingle())
                    }

                    '+' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.ADD, current, parseExprSingle())
                    }

                    '-' -> {
                        ++i
                        current = BinaryExpr.priority(Operator.SUB, current, parseExprSingle())
                    }

                    'a' -> {
                        if (!tryCurrentWord("and")) return current

                        current = BinaryExpr.priority(Operator.AND, current, parseExprSingle())
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

                        current = BinaryExpr.priority(Operator.DIV, current, parseExprSingle())
                    }

                    'e' -> {
                        if (!tryCurrentWord("eq")) return current

                        current = BinaryExpr.priority(Operator.VAL_EQ, current, parseExprSingle())
                    }

                    'g' -> {
                        when {
                            tryCurrentWord("ge") ->
                                current = BinaryExpr.priority(Operator.VAL_GE, current, parseExprSingle())

                            tryCurrentWord("gt") ->
                                current = BinaryExpr.priority(Operator.VAL_GT, current, parseExprSingle())

                            else -> return current
                        }
                    }

                    'i' -> {
                        when {
                            tryCurrentWord("instance") -> {
                                skipWhitespace()
                                if (!tryCurrentWord("of")) {
                                    throw IllegalArgumentException(
                                        "@$i> Missing 'of' in 'instance' expression: '${
                                            str.substring(
                                                i
                                            )
                                        }'"
                                    )
                                }
                                skipWhitespace()
                                current = InstanceOfExpr(current, parseSequenceType())
                            }

                            tryCurrentWord("idiv") ->
                                current = BinaryExpr.priority(Operator.IDIV, current, parseExprSingle())

                            tryCurrent("is") ->
                                current = BinaryExpr.priority(Operator.IS, current, parseExprSingle())

                            else -> return current
                        }
                    }

                    'l' -> {
                        when {
                            tryCurrentWord("le") ->
                                current = BinaryExpr.priority(Operator.VAL_LE, current, parseExprSingle())

                            tryCurrentWord("lt") ->
                                current = BinaryExpr.priority(Operator.VAL_LT, current, parseExprSingle())

                            else -> return current
                        }
                    }

                    'm' -> {
                        if (!tryCurrentWord("mod")) return current
                        current = BinaryExpr.priority(Operator.MOD, current, parseExprSingle())
                    }

                    'n' -> {
                        if (!tryCurrentWord("ne")) return current
                        current = BinaryExpr.priority(Operator.VAL_NEQ, current, parseExprSingle())
                    }

                    'o' -> {
                        if (!tryCurrentWord("or")) return current
                        current = BinaryExpr.priority(Operator.OR, current, parseExprSingle())
                    }

                    't' -> {
                        if (!tryCurrentWord("to")) return current
                        current = RangeExpr(current, parseExprSingle())
                    }

                    else -> return current //no expression elements

                }
                skipWhitespace()
            }
            return current
        }

        private fun parseSequenceOrParen(): ParenExpr {
            require(str[i] == '(') {
                "@$i> Expected '(' in sequence expression, found: '${str.substring(0, i)}>${str[i]}<${str.substring(i)}"
            }
            val c: ParenExpr
            ++i
            val expr = parseExprSingle()
            skipWhitespace()
            require(i < str.length) { "@$i> missing closing )" }
            if (str[i] != ')') {
                val elements: MutableList<ExprSingle> = mutableListOf(expr)
                do {
                    require(tryCurrent(',')) {
                        "@$i> Invalid character '${str[i]}' in range expression: '${
                            str.substring(
                                i
                            )
                        }'"
                    }
                    // tryCurrent will move the parsing position forward
                    skipWhitespace()
                    elements.add(parseExprSingle())
                    require(i < str.length) { "@$i> missing closing )" }
                } while (str[i] != ')')
                c = ParenExpr(SequenceExpr(elements))
            } else {
                c = expr as? ParenExpr ?: ParenExpr(expr)
            }
            ++i
            return c
        }

        private fun parseQuantifiedExpr(kind: QuantifiedExpr.Kind): ExprSingle {
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

            /*
                        //TODO might be regular expression parsing
                        val exprs = mutableListOf<ExprSingle>()
                        exprs.add(parseExprSingle())
                        while (tryCurrent(',')) {
                            skipWhitespace()
                            exprs.add(parseExprSingle())
                        }
            */
            val source = parseExpr()//exprs.singleOrNull() ?: SequenceExpr(exprs)

            skipWhitespace()

            require(tryCurrentWord("satisfies")) { "@$i> Missing satisfies in quantified expression: ${str.substring(i)}" }

            skipWhitespace()

            val condition = parseExprSingle()
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
            val steps = mutableListOf<PrimaryOrStep>()
            skipWhitespace()
            while (i < str.length) {
                val c = str[i]
                when {
//                    c == ' ' -> ++i // ignore

                    c == '.' -> { // TODO can use step parsing
                        ++i
                        val axis = when {
                            tryCurrent('.') -> Axis.PARENT
                            else -> Axis.SELF
                        }
                        steps.add(AxisStep(axis, NodeTest.NodeTypeTest(NodeType.NODE)))
                    }

                    c == '$' -> {
                        steps.add(parseStep())
                    }

                    c == '/' -> {
                        val other: Char?
                        if (start == i) {
                            rooted = true
                            other = if (i + 1 < str.length) str[i + 1] else null
                        } else {
                            other = str[i - 1]
                        }

                        if (other == '/') { // shortcut
                            if (rooted) ++i
                            steps.add(AxisStep(Axis.DESCENDANT_OR_SELF, NodeTest.NodeTypeTest(NodeType.NODE)))
                        } else {
                            ++i
                            skipWhitespace()
                            steps.add(parseStep())
                        }
                    }

                    c == '(' -> {
                        require(steps.isEmpty()) { "Primary expression in invalid point" }
                        ++i
                        steps.add(FilterExpr(parseExprSingle(), parsePredicates()))
                        skipWhitespace()
                        require(tryCurrent(')')) { "@$i> Expression not ended by ')'" }
                    }

                    c.isDigit() -> {
                        steps.add(FilterExpr(parseExprSingle(), parsePredicates()))
                    }

                    isNameStartChar(c) ||
                            c == '*' ||
                            c == '@' -> { //attribute
                        steps.add(parseStep())
                    }

                    else -> break
                }
                skipWhitespace()
                if (!tryCurrent('/')) break

                skipWhitespace()
            }
            return LocationPath(rooted, steps)
        }

        private fun parseStringLiteral(): StringLiteral {
            val delim = when (str[i]) {
                '\'' -> '\''
                '"' -> '"'
                else -> throw IllegalArgumentException("@$i> Literal does not start with quote, but with '${str[i]}'")
            }
            ++i
            var start = i

            val string = StringBuilder()

            while (i < str.length) {
                when (str[i]) {
                    delim if (i + 1 < str.length && str[i + 1] == delim) -> {
                        string.append(str, start, i + 1)
                        i += 2 // skip reading the second delimiter (again)
                        start = i // Set the start after the second delimiter
                    }

                    delim -> break
                    else -> ++i
                }
            }
            if (i > start) {
                string.append(str, start, i)
            }
            require(i < str.length) { "@$i> Literal string not closed" }
            return StringLiteral(string.toString()).also { ++i } // skip delim
        }

        private fun parseNumber(): NumberLiteral<*> {
            val start = i

            if (str[i] == '-') ++i

            require(i < str.length && str[i].isDigit()) { "@$start> '${str.substring(start, i)}' not a number" }

            var seenPeriod = false
            var seenExp = false
            while (i < str.length) {
                when (str[i]) {
                    '.' -> when {
                        seenPeriod || seenExp -> return DoubleLiteral(str.substring(start, i).toDouble())
                        else -> seenPeriod = true
                    }

                    'e', 'E' -> when {
                        seenExp -> return DoubleLiteral(str.substring(start, i).toDouble())
                        else -> {
                            seenExp = true
                            // skip signs here
                            if (i + 1 < str.length) when (val c = str[i + 1]) {
                                '+' -> ++i
                                '-' -> ++i
                            }
                        }
                    }

                    !in '0'..'9' -> break
                }
                ++i
            }
            return when {
                seenPeriod || seenExp -> DoubleLiteral(str.substring(start, i).toDouble())
                else -> IntLiteral(str.substring(start, i).toLong())
            }
        }

        private fun parseStep(): PrimaryOrStep {
            skipWhitespace()
            require(i < str.length) { "@$i> Empty expression" }

            val c = str[i]
            when {
                c == '/' -> { // special case for "empty" expression
//                    ++i
                    return AxisStep(Axis.DESCENDANT_OR_SELF, NodeTest.NodeTypeTest(NodeType.NODE))
                }

                c == '.' && tryCurrent("..") -> {
                    skipWhitespace()
                    return AxisStep(Axis.PARENT, NodeTest.NodeTypeTest(NodeType.NODE), parsePredicates())
                }

                c == '.' -> {
                    ++i
                    skipWhitespace()
                    return AxisStep(Axis.SELF, NodeTest.NodeTypeTest(NodeType.NODE), parsePredicates())
                }

                c == '$' -> {
                    val varRef = parseVariableReference()
                    skipWhitespace()
                    return FilterExpr(varRef, parsePredicates())
                }

                c == '@' -> { //attribute
                    return parseAttribute()
                }

                c == '*' -> {
                    ++i
                    skipWhitespace()
                    return AxisStep(Axis.CHILD, NodeTest.AnyNameTest, parsePredicates())
                }

                c == '(' -> {
                    val parenExpr = parseSequenceOrParen()
                    return parsePostfixExpr(parenExpr)
                }

                isNameStartChar(c) -> {
                    val word = parseNCName()

//                    val curName: QName
                    val axis: Axis
                    val nodeTest: NodeTest

                    skipWhitespace()
                    if (tryCurrent("::")) {
                        axis = Axis.from(word)
                        skipWhitespace()
                        nodeTest =
                            requireNotNull(parseNodeTest()) { "@$i> Missing node test in step: '${str.substring(i)}'" }
                    } else if (peekCurrent('(') && word == "if") {
                        return FilterExpr(parseIfConditionAndConsequences(), emptyList())
                    } else {
                        val eqName = parseEQNameOrWildcard(word)
                        val maybeNoteTypeTest = maybeParseNodeTypeTest(eqName)
                        axis = Axis.CHILD
                        if (maybeNoteTypeTest != null) {
                            nodeTest = maybeNoteTypeTest
                        } else if (eqName is QNameSpec.EQName && tryCurrentToken('(')){
                            val funcCall = StaticFunctionCall(eqName.asQName(), parseArgs())
                            return parsePostfixExpr(funcCall)
                        } else {
                            nodeTest = eqName.asNodeTest()
                        }

                    }

                    val currentTest: NodeTest
                    if (nodeTest is NodeTest.QNameTest && peekCurrent('(')) {
                        val curName = nodeTest.qName
                        val args = parseArgs()

                        val nodeType =
                            if (curName.namespaceURI.isEmpty()) NodeType.maybeValueOf(curName.localPart) else null
                        when (nodeType) {
                            NodeType.PROCESSING_INSTRUCTION -> {
                                if (args.isEmpty()) {
                                    currentTest = NodeTest.ProcessingInstructionTest()
                                } else if (args.size == 1) {
                                    currentTest = when (val arg = args.first()) {
                                        is LiteralExpr<*> ->
                                            NodeTest.ProcessingInstructionTest(NodeTest.NameOrLiteral.Literal(arg.value as String))

                                        else -> throw IllegalArgumentException("Unexpected arguments to processing instruction test")
                                    }
                                } else throw IllegalArgumentException("Unexpected arguments to processing instruction test")
                            }

                            is NodeType -> {
                                currentTest = NodeTest.NodeTypeTest(nodeType, args)
                            }

                            else -> {
                                skipWhitespace()
                                return FilterExpr(StaticFunctionCall(curName, args), parsePredicates())
                            }
                        }

                    } else {
                        currentTest = nodeTest
                    }
                    skipWhitespace()

                    return AxisStep(axis, currentTest, parsePredicates())
                }

                else -> { // finish the step, but don't throw an exception
                    TODO("Not valid")
//                    break;
//                        throw IllegalArgumentException("@$i> Unexpected token '${c}' in '$str'")
                }
            }
        }

        private fun parseIfConditionAndConsequences(): IfExpr {
            require(tryCurrent('('))
            skipWhitespace()
            val testExpr = parseExpr()
            skipWhitespace()
            require(tryCurrent(')'))
            skipWhitespace()
            require(tryCurrentWord("then"))
            skipWhitespace()
            val thenExpr = parseExprSingle()
            skipWhitespace()
            require(tryCurrentWord("else"))
            val elseExpr = parseExprSingle()
            return IfExpr(testExpr, thenExpr, elseExpr)
        }

        private fun parseArgs(): List<ExprSingle> {
            require(tryCurrent('('))

            // TODO(use parseExpr(isSingle = false)
            val args = mutableListOf<ExprSingle>()
            if (!tryCurrent(')')) {
                while (true) {
//                    skipWhitespace()
                    args.add(parseExprSingle())
                    require(i < str.length) { "@$i> Missing closing parenthesis" }
                    if (tryCurrent(')')) break
                    require(tryCurrent(',')) { "@$i> parameters should be separated by ',': '${str.substring(i - 1)}' in $str" }
                }
            }

            return args
        }

        private fun parseAttribute(): AxisStep {
            check(tryCurrent('@'))
            skipWhitespace()
            val test: NodeTest =
                requireNotNull(parseNodeTest()) { "@$i> Missing node test for attribute: '${str.substring(i)}'" }
            return AxisStep(Axis.ATTRIBUTE, test, parsePredicates())
        }

        private fun parseNodeTest(): NodeTest {
            val name = requireNotNull(parseEQNameOrWildcard()) { "Missing node test in expression: ${str.substring(i)}"}
            return name.asNodeTest()
        }

        private fun maybeParseNodeTest(): NodeTest? {
            return parseEQNameOrWildcard()?.asNodeTest()
        }



        @OptIn(ExperimentalContracts::class)
        private fun parseEQNameOrWildcard(): QNameSpec? {
            val word = when (val c=peekCurrentToken() ?: return null) {
                '*' -> {
                    ++i
                    "*"
                }
                else if isNameStartChar(c) -> parseNCName()
                else -> return null
            }
            return parseEQNameOrWildcard(word)
        }

        @OptIn(ExperimentalContracts::class)
        private fun parseEQNameOrWildcard(initialWord: String): QNameSpec {

            if (initialWord == "*") {
                return when { // *: must start localname woildcard
                    tryCurrentToken(':') -> QNameSpec.LocalNameWC(parseNCName())
                    else -> QNameSpec.Any
                }
            }

            if (initialWord == "Q" && peekCurrentToken('{')) {
                val endBrace = str.indexOf('}', i + 1)
                require(endBrace >= 0) { "@$i> Missing closing brace in Braced URI literal: '${str.substring(i)}'" }
                val namespace = str.substring(i + 1, endBrace)
                i = endBrace + 1

                if (tryCurrentToken('*')) {
                    return QNameSpec.Namespace(namespace)
                } else {
                    val localPart = parseNCName()
                    return QNameSpec.EQName(namespace, localPart, null)
                }
            } else if (tryCurrentToken(':')) { //namespace separator
                val ns = requireNotNull(namespaceContext.getNamespaceURI(initialWord)) {
                    "No namespace could be found for prefix '$initialWord'"
                }
                return when {
                    tryCurrentToken('*') -> QNameSpec.Namespace(ns, prefix = initialWord)

                    else -> QNameSpec.EQName(ns, localName = parseNCName(), prefix = initialWord)
                }
            } else {
                val ns = namespaceContext.getNamespaceURI("") ?: ""
                return QNameSpec.EQName(ns, localName = initialWord, prefix = null)
            }
        }

        private fun maybeParseNodeTypeTest(name: QNameSpec): NodeTest? {
            val nodeType = when (name) {
                is QNameSpec.WildCard -> return name.asNodeTest()
                is QNameSpec.EQName if(name.prefix.isNullOrEmpty() && name.namespace.isNullOrEmpty()) ->
                    NodeType.maybeValueOf(name.localName) ?: return null
                else -> return null
            }

            val args = parseArgs()
            return NodeTest.NodeTypeTest(nodeType, args)
        }

        private fun parseNodeTestXX(preParsedWord: String? = null): NodeTest? {
            val word = preParsedWord ?: peekCurrentToken()?.let {
                if (isNameStartChar(it)) parseNCName() else null
            }

            // Kind test (hardcoded list), or eq name or wildcard
            if (word != null) {
                NodeType.maybeValueOf(word)?.let {
                    val args = parseSequenceOrParen().toExprList()
                    return NodeTest.NodeTypeTest(it, args)
                }

                val qName: QName

                if (word == "Q" && peekCurrentToken('{')) {
                    val endBrace = str.indexOf('}', i + 1)
                    require(endBrace >= 0) { "@$i> Missing closing brace in Braced URI literal: '${str.substring(i)}'" }
                    val namespace = str.substring(i + 1, endBrace)
                    i = endBrace + 1

                    if (tryCurrentToken('*')) {
                        return NodeTest.NSTest(VAnyURI(namespace))
                    } else {
                        val localPart = parseNCName()
                        qName = QName(namespace, localPart)
                    }
                } else if (tryCurrentToken(':')) { //namespace separator
                    val ns = requireNotNull(namespaceContext.getNamespaceURI(word)) {
                        "No namespace could be found for prefix '$word'"
                    }
                    when {
                        tryCurrentToken('*') -> {

                            return NodeTest.NSTest(VAnyURI(ns), VNCName(word))
                        }

                        else -> {
                            val localPart = parseNCName()
                            qName = QName(ns, localPart)
                        }
                    }
                } else {
                    val ns = namespaceContext.getNamespaceURI("") ?: ""
                    return NodeTest.QNameTest(QName(ns, word, ""))
                }
                if (peekCurrentToken()=='(') {
                    TODO()
                    // function call
                    // val args = parseSequenceOrParen().toExprList()
                    // return FilterExpr(StaticFunctionCall(qName, args), parsePredicates())
                } else {
                    return NodeTest.QNameTest(qName)
                }

            }

            if (!tryCurrent('*')) return null // no node test here

            return when {
                tryCurrent(':') -> NodeTest.LocalNameTest(parseNCName())

                else -> NodeTest.AnyNameTest
            }
        }

        private fun parseNodeTestOld(firstWord: String? = null): NodeTest? {
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

        private fun parsePostfixExpr(primary: ExprSingle): FilterExpr {
            var current = FilterExpr(primary)
            while(true) {
                when (val c = peekCurrentToken()) {
                    '[' -> {
                        current = FilterExpr(primary, current.predicates + parsePredicates())
                    }
                    '(' -> {
                        val newPrimary: ExprSingle = when {
                            current.predicates.isEmpty() -> current.primaryExpr
                            else -> LocationPath(false, listOf(current))
                        }
                        val args = when (val s = parseSequenceOrParen().expr) {
                            is ExprSingle -> listOf(s)
                            is SequenceExpr -> s.elements
                        }
                        current = FilterExpr(DynamicFunctionCall(newPrimary, args))
                    }
                    '?' -> {
                        ++i
                        val newPrimary: ExprSingle = when {
                            current.predicates.isEmpty() -> current.primaryExpr
                            else -> LocationPath(false, listOf(current))
                        }
                        when (val c2 = peekCurrent()) {
                            null -> throw IllegalArgumentException("@$i> Missing key specifier at end of expression")
                            '(' -> {
                                val expr = parseSequenceOrParen()
                                val newExpr = LookupExpr(newPrimary, LookupExpr.ParenKey(expr.expr))
                                current = FilterExpr(newExpr)
                            }
                            '*' -> {
                                ++i
                                val newExpr = LookupExpr(newPrimary, LookupExpr.AnyKey)
                                current = FilterExpr(newExpr)
                            }

                            in '0'..'9' -> {
                                var j = i+1
                                while (j < str.length && str[j].isDigit()) ++j
                                val newExpr = LookupExpr(newPrimary, LookupExpr.IntegerKey(str.substring(i, j).toInt()))
                                i = j
                                current = FilterExpr(newExpr)
                            }

                            else if isNameStartChar(c2) -> {
                                val name = parseNCName()
                                val newExpr = LookupExpr(newPrimary, LookupExpr.NCNameKey(name))
                                current = FilterExpr(newExpr)
                            }

                            else -> throw IllegalArgumentException("@$i> Invalid key specifier: '${str.substring(i-1)}'")
                        }
                    }

                    else -> return current

                }

            }
        }

        private fun parsePredicates(): List<Expr> = buildList {
            while (tryCurrentToken('[')) {
                val start = i - 1
                add(parseExpr())
                require(tryCurrentToken(']')) { "@$i> Predicate not closed by ']': '${str.substring(i)}' in '${str.substring(start)}'" }
            }
        }

        private fun peekCurrent(): Char? {
            if (i >= str.length) return null
            return str[i]
        }

        private fun peekCurrentToken(): Char? {
            skipWhitespace()
            if (i >= str.length) return null
            return str[i]
        }

        private fun peekCurrent(char: Char): Boolean {
            if (i >= str.length) return false
            return str[i] == char
        }

        private fun peekCurrentToken(char: Char): Boolean {
            skipWhitespace()
            return i < str.length && str[i] == char
        }

        private fun tryCurrent(char: Char): Boolean {
            if (i >= str.length) return false
            if (str[i] == char) {
                ++i
                return true
            }
            return false
        }

        private fun tryCurrentToken(char: Char): Boolean {
            val s = i
            skipWhitespace()
            when {
                i < str.length && str[i] == char -> {
                    ++i
                    return true
                }

                else -> return false
            }
        }

        private fun peekCurrent(check: String): Boolean {
            val end = i + check.length
            if ((end - 1) >= str.length) return false
            return str.substring(i, end) == check
        }

        private fun peekCurrentToken(check: String): Boolean {
            skipWhitespace()
            val end = i + check.length
            if ((end - 1) >= str.length) return false
            return str.substring(i, end) == check
        }

        private fun tryCurrent(check: String): Boolean {
            val end = i + check.length
            if ((end - 1) >= str.length) return false
            if (str.substring(i, end) == check) {
                i = end
                return true
            }
            return false
        }

        private fun tryCurrentToken(check: String): Boolean {
            skipWhitespace()
            val end = i + check.length
            if ((end - 1) >= str.length) return false
            if (str.substring(i, end) == check) {
                i = end
                return true
            }
            return false
        }

        private fun peekCurrentWord(check: String): Boolean {
            if (!peekCurrent(check)) return false
            val j = i + check.length
            return j >= str.length || !isNameChar11(str[j])
        }

        private fun tryCurrentWord(check: String): Boolean {
            if (!peekCurrent(check)) return false
            val j = i + check.length
            if (j >= str.length || !isNameChar11(str[j])) {
                i = j
                return true
            }
            return false
        }

        private fun tryCurrentWordToken(check: String): Boolean {
            if (!peekCurrentToken(check)) return false
            val j = i + check.length
            if (j >= str.length || !isNameChar11(str[j])) {
                i = j
                return true
            }
            return false
        }

        fun lookupNamespace(prefix: String?): String = when {
            prefix.isNullOrEmpty() -> namespaceContext.getNamespaceURI("") ?: ""
            else -> {
                return namespaceContext.getNamespaceURI(prefix)
                    ?: XQUERY_BUILTIN_PREFIX_MAPPINGS[prefix]
                    ?: throw IllegalArgumentException("Missing namespace for prefix '$prefix'")
            }
        }


        companion object {

            val STEP_DESCENDANT_OR_SELF = AxisStep(Axis.DESCENDANT_OR_SELF, NodeTest.node)

        }
    }

}
