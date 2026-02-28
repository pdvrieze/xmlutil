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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import io.github.pdvrieze.formats.xpath.impl.*
import io.github.pdvrieze.formats.xpath.impl.functions.Fn
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.internal.isNameChar11
import nl.adaptivity.xmlutil.core.internal.isNameStartChar
import kotlin.contracts.ExperimentalContracts

@OptIn(XPathInternal::class)
internal class XPathExpressionImpl internal constructor(
    override val xmlString: String,
    override val expr: Expr,
    override val version: XPathVersion,
) : VToken, XPathExpression {

    companion object {

        // TODO: Make including this configurable
        private val XQUERY_BUILTIN_PREFIX_MAPPINGS = HashMap<String, String>().apply {
            put("xs", XMLConstants.XSD_NS_URI)
            put("fn", XMLConstants.XPATH_FUNCTIONS_NAMESPACE)
            put("map", "${XMLConstants.XPATH_FUNCTIONS_NAMESPACE}/map")
            put("array", "${XMLConstants.XPATH_FUNCTIONS_NAMESPACE}/array")
            put("math", "${XMLConstants.XPATH_FUNCTIONS_NAMESPACE}/math")
            put("err", "http://www.w3.org/2005/xqt-errors")
        }

        @OptIn(XPathInternal::class)
        internal val STEP_DOC_ROOT = FilterExpr(
            TreatAsExpr(
                StaticFunctionCall(Fn.root.name, LocationPath(AxisStep(Axis.SELF, NodeTest.node))),
                SequenceTypeTest.ItemSequenceTest(ItemTypeTest.documentNode, SequenceTypeTest.OccurrenceType.ANY)
            ),
            emptyList()
        )

    }


    internal class Parser(
        private val str: String,
        private val namespaceContext: NamespaceContext,
        private val version: XPathVersion,
        private val posInfo: XmlReader.LocationInfo?
    ) {
        var i: Int = 0

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
            parseError("Comment not closed", start)
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
                parseRequire(tryCurrent('}'))
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
                parseRequire(tryCurrent('}'))
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

        private fun parseItemType(): ItemTypeTest {
            skipWhitespace()
            if (tryCurrentToken('(')) { // ParenthesizedItemType
                val t = parseItemType()
                parseRequire(tryCurrentToken(')'), "Expected ')'")
                return t
            }

            val localOrPrefix = parseNCName()
            if (tryCurrentToken('(')) {
                when (localOrPrefix) {
                    "item" -> {
                        parseRequire(tryCurrentToken(')')) { "The item type specifier has no arguments" }
                        return ItemTypeTest.ItemTestTest
                    }

                    "function" -> when {
                        tryCurrentToken('*') -> {
                            parseRequire(tryCurrentToken(')'))
                            return FunctionTypeTest.ANY
                        }

                        else -> {
                            val params = mutableListOf<SequenceTypeTest>()
                            while (!tryCurrentToken(')')) {
                                params.add(parseSequenceType())
                            }
                            parseRequire(tryCurrentWordToken("as"), "The function type specifier has no return type")
                            val returnType = parseSequenceType()
                            return FunctionTypeTest.Typed(returnType, params)
                        }
                    }

                    "map" -> when {
                        tryCurrent('*') -> {
                            parseRequire(tryCurrentToken(')'))
                            return MapTypeTest.ANY
                        }

                        else -> {
                            val inType = AtomicOrUnionTypeTest(parseEQName())
                            parseRequire(tryCurrentToken(","))
                            val outType = parseSequenceType()
                            parseRequire(tryCurrentToken(')')) { "Map specifiers must be closed by ')'"}
                            return MapTypeTest.Typed(inType, outType)
                        }
                    }

                    "array" -> when {
                        tryCurrent('*') -> {
                            parseRequire(tryCurrentToken(')'))
                            return MapTypeTest.ANY
                        }

                        else -> {
                            val elemType = parseSequenceType()
                            parseRequire(tryCurrentToken(')')) { "Array specifiers must be closed by ')'"}
                            return ArrayTypeTest.Typed(elemType)
                        }
                    }

                    else -> { // Handle the different kinds of node type parameter packs better
                        val nodeType = NodeType.maybeValueOf(localOrPrefix)
                        if (nodeType != null) {
                            --i
                            return NodeTypeTest(nodeType, parseArgs())
                        }
                    }
                }
            }


            if (tryCurrentToken(':')) {
                val localName = parseNCName()
                return AtomicOrUnionTypeTest(QName(lookupNamespace(localOrPrefix), localName, localOrPrefix))
            } else {
                return AtomicOrUnionTypeTest(QName(lookupNamespace(""), localOrPrefix, ""))
            }
        }

        private fun parseSequenceType(): SequenceTypeTest {
            if (tryCurrentWordToken("empty-sequence")) {
                parseRequire(tryCurrentToken('('))
                parseRequire(tryCurrentToken(')'))
                return SequenceTypeTest.EmptySequence
            }
            val itemType = parseItemType()
            val occurrence = when (peekCurrentToken()) {
                '?' -> {
                    ++i
                    SequenceTypeTest.OccurrenceType.OPTIONAL
                }

                '*' -> {
                    ++i
                    SequenceTypeTest.OccurrenceType.ANY
                }

                '+' -> {
                    ++i
                    SequenceTypeTest.OccurrenceType.AT_LEAST_ONE
                }

                else -> SequenceTypeTest.OccurrenceType.SINGLE
            }
            return SequenceTypeTest.ItemSequenceTest(itemType, occurrence)
        }

        private fun parseVariableReference(): VariableRef {
            parseRequire(tryCurrentToken('$'), "Missing '$' in variable reference")
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
                'f' if (tryCurrentWord("for")) -> parseForExprCont()
                'l' if (tryCurrentWord("let")) -> parseLetExprCont()
                's' if (tryCurrentWord("some")) -> parseQuantifiedExprCont(QuantifiedExpr.Kind.SOME)
                'e' if (tryCurrentWord("every")) -> parseQuantifiedExprCont(QuantifiedExpr.Kind.EVERY)
                'i' if (tryCurrentWord("if")) -> parseIfExprCont()
                else -> parseOrExpr()
            }
        }

        private fun parseForExprCont(): ForExpr {
            skipWhitespace()
            val bindings = mutableListOf<ForExpr.Binding>()
            do {
                parseRequire(tryCurrent('$'))
                val varName = parseNCName()
                parseRequire(tryCurrentWordToken("in"))
                val seqExpr = parseExprSingle()
                bindings.add(ForExpr.Binding(varName, seqExpr))
            } while (tryCurrentToken(','))

            parseRequire(tryCurrentWordToken("return"))
            val returned = parseExprSingle()
            return ForExpr(bindings, returned)
        }

        private fun parseLetExprCont(): LetExpr {
            skipWhitespace()
            val bindings = mutableListOf<LetExpr.Binding>()
            do {
                parseRequire(tryCurrentToken('$'))
                val varName = parseNCName()
                parseRequire(tryCurrentToken(":="))
                val rValueExpr = parseExprSingle()
                bindings.add(LetExpr.Binding(varName, rValueExpr))
            } while (tryCurrentToken(','))

            parseRequire(tryCurrentWordToken("return"))
            val returned = parseExprSingle()
            return LetExpr(bindings, returned)
        }

        private fun parseIfExprCont(): IfExpr {
            parseRequire(tryCurrentToken('('), "Missing opening parenthesis in if expression")

            val condition = parseExpr()

            parseRequire(tryCurrentToken(')')) { "Missing closing parenthesis in if expression" }
            parseRequire(tryCurrentWordToken("then"), "Missing 'then' in if expression")

            val thenExpr = parseExprSingle()

            parseRequire(tryCurrentWordToken("else"))
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
                '=' -> {
                    i+=1
                    return BinaryExpr.priority(Operator.EQ, current, parseStringConcatExpr())
                }

                '!' if str[i + 1] == '=' -> {
                    i+=2
                    return BinaryExpr.priority(Operator.NEQ, current, parseStringConcatExpr())
                }

                '<' -> {
                    i+=1
                    return when {
                        tryCurrent('=') -> BinaryExpr.priority(Operator.LE, current, parseStringConcatExpr())

                        tryCurrent('<') -> BinaryExpr.priority(Operator.PRECEDES, current, parseStringConcatExpr())

                        else -> BinaryExpr.priority(Operator.LT, current, parseStringConcatExpr())
                    }
                }
                '>' -> {
                    i+=1
                    return when {
                        tryCurrent('=') -> BinaryExpr.priority(Operator.GE, current, parseStringConcatExpr())
                        tryCurrent('>') -> BinaryExpr.priority(Operator.FOLLOWS, current, parseStringConcatExpr())
                        else -> BinaryExpr.priority(Operator.GT, current, parseStringConcatExpr())
                    }
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

                current = when (peekCurrentToken()) {
                    '+' -> {
                        i+=1
                        BinaryExpr.priority(Operator.ADD, current, parseMultiplicativeExpr())
                    }
                    '-' -> {
                        i+=1
                        BinaryExpr.priority(Operator.SUB, current, parseMultiplicativeExpr())
                    }
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
                parseRequire(tryCurrentWordToken("of"), "Missing 'of' in 'instance of' expression")
                return InstanceOfExpr(e, parseSequenceType())
            }
            return e
        }

        private fun parseTreatExpr(): ExprSingle {
            val e = parseCastableExpr()

            if (tryCurrentWordToken("treat")) {
                parseRequire(tryCurrentWordToken("as"), "Missing 'as' in 'treat as' expression")
                return TreatAsExpr(e, parseSequenceType())
            }
            return e
        }

        private fun parseCastableExpr(): ExprSingle {
            val e = parseCastExpr()

            if (tryCurrentWordToken("castable")) {

                parseRequire(tryCurrentWordToken("as"), "Missing 'as' in 'castable as' expression")

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

            parseRequire(tryCurrentWordToken("as"), "Missing 'as' in 'castable as' expression")

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
                '+' -> {
                    i+=1
                    UnaryExpr.Plus(parseValueExpr())
                }
                '-' -> {
                    i+=1
                    UnaryExpr.Minus(parseValueExpr())
                }
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
            steps.add(parseRequireNotNull(parseStepExpr(), "Missing step in path")) // no step
            while (tryCurrentToken('/')) {
                when {
                    tryCurrentToken("/") -> steps.apply {
                        add(AxisStep(Axis.DESCENDANT_OR_SELF, NodeTypeTest(NodeType.ANY_KIND)))
                        add(parseRequireNotNull(parseStepExpr(), "Missing step after '//' in relative path expression"))
                    }

                    else -> steps.add(parseRequireNotNull(parseStepExpr(), "Missing step after '/' in relative path expression"))
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

                    val nodeTest = parseRequireNotNull(parseNodeTest(), "Missing node test in attribute shorthand")
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
                    if (ncName == "map" && peekCurrentToken('{')) {
                        return parsePostfixExpr(parseMapConstructorCont())
                    } else if (ncName == "array" && peekCurrentToken('{')) {
                        return parsePostfixExpr(parseCurlyArrayConstructorCont())
                    } else if (tryCurrentToken("::")) { // found axis
                        val axis = Axis.from(ncName)
                        val nodeTest = parseRequireNotNull(parseNodeTest()) {
                            "Expected node test after axis name: ${str.substring(start)}"
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
            parseRequire(tryCurrentToken('['), "Missing '[' in square array constructor")
            val exprs = mutableListOf<ExprSingle>()
            if (! tryCurrentToken(']')) { // empty is allowed
                exprs.add(parseExprSingle())
                while (tryCurrentToken(',')) {
                    exprs.add(parseExprSingle())
                }
                parseRequire(tryCurrentToken(']'), "Missing ']' in square array constructor")
            }
            return ArrayConstructor.Square(exprs)
        }

        private fun parseCurlyArrayConstructorCont(): ExprSingle {
            val expr = parseEnclosedExpr()
            return ArrayConstructor.Curly(expr.contentExpr)
        }

        private fun parseUnaryLookup(): ExprSingle {
            parseRequire(tryCurrentToken('?'), "Missing '?' in unary lookup")
            when (val c = peekCurrentToken()) {
                null -> throw IllegalArgumentException("Expected key specifier, found end of expression")

                '*' -> return LookupExpr(null, LookupExpr.AnyKey)

                '(' -> {
                    ++i
                    val key: Expr = when {
                        tryCurrentToken(')') -> SequenceExpr(emptyList())
                        else -> parseExpr()
                    }
                    return LookupExpr(null, LookupExpr.ParenKey(key))
                }

                in '0'..'9' -> {
                    var j = i+1
                    while (str[j] in '0'..'9') { ++j }
                    val value = str.substring(i, j).toLong()
                    i = j
                    return DynamicFunctionCall(
                        LocationPath(AxisStep(Axis.SELF, NodeTest.node)),
                        listOf(IntLiteral(value))
                    )
                }

                else if isNameStartChar(c) -> {
                    val name = parseNCName()
                    return LookupExpr(null, LookupExpr.NCNameKey(name))
                }
            }

            throw IllegalArgumentException("Expected key specifier")
        }

        fun parseMapConstructorCont(): ExprSingle {
            // Expects "map" before
            parseRequire(tryCurrentToken('{'))
            val entries = mutableListOf<MapConstructor.Entry>()
            if (! peekCurrentToken('}')) {
                do {
                    val key = parseExprSingle()
                    parseRequire(tryCurrentToken(':'))
                    val value = parseExprSingle()
                    entries.add(MapConstructor.Entry(key, value))
                } while (tryCurrentToken(','))
            }
            parseRequire(tryCurrentToken('}'))
            return MapConstructor(entries)
        }

        private fun parseSequenceOrParen(): ParenExpr {
            parseRequire(tryCurrentToken('('), "Expected '(' in sequence expression")
            if (tryCurrentToken(')')) return ParenExpr(SequenceExpr(emptyList()))

            val elements: MutableList<ExprSingle> = mutableListOf<ExprSingle>()
            do {
                elements.add(parseExprSingle())
            } while (tryCurrentToken(','))
            parseRequire(tryCurrentToken(')')) { "Expected ')' to finish sequence expression" }

            return ParenExpr(SequenceExpr(elements))
        }

        private fun parseQuantifiedExprCont(kind: QuantifiedExpr.Kind): ExprSingle {

            parseRequire(peekCurrentToken('$'), "Missing '$' in quantified expression ")

            val bindings = mutableListOf<QuantifiedExpr.Binding>()
            do {
                val varName = parseVariableReference()

                parseRequire(tryCurrentWordToken("in"), "Missing 'in' in quantified expression ")

                val source = parseExprSingle()//exprs.singleOrNull() ?: SequenceExpr(exprs)

                bindings.add(QuantifiedExpr.Binding(varName.varName, source))
            } while (tryCurrentToken(','))

            parseRequire(tryCurrentWord("satisfies"), "Missing satisfies in quantified expression")

            val condition = parseExprSingle()

            return QuantifiedExpr(kind, bindings, condition)
        }

        fun parseEnclosedExpr(): EnclosedExpr {
            parseRequire(tryCurrentToken('{'))
            val contentExpr = when {
                !peekCurrentToken('}') -> parseExpr()
                else -> ParenExpr(SequenceExpr(emptyList())) // by default empty sequence
            }
            parseRequire(tryCurrentToken('}'))
            return EnclosedExpr(contentExpr)
        }

        fun parse(): Expr {
            val e = try { 
                parseExpr() 
            } catch (e: NumberFormatException) {
                parseError(e)
            }
            skipWhitespace()
            parseRequire(i>=str.length, "Trailing content in expression")
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
                        steps.add(AxisStep(axis, NodeTypeTest(NodeType.ANY_KIND)))
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
                            steps.add(AxisStep(Axis.DESCENDANT_OR_SELF, NodeTypeTest(NodeType.ANY_KIND)))
                        } else {
                            ++i
                            skipWhitespace()
                            steps.add(parseStep())
                        }
                    }

                    c == '(' -> {
                        parseRequire(steps.isEmpty(), "Primary expression in invalid point")
                        ++i
                        steps.add(FilterExpr(parseExprSingle(), parsePredicates()))
                        skipWhitespace()
                        parseRequire(tryCurrent(')')) { "Expression not ended by ')'" }
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
                else -> parseError("Literal does not start with quote")
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
            parseRequire(i < str.length, "Literal string not closed")
            return StringLiteral(string.toString()).also { ++i } // skip delim
        }

        private fun parseNumber(): NumberLiteral<*> {
            val start = i

            if (str[i] == '-') ++i

            parseRequire(i < str.length && str[i].isDigit(), "@$start> '${str.substring(start, i)}' not a number")

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
            parseRequire(i < str.length, "Empty expression")

            val c = str[i]
            when {
                c == '/' -> { // special case for "empty" expression
//                    ++i
                    return AxisStep(Axis.DESCENDANT_OR_SELF, NodeTypeTest(NodeType.ANY_KIND))
                }

                c == '.' && tryCurrent("..") -> {
                    skipWhitespace()
                    return AxisStep(Axis.PARENT, NodeTypeTest(NodeType.ANY_KIND), parsePredicates())
                }

                c == '.' -> {
                    ++i
                    skipWhitespace()
                    return AxisStep(Axis.SELF, NodeTypeTest(NodeType.ANY_KIND), parsePredicates())
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
                            parseRequireNotNull(parseNodeTest(), "Missing node test in step")
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

                                        else -> parseError("Unexpected arguments to processing instruction test")
                                    }
                                } else parseError("Unexpected arguments to processing instruction test")
                            }

                            is NodeType -> {
                                currentTest = NodeTypeTest(nodeType, args)
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
//                        throw IllegalArgumentException("Unexpected token '${c}' in '$str'")
                }
            }
        }

        private fun parseIfConditionAndConsequences(): IfExpr {
            parseRequire(tryCurrentToken('('))

            val testExpr = parseExpr()
            parseRequire(tryCurrentToken(')'))

            parseRequire(tryCurrentWordToken("then"))

            val thenExpr = parseExprSingle()

            parseRequire(tryCurrentWordToken("else"))
            val elseExpr = parseExprSingle()
            return IfExpr(testExpr, thenExpr, elseExpr)
        }

        private fun parseArgs(): List<ExprSingle> {
            parseRequire(tryCurrent('('))

            // TODO(use parseExpr(isSingle = false)
            val args = mutableListOf<ExprSingle>()
            if (!tryCurrentToken(')')) {
                while (true) {
//                    skipWhitespace()
                    args.add(parseExprSingle())
                    parseRequire(i < str.length, "Missing closing parenthesis")
                    if (tryCurrentToken(')')) break
                    parseRequire(tryCurrentToken(','), "parameters should be separated by ','")
                }
            }

            return args
        }

        private fun parseAttribute(): AxisStep {
            check(tryCurrent('@'))
            skipWhitespace()
            val test: NodeTest =
                parseRequireNotNull(parseNodeTest(), "Missing node test for attribute")
            return AxisStep(Axis.ATTRIBUTE, test, parsePredicates())
        }

        private fun parseNodeTest(): NodeTest {
            val name = parseRequireNotNull(parseEQNameOrWildcard(), "Missing node test in expression")
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
                parseRequire(endBrace >= 0, "Missing closing brace in Braced URI literal")
                val namespace = str.substring(i + 1, endBrace)
                i = endBrace + 1

                if (tryCurrentToken('*')) {
                    return QNameSpec.Namespace(namespace)
                } else {
                    val localPart = parseNCName()
                    return QNameSpec.EQName(namespace, localPart, null)
                }
            } else if (tryCurrentToken(':')) { //namespace separator
                val ns = lookupNamespace(initialWord)
                return when {
                    tryCurrentToken('*') -> QNameSpec.Namespace(ns, prefix = initialWord)

                    else -> QNameSpec.EQName(ns, localName = parseNCName(), prefix = initialWord)
                }
            } else {
                return QNameSpec.EQName(lookupNamespace(""), localName = initialWord, prefix = null)
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
            return NodeTypeTest(nodeType, args)
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
                            null -> parseError("Missing key specifier at end of expression")
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

                            else -> parseError("Invalid key specifier start: $c2", i - 1)
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
                parseRequire(tryCurrentToken(']'), "Predicate not closed by ']'")
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
                    ?: parseError("Missing namespace for prefix '$prefix'")
            }
        }

        fun <T> parseRequireNotNull(value: T?, message: String): T {
            return value ?: parseError(message)
        }

        inline fun <T> parseRequireNotNull(value: T?, message: () -> String): T {
            return value ?: parseError(message())
        }

        fun parseRequire(condition: Boolean, message: String = "Unexpected token") {
            parseRequire(condition, { message })
        }

        inline fun parseRequire(condition: Boolean, message: () -> String) {
            if (! condition) {
                parseError("Invalid expression", message())
            }
        }

        fun parseError(cause: Throwable, startIdx: Int = i): Nothing =
            parseError(null, cause.message?:"<unknown error>", cause, startIdx)

        fun parseError(message: String, startIdx: Int = i): Nothing {
            parseError(null, message, startIdx = startIdx)
        }

        fun parseError(msgPrefix: String?, message: String, cause: Throwable? = null, startIdx: Int = i): Nothing {
            val indent = " ".repeat(6)
            val msg = buildString {
                msgPrefix?.let {
                    append(msgPrefix)
                    if (posInfo !=null && msgPrefix.lastOrNull()!=' ') append(' ')
                }
                if (posInfo != null) append(" at $posInfo")
                append(": ").appendLine(message)
                val a = str.lastIndexOf('\n', startIdx)
                val pos = if (a < 0) startIdx else (startIdx - a)
                val b = str.indexOf('\n', startIdx)
                appendLine(((if (b < 0) str else str.substring(0, b))).prependIndent(indent))
                for (_ in 0 until (pos + indent.length)) append(' ')
                when {
                    b < 0 -> append("^")
                    else -> appendLine("^").append(str.substring(b + 1).prependIndent(indent))
                }
            }
            throw IllegalArgumentException(msg, cause)
        }

        companion object {

            val STEP_DESCENDANT_OR_SELF = AxisStep(Axis.DESCENDANT_OR_SELF, NodeTest.node)

        }
    }

}
