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

@file:OptIn(XPathInternal::class)

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.impl.token.*
import io.github.pdvrieze.xml.schematypes.values.instances.XsdQNameImpl
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.internal.isNameStartChar
import kotlin.contracts.ExperimentalContracts

internal class XQueryParser(
    str: String,
    private val namespaceContext: NamespaceContext,
    override val xpathVersion: XPathVersion,
    posInfo: XmlReader.LocationInfo?
): Tokenizer(str, posInfo) {

    override val isXPath2 get() = xpathVersion >= XPathVersion.XPath2_0
    override val isXPath30 get() = xpathVersion >= XPathVersion.XPath3_0
    override val isXPath31 get() = xpathVersion >= XPathVersion.XPath3_1

    fun UnresolvedQNameToken.toQName(): QName {
        val effectiveNS = namespace ?: prefix?.let { p ->
            requireNotNull(lookupNamespace(p.toString())) { "No namespace for prefix '$p' found" }
        } ?: ""

        return XsdQNameImpl(effectiveNS.toString(), localName.toString(), prefix?.toString() ?: "")
    }

    fun QNameSpec.EQName.toQName(): QName {
        when (this) {
            is QNameSpec.ResolvedQName -> return name

            is QNameSpec.UriQualifiedName -> {
                val prefix = namespaceContext.getPrefix(namespace) ?: ""
                return XsdQNameImpl(namespace, localName, prefix)
            }
        }
    }

    context(ctx: ParseContext)
    private fun parseItemType(): ItemTypeTest {
        skipWhitespace()
        if (tryCurrentToken('(')) { // ParenthesizedItemType
            val t = parseItemType()
            parseRequire(tryCurrentToken(')'), "Expected ')'")
            return t
        }

        val qNameOrToken = parseQNameOrBuiltin()
        val mark = mark()
        if (tryCurrentToken('(')) {
            when (qNameOrToken) {
                ReservedFunctions.ITEM -> {
                    parseRequire(tryCurrentToken(')')) { "The item type specifier has no arguments" }
                    return ItemTypeTest.ItemTestTest
                }

                ReservedFunctions.FUNCTION if isXPath30 -> when {
                    tryCurrentToken('*') -> {
                        parseRequire(tryCurrentToken(')'))
                        @OptIn(NeedsXPath3_0::class)
                        return FunctionTypeTest.ANY
                    }

                    else -> {
                        val params = mutableListOf<SequenceType>()
                        if (!tryCurrentToken(')')) {

                            do {
                                params.add(parseSequenceType())
                            } while (tryCurrentToken(','))
                            parseRequire(tryCurrentToken(')'), "Closing ) needed in function parameters")
                        }
                        parseRequire(tryCurrent(Keywords.AS), "The function type specifier has no return type")
                        val returnType = parseSequenceType()
                        @OptIn(NeedsXPath3_0::class)
                        return FunctionTypeTest.Typed(returnType, params)
                    }
                }

                ReservedFunctions.MAP if isXPath31 -> when {
                    tryCurrentToken('*') -> {
                        parseRequire(tryCurrentToken(')'))
                        @OptIn(NeedsXPath3_1::class)
                        return MapTypeTest.ANY
                    }

                    else -> {
                        val inType = AtomicOrUnionTypeTest(parseEQNameTokenDelim().toQName())
                        parseRequire(tryCurrentToken(','))
                        val outType = parseSequenceType()
                        parseRequire(tryCurrentToken(')')) { "Map specifiers must be closed by ')'" }
                        @OptIn(NeedsXPath3_1::class)
                        return MapTypeTest.Typed(inType, outType)
                    }
                }

                ReservedFunctions.ARRAY if isXPath31 -> when {
                    tryCurrentToken('*') -> {
                        parseRequire(tryCurrentToken(')'))
                        @OptIn(NeedsXPath3_1::class)
                        return MapTypeTest.ANY
                    }

                    else -> {
                        val elemType = parseSequenceType()
                        parseRequire(tryCurrentToken(')')) { "Array specifiers must be closed by ')'" }
                        @OptIn(NeedsXPath3_1::class)
                        return ArrayTypeTest.Typed(elemType)
                    }
                }

                is NodeType -> { // Handle the different kinds of node type parameter packs better
                    @OptIn(NeedsXPath2::class)
                    val nodeType = qNameOrToken
                    if (nodeType.minVersion <= xpathVersion) {
                        // we checked first whether there were parentheses. Maybe this should be undone.
                        mark.reset()

                        return parseNodeTypeArgs(nodeType)
                    } else {
                        parseError("Unsupported node type ${nodeType.literal} in version $xpathVersion mode")
                    }
                }

                else -> {
                    parseError("Unsupported node type $qNameOrToken in version $xpathVersion mode")
                }
            }
        }


        if (qNameOrToken !is UnresolvedQNameToken) {
            parseError("Unsupported token $qNameOrToken found while parsing item type specifier")
        } else {
            return AtomicOrUnionTypeTest(qNameOrToken.toQName())
        }
    }

    @OptIn(NeedsXPath2::class, NeedsXPath3_0::class)
    private fun parseNodeTypeArgs(nodeType: NodeType): NodeKindTest {
        parseRequireNotNull(tryCurrentToken('('), "Missing ( in node type test")
        val result = when (nodeType) {
            NodeType.DOCUMENT -> when (val nested = tryAnyOf(NodeType.ELEMENT, NodeType.SCHEMA_ELEMENT)) {
                null -> NodeKindTest.DocumentTest()
                else -> NodeKindTest.DocumentTest(parseNodeTypeArgs(nested))
            }

            NodeType.ELEMENT -> when {
                peekNextToken(')') -> NodeKindTest.ElementTest()

                else -> {
                    val name = when {
                        tryCurrent('*') -> QNameSpec.Any
                        else -> QNameSpec.ResolvedQName(parseEQNameTokenDelim().toQName())
                    }
                    if (tryCurrentToken(',')) {
                        val typeName = parseEQNameTokenDelim().toQName()
                        NodeKindTest.ElementTest(name, typeName, tryCurrentToken('?'))
                    } else {
                        NodeKindTest.ElementTest(name)
                    }
                }
            }

            NodeType.ATTRIBUTE -> when {
                peekNextToken(')') -> NodeKindTest.AttributeTest()

                else -> {
                    val name = when {
                        tryCurrent('*') -> QNameSpec.Any
                        else -> QNameSpec.ResolvedQName(parseEQNameTokenDelim().toQName())
                    }
                    if (tryCurrentToken(',')) {
                        val typeName = parseEQNameTokenDelim().toQName()
                        NodeKindTest.AttributeTest(name, typeName, tryCurrentToken('?'))
                    } else {
                        NodeKindTest.AttributeTest(name)
                    }
                }
            }

            NodeType.SCHEMA_ELEMENT -> NodeKindTest.SchemaElementTest(parseEQNameTokenDelim().toQName())

            NodeType.SCHEMA_ATTRIBUTE -> NodeKindTest.SchemaAttributeTest(parseEQNameTokenDelim().toQName())

            NodeType.COMMENT -> NodeKindTest.CommentTest
            NodeType.TEXT -> NodeKindTest.TextTest
            NodeType.ANY_NODE -> NodeKindTest.AnyNode
            NodeType.NAMESPACE_NODE -> NodeKindTest.NamepaceNodeTest

            NodeType.PROCESSING_INSTRUCTION -> {
                when (peekNextChar()) {
                    ')' -> NodeKindTest.ProcInstrTest()
                    '\'', '"' -> NodeKindTest.ProcInstrTest(parseStringLiteral().value)
                    else -> NodeKindTest.ProcInstrTest(parseEQNameTokenDelim().toQName())
                }
            }
        }

        parseRequire(tryCurrentToken(')'), "Missing ) in node type test")
        return result
    }

    @OptIn(NeedsXPath2::class)
    context(ctx: ParseContext)
    private fun parseSequenceType(): SequenceType {
        if (tryCurrent(ReservedFunctions.EMPTY_SEQUENCE)) {
            parseRequire(tryCurrentToken('('))
            parseRequire(tryCurrentToken(')'))
            return SequenceType.EmptySequence
        }
        val itemType = parseItemType()

        val occurrence = when (tryAnyOf('?', '*', '+')) {
            '?' -> SequenceType.OccurrenceType.OPTIONAL

            '*' -> SequenceType.OccurrenceType.ANY

            '+' -> SequenceType.OccurrenceType.AT_LEAST_ONE

            else -> SequenceType.OccurrenceType.SINGLE
        }
        return SequenceType.ItemTypeSequence(itemType, occurrence)
    }

    private fun parseVariableReference(): VariableRef {
        parseRequire(tryCurrentToken('$'), "Missing '$' in variable reference")
        skipWhitespace()
        return VariableRef(parseEQNameTokenUndelim().toQName())
    }

    @OptIn(NeedsXPath2::class)
    context(ctx: ParseContext)
    private fun parseExpr(): Expr {
        val e = parseExprSingle()

        if (!tryCurrent(Operator.COMMA)) return e
//        val _ = tryAnyOf(Operator.COMMA) ?: return e

        val expressions = mutableListOf(e)

        do {
            expressions.add(parseExprSingle())
        } while (tryAnyOf(Operator.COMMA) != null)

        return SequenceExpr(expressions.toList())
    }

    context(ctx: ParseContext)
    private fun parseExprSingle(): ExprSingle {
        val kw = tryAnyOf(Keywords.FOR, Keywords.LET, Keywords.SOME, Keywords.EVERY, Keywords.IF)

        @OptIn(NeedsXPath2::class, NeedsXPath3_0::class)
        return when (kw) {
            Keywords.FOR -> parseForExprCont()
            Keywords.LET -> parseLetExprCont()
            Keywords.SOME -> parseQuantifiedExprCont(QuantifiedExpr.Kind.SOME)
            Keywords.EVERY -> parseQuantifiedExprCont(QuantifiedExpr.Kind.EVERY)
            Keywords.IF -> parseIfExprCont()
            else -> parseLogicExpr()
        }
    }

    @NeedsXPath2
    context(ctx: ParseContext)
    private fun parseForExprCont(): ForExpr {
        skipWhitespace()
        val bindings = mutableListOf<ForExpr.Binding>()
        do {
            parseRequire(tryCurrentToken('$'))
            val varName = parseEQNameTokenUndelim().toQName()
            parseRequire(tryCurrent(Keywords.IN), "Missing 'in' in for expression")
            val seqExpr = parseExprSingle()
            bindings.add(ForExpr.Binding(varName, seqExpr))
        } while (tryCurrent(Operator.COMMA))

        parseRequire(tryCurrent(Keywords.RETURN), "Missing 'return' in for expression")
        val returned = parseExprSingle()
        return ForExpr(bindings.toList(), returned)
    }

    @NeedsXPath3_0
    context(ctx: ParseContext)
    private fun parseLetExprCont(): LetExpr {
        skipWhitespace()
        val bindings = mutableListOf<LetExpr.Binding>()
        do {
            parseRequire(tryCurrentToken('$'))
            val varName = parseEQNameTokenUndelim().toQName()
            parseRequire(tryCurrentToken(":="))
            val rValueExpr = parseExprSingle()
            bindings.add(LetExpr.Binding(varName, rValueExpr))
        } while (tryCurrent(Operator.COMMA))

        parseRequire(tryCurrent(Keywords.RETURN), "Missing 'return' in let expression")
        val returned = parseExprSingle()
        return LetExpr(bindings.toList(), returned)
    }

    @OptIn(NeedsXPath2::class)
    context(ctx: ParseContext)
    private fun parseIfExprCont(): IfExpr {
        parseRequire(tryCurrentToken('('), "Missing opening parenthesis in if expression")

        val condition = parseExpr()

        parseRequire(tryCurrentToken(')')) { "Missing closing parenthesis in if expression" }
        parseRequire(tryCurrent(Keywords.THEN), "Missing 'then' in if expression")

        val thenExpr = parseExprSingle()

        parseRequire(tryCurrent(Keywords.ELSE), "Missing 'else' in if expression")
        return IfExpr(condition, thenExpr, parseExprSingle())
    }

    private inline fun parseOperator(operator: Operator, crossinline parseBelow: () -> ExprSingle): ExprSingle {
        val e = parseBelow()
        if (!tryCurrent(operator)) return e
        val exprs = mutableListOf(e)
        do {
            exprs.add(parseBelow())
        } while (tryCurrent(operator))

        return OperatorExpr(operator, exprs.toList())
    }

    private inline fun parseOperators(vararg operators: Operator, crossinline parseBelow: () -> ExprSingle): ExprSingle {
        var current = parseBelow()

        do {
            current = when (val op = tryAnyOf(*operators)) {
                null -> return current
                else -> OperatorExpr.priority(op, current, parseBelow())
            }
        } while (curPos < str.length)

        return current
    }

    context(ctx: ParseContext)
    private fun parseLogicExpr(): ExprSingle {
        return parseOperators(Operator.OR, Operator.AND) { parseComparisonExpr() }
    }

    @OptIn(NeedsXPath2::class)
    context(ctx: ParseContext)
    private fun parseComparisonExpr(): ExprSingle {
        val current: ExprSingle = parseStringConcatExpr()

        val op = tryAnyOf(Operator.NEQ, Operator.LE, Operator.GE,
            Operator.EQ, Operator.LT, Operator.GT, Operator.PRECEDES, Operator.FOLLOWS,
            Operator.VAL_EQ, Operator.VAL_NEQ, Operator.VAL_LT, Operator.VAL_LE,
            Operator.VAL_GT, Operator.VAL_GE, Operator.IS) ?: return current

        return BinaryExpr.priority(op, current, parseStringConcatExpr())
    }

    @OptIn(NeedsXPath3_0::class)
    context(ctx: ParseContext)
    private fun parseStringConcatExpr(): ExprSingle {
        return parseOperator(Operator.CONCAT) { parseRangeExpr() }
    }

    context(ctx: ParseContext)
    private fun parseRangeExpr(): ExprSingle {
        val e = parseArithmeticExpr()

        @OptIn(NeedsXPath2::class)
        return when {
            tryCurrent(Operator.TO) -> RangeExpr(e, parseArithmeticExpr())
            else -> e
        }
    }

    context(ctx: ParseContext)
    private fun parseArithmeticExpr(): ExprSingle {
        @OptIn(NeedsXPath2::class)
        return parseOperators(Operator.ADD, Operator.SUB, Operator.MUL, Operator.DIV, Operator.IDIV, Operator.MOD) { parseUnionExpr() }
    }

    context(ctx: ParseContext)
    private fun parseUnionExpr(): ExprSingle {
        // no simplification as both are equivalent
        val expr = parseIntersectExceptExpr()

        val _ = tryAnyOf(Operator.UNION, Operator.PIPEUNION) ?: return expr

        val unions = mutableListOf(expr)
        unions.add(parseIntersectExceptExpr())

        while (tryAnyOf(Operator.UNION, Operator.PIPEUNION) != null) {
            unions.add(parseIntersectExceptExpr())
        }
        return OperatorExpr(Operator.UNION, unions.toList())

    }

    @OptIn(NeedsXPath2::class)
    context(ctx: ParseContext)
    private fun parseIntersectExceptExpr(): ExprSingle {
        if (!isXPath2) return parseUnaryExpr()
        return parseOperators(Operator.INTERSECT, Operator.EXCEPT) { parseInstanceofExpr() }
    }

    @NeedsXPath2
    context(ctx: ParseContext)
    private fun parseInstanceofExpr(): ExprSingle {
        val e = parseTreatExpr()

        if (tryCurrent(Keywords.INSTANCE)) {
            parseRequire(tryCurrent(Keywords.OF), "Missing 'of' in 'instance of' expression")
            return InstanceOfExpr(e, parseSequenceType())
        }
        return e
    }

    @NeedsXPath2
    context(ctx: ParseContext)
    private fun parseTreatExpr(): ExprSingle {
        val e = parseCastableExpr()

        if (tryCurrent(Keywords.TREAT)) {
            parseRequire(tryCurrent(Keywords.AS), "Missing 'as' in 'treat as' expression")
            return TreatAsExpr(e, parseSequenceType())
        }
        return e
    }

    @NeedsXPath2
    context(ctx: ParseContext)
    private fun parseCastableExpr(): ExprSingle {
        val e = parseCastExpr()

        if (tryCurrent(Keywords.CASTABLE)) {

            parseRequire(tryCurrent(Keywords.AS), "Missing 'as' in 'castable as' expression")

            skipWhitespace()
            val typeName = parseQName().toQName()
            val allowsEmpty = tryCurrentToken('?')
            return CastableExpr(e, typeName, allowsEmpty)
        }
        return e
    }

    @NeedsXPath2
    context(ctx: ParseContext)
    private fun parseCastExpr(): ExprSingle {
        val expr = parseArrowExpr()

        if (!tryCurrent(Keywords.CAST)) return expr

        parseRequire(tryCurrent(Keywords.AS), "Missing 'as' in 'castable as' expression")
        skipWhitespace()

        val typeName = parseQName().toQName()
        val allowsEmpty = tryCurrentToken('?')
        return CastExpr(expr, typeName, allowsEmpty)
    }

    context(ctx: ParseContext)
    private fun parseArrowExpr(): ExprSingle {
        var expr = parseUnaryExpr()

        @OptIn(NeedsXPath2::class, NeedsXPath3_0::class, NeedsXPath3_1::class)
        while (tryCurrent(Operator.ARROW)) {
            val functionSpecifier = parseArrowFunctionSpecifier()

            val params = parseArgs()
            expr = ArrowFunction(expr, functionSpecifier, params)
        }
        return expr
    }

    @NeedsXPath3_1
    @OptIn(NeedsXPath2::class)
    context(ctx: ParseContext)
    private fun parseArrowFunctionSpecifier(): ArrowFunctionSpecifier {
        skipWhitespace()
        return when (peekNextToken()) {
            '$'.code -> ArrowFunctionSpecifier.VarRefFunc(parseVariableReference().varName)
            // this is just a sequence, not parameters/arguments
            '('.code -> ArrowFunctionSpecifier.SeqFunc(parseParenthesizedExpressionList())
            else -> ArrowFunctionSpecifier.QNameFunc(parseEQNameTokenDelim().toQName())
        }

    }

    context(ctx: ParseContext)
    private fun parseUnaryExpr(): ExprSingle {
        @OptIn(NeedsXPath2::class)
        return when (tryAnyOf(Operator.UNARY_PLUS, Operator.UNARY_MINUS)) {
            Operator.UNARY_PLUS -> UnaryExpr.Plus(parseUnaryExpr())

            Operator.UNARY_MINUS -> UnaryExpr.Minus(parseUnaryExpr())

            else -> parseValueExpr()
        }
    }

    @OptIn(NeedsXPath3_0::class)
    context(ctx: ParseContext)
    private fun parseValueExpr(): ExprSingle {
        val e = parsePathExpr()

        if (!tryCurrent(Operator.MAP)) return e

        val exprs = mutableListOf(e)

        do {
            exprs.add(parsePathExpr())
        } while (tryAnyOf(Operator.MAP) != null)

        return MapExpr(exprs.toList())
    }

    context(ctx: ParseContext)
    private fun parsePathExpr(): ExprSingle {
        val steps = mutableListOf<PrimaryOrStep>()
        if (!peekNextToken('/')) {
            parseRelativePathExprTo(steps)
            return (steps.singleOrNull() as? FilterExpr)?.takeIf { it.predicates.isEmpty() }?.primaryExpr
                ?: LocationPath(false, steps.toList())
        }

        // starts with '/'
        steps.add(XPathExpressionImpl.STEP_DOC_ROOT)

        // TODO special leading lone slash
        if (curPos >= str.length) return LocationPath(true, steps.toList())

        if (tryCurrent("//")) {
            steps.add(STEP_DESCENDANT_OR_SELF)
            parseRelativePathExprTo(steps)
        } else {
            check(tryCurrent('/'))
            skipWhitespace()

            val n = peekNextToken()
            when (val c2 = n.toChar()) {
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
                in '0'..'9' -> parseRelativePathExprTo(steps)

                // letters are step starts
                else if isNameStartChar(c2) -> parseRelativePathExprTo(steps)

                else -> return LocationPath(true, steps.toList())
            }
        }
        return LocationPath(true, steps.toList())
    }

    context(ctx: ParseContext)
    private fun parseRelativePathExprTo(steps: MutableList<PrimaryOrStep>) {
        steps.add(parseRequireNotNull(parseStepExpr(), "Missing step in path")) // no step
        while (tryCurrentToken('/')) {
            when {
                tryCurrentToken("/") -> steps.apply {
                    add(AxisStep(Axis.DESCENDANT_OR_SELF, NodeKindTest.AnyNode))
                    add(parseRequireNotNull(parseStepExpr(), "Missing step after '//' in relative path expression"))
                }

                else -> steps.add(
                    parseRequireNotNull(
                        parseStepExpr(),
                        "Missing step after '/' in relative path expression"
                    )
                )
            }
        }
    }

    context(ctx: ParseContext)
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

        when (val c = peekNextCharToken()) {
            '\u0000' -> return null

            '@' -> {
                assert(tryCurrent('@'))
                val axis = Axis.ATTRIBUTE
                val name = parseRequireNotNull(parseEQNameOrWildcard(), "Missing node test in expression")
                @OptIn(NeedsXPath2::class)
                val nodeTest = NodeKindTest.AttributeTest(name)

                val predicates = parsePredicates()
                @OptIn(NeedsXPath2::class)
                return AxisStep(axis, nodeTest, predicates)
            }

            in '0'..'9' -> return parsePostfixExpr(parseNumber())

            '.' -> when {
                    tryCurrent("..") -> {
                        return AxisStep(Axis.PARENT, NodeTest.node)
                    }

                    peekNextChar(1) in '0'..'9' -> return parsePostfixExpr(parseNumber())

                    else -> {
                        val _= tryCurrent('.')
                        return parsePostfixExpr(ContextItemExpr)
                    }
                }

            '(' if isXPath2 -> @OptIn(NeedsXPath2::class) return parsePostfixExpr(parseParenthesizedExpression())

            '$' -> return parsePostfixExpr(parseVariableReference())

            '\'', '"' -> return parsePostfixExpr(parseStringLiteral())

            '[' if isXPath31 -> {
                @OptIn(NeedsXPath3_1::class)
                return parsePostfixExpr(parseSquareArrayConstructor())
            }

            '?' if isXPath31 ->
                @OptIn(NeedsXPath3_1::class)
                return parsePostfixExpr(parseUnaryLookup())

            else if (c != ':' && isNameStartChar(c)) -> {
                val ncName = parseNCNameUndelim().name

                val maybeReserved = ReservedFunctions.getReserved(ncName)

                @OptIn(NeedsXQuery1::class)
                when (maybeReserved) {
                    ReservedFunctions.MAP if peekNextToken('{') ->
                        @OptIn(NeedsXPath3_1::class)
                        return parsePostfixExpr(parseMapConstructorCont())

                    ReservedFunctions.ARRAY if peekNextToken('{') ->
                        @OptIn(NeedsXPath3_1::class)
                        return parsePostfixExpr(parseCurlyArrayConstructorCont())

/*
                    ReservedFunctions.SWITCH ->
                        parseError("`switch` is reserved in XPath 3.0 (for XQuery)")

                    ReservedFunctions.TYPESWITCH ->
                        parseError("`typeswitch` is reserved in XPath 3.0 (for XQuery)")
*/

                    ReservedFunctions.FUNCTION if peekNextToken('(') -> {
                        parseRequire(ReservedFunctions.FUNCTION.minSpecVersion.isSupported) {
                            "Reserved function name '$ncName' is not supported in this XPath version ($xpathVersion)"
                        }

                        @OptIn(NeedsXPath3_0::class)
                        return parsePostfixExpr(parseInlineFunctionCont())
                    }

                    else -> {}
                }

                if (tryCurrentToken("::")) { // found axis
                    val axis = Axis.from(ncName)
                    val nodeTest = parseNodeTest()
                    return AxisStep(axis, nodeTest, parsePredicates())
                }

                when (val nameOrWildcard = parseEQNameOrWildcard(ncName)) {
                    is QNameSpec.WildCard -> { // function calls are only supported by 3+
                        return AxisStep(Axis.CHILD, nameOrWildcard.asNodeTest(xpathVersion), parsePredicates())
                    }

                    is QNameSpec.EQName -> when (val c = peekNextToken()) {
                        '('.code -> when (val nt = maybeParseNodeTypeTest(nameOrWildcard)) {
                            null -> {
                                if (nameOrWildcard.prefix.isNullOrEmpty()) {
                                    if (maybeReserved != null) {
                                        require(! xpathVersion.includes(maybeReserved.minSpecVersion)) {
                                            "Name: ${nameOrWildcard.localName} is reserved and not allowed as unprefixed function name"
                                        }
                                    }
                                }

                                val funcCall = StaticFunctionCall(nameOrWildcard.toQName(), parseArgs())

                                return parsePostfixExpr(funcCall)
                            }

                            else -> return AxisStep(Axis.CHILD, nt, parsePredicates())
                        }

                        '#'.code if isXPath30 -> {
                            assert(tryCurrent('#'))

                            if (nameOrWildcard.prefix.isNullOrEmpty()) {
                                if (maybeReserved != null) {
                                    require(! xpathVersion.includes(maybeReserved.minSpecVersion)) {
                                        "Name: ${nameOrWildcard.localName} is reserved and not allowed as unprefixed function name"
                                    }
                                }
                            }

                            val idx = parseUnsignedLong()

                            @OptIn(NeedsXPath3_0::class)
                            return parsePostfixExpr(FunctionItem.NamedRef(nameOrWildcard.toQName(), idx))
                        }

                        else -> return AxisStep(Axis.CHILD, nameOrWildcard.asNodeTest(xpathVersion), parsePredicates())
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

    @NeedsXPath3_1
    context(ctx: ParseContext)
    private fun parseSquareArrayConstructor(): ExprSingle {
        parseRequire(tryCurrentToken('['), "Missing '[' in square array constructor")
        val exprs = mutableListOf<ExprSingle>()
        if (!tryCurrentToken(']')) { // empty is allowed
            exprs.add(parseExprSingle())
            while (tryAnyOf(Operator.COMMA) != null) {
                exprs.add(parseExprSingle())
            }
            parseRequire(tryCurrentToken(']'), "Missing ']' in square array constructor")
        }
        return ArrayConstructor.Square(exprs.toList())
    }

    @OptIn(NeedsXPath3_0::class)
    @NeedsXPath3_1
    context(ctx: ParseContext)
    private fun parseCurlyArrayConstructorCont(): ExprSingle {
        assertPrevious(ReservedFunctions.ARRAY)
        val expr = parseEnclosedExpr()
        return ArrayConstructor.Curly(expr.contentExpr)
    }

    @NeedsXPath3_1
    context(ctx: ParseContext)
    private fun parseUnaryLookup(): ExprSingle {
        parseRequire(tryCurrentToken('?'), "Missing '?' in unary lookup")
        val next = peekNextToken()
        if (next < 0) throw IllegalArgumentException("Expected key specifier, found end of expression")
        when (val c = next.toChar()) {
            '*' -> {
                assert(tryCurrent('*'))
                return LookupExpr(null, LookupExpr.AnyKey)
            }

            '(' -> {
                val params = parseParenthesizedExpressionList()

                return LookupExpr(null, LookupExpr.ParenKey(params))
            }

            in '0'..'9' if isXPath30 -> {
                val value = parseUnsignedLong()

                @OptIn(NeedsXPath2::class, NeedsXPath3_0::class)
                return DynamicFunctionCall(
                    LocationPath(AxisStep(Axis.SELF, NodeTest.node)),
                    listOf(LongLiteral(value))
                )
            }

            else if isNameStartChar(c) -> {
                val name = parseNCNameUndelim().name
                return LookupExpr(null, LookupExpr.NCNameKey(name))
            }
        }

        throw IllegalArgumentException("Expected key specifier")
    }

    @NeedsXPath3_1
    context(ctx: ParseContext)
    private fun parseMapConstructorCont(): ExprSingle {
        assertPrevious(ReservedFunctions.MAP)
        // Expects "map" before
        parseRequire(tryCurrentToken('{'))
        val entries = mutableListOf<MapConstructor.Entry>()
        if (!peekNextToken('}')) {
            do {
                val key = parseExprSingle()
                parseRequire(tryCurrentToken(':'))
                val value = parseExprSingle()
                entries.add(MapConstructor.Entry(key, value))
            } while (tryAnyOf(Operator.COMMA) != null)
        }
        parseRequire(tryCurrentToken('}'))
        return MapConstructor(entries.toList())
    }

    @NeedsXPath2
    context(ctx: ParseContext)
    private fun parseParenthesizedExpression(): ExprSingle {
        parseRequire(tryCurrentToken('('), "Expected '(' in sequence expression")
        if (tryCurrentToken(')')) return EmptySequenceExpr


        val elements: MutableList<ExprSingle> = mutableListOf()
        do {
            elements.add(parseExprSingle())
        } while (tryAnyOf(Operator.COMMA) != null)
        parseRequire(tryCurrentToken(')')) { "Expected ')' to finish sequence expression" }

        return ParenExpr(elements.singleOrNull() ?: SequenceExpr(elements.toList()))
    }

    /**
     * For now allow this to generate sequence expressions even if they don't really exist in XPath 2.0
     */
    context(ctx: ParseContext)
    private fun parseParenthesizedExpressionList(): List<ExprSingle> {
        parseRequire(tryCurrentToken('('), "Expected '(' in sequence expression")
        if (tryCurrentToken(')')) return emptyList()

        val elements: MutableList<ExprSingle> = mutableListOf()
        do {
            elements.add(parseExprSingle())
        } while (tryAnyOf(Operator.COMMA) != null)
        parseRequire(tryCurrentToken(')')) { "Expected ')' to finish sequence expression" }

        return elements.toList()
    }

    @NeedsXPath2
    context(ctx: ParseContext)
    private fun parseQuantifiedExprCont(kind: QuantifiedExpr.Kind): ExprSingle {
        assertPrevious(kind)

        val bindings = mutableListOf<QuantifiedExpr.Binding>()
        do {
            val varName = parseVariableReference()

            parseRequire(tryCurrent(Keywords.IN), "Missing 'in' in quantified expression ")

            val source = parseExprSingle()

            bindings.add(QuantifiedExpr.Binding(varName.varName, source))
        } while (tryAnyOf(Operator.COMMA) != null)

        parseRequire(tryCurrent(Keywords.SATISFIES), "Missing satisfies in quantified expression")

        val condition = parseExprSingle()

        return QuantifiedExpr(kind, bindings.toList(), condition)
    }

    @NeedsXPath3_0
    context(ctx: ParseContext)
    private fun parseInlineFunctionCont(): FunctionItem.Inline {
        assertPrevious(ReservedFunctions.FUNCTION)
        parseRequire(tryCurrentToken('('), "Expected function parameters start")
        val params: List<FunctionItem.Inline.Param>
        if (!tryCurrentToken(')')) {
            params = mutableListOf()
            do {
                parseRequire(tryCurrentToken('$'), "Function parameters start with \$")
                val varName = parseEQNameTokenUndelim().toQName()
                val type = if (tryCurrent(Keywords.AS)) parseSequenceType() else null
                params.add(FunctionItem.Inline.Param(varName, type))
            } while (tryCurrent(Operator.COMMA))
            parseRequire(tryCurrentToken(')'), "Expected ')' to finish function parameters")
        } else {
            params = emptyList()
        }
        val returnType = if (tryCurrent(Keywords.AS)) parseSequenceType() else null
        val body = parseEnclosedExpr()
        @OptIn(NeedsXPath3_0::class)
        return FunctionItem.Inline(params, returnType, body.contentExpr)
    }

    context(ctx: ParseContext)
    private fun parseEnclosedExpr(): EnclosedExpr {
        parseRequire(tryCurrentToken('{'))
        @OptIn(NeedsXPath2::class)
        val contentExpr = when {
            !peekNextToken('}') -> parseExpr()

            !isXPath31 -> parseError("Before 3.1 function bodies may not be empty")

            else -> ParenExpr(SequenceExpr(emptyList())) // by default empty sequence
        }
        parseRequire(tryCurrentToken('}'))
        @OptIn(NeedsXPath3_0::class)
        return EnclosedExpr(contentExpr)
    }

    context(ctx: ParseContext)
    fun parseXPathExpr(): Expr {
        val e = try {
            parseExpr()
        } catch (e: IllegalArgumentException) {
            parseError(null, "XPath($xpathVersion): ${e.message}", e)
        } catch (e: NumberFormatException) {
            parseError(null, "XPath($xpathVersion): ${e.message}", e)
        }
        skipWhitespace()
        parseRequire(curPos >= str.length, "Trailing content in expression")
        return e
    }

    context(ctx: ParseContext)
    private fun parseArgs(): List<ExprSingleOrPlaceholder> {
        parseRequire(tryCurrentToken('('))

        if (tryCurrentToken(')')) return emptyList()

        val args = mutableListOf<ExprSingleOrPlaceholder>()
        do {
            val mark = mark()
            if (isXPath30 && tryCurrentToken('?')) {

                if (peekAnyOf(',', ')')) {
                    @OptIn(NeedsXPath3_1::class)
                    args.add(ParamPlaceholder)
                } else {
                    mark.reset()
                    //reset position. This must be a lookup so shortcut there
                    @OptIn(NeedsXPath3_1::class)
                    args.add(parseUnaryLookup())
                }
            } else {
                args.add(parseExprSingle())
            }
            skipWhitespace()
        } while (tryCurrentToken(','))
        parseRequire(tryCurrentToken(')'), "Missing closing parenthesis in parameters")
        return args.toList()

    }

    context(ctx: ParseContext)
    private fun parseNodeTest(): NodeTest {
        val name = parseRequireNotNull(parseEQNameOrWildcard(), "Missing node test in expression")
        return maybeParseNodeTypeTest(name) ?: name.asNodeTest(xpathVersion)
    }

    context(ctx: ParseContext)
    private fun maybeParseNodeTest(): NodeTest? {
        return parseEQNameOrWildcard()?.let { maybeParseNodeTypeTest(it) ?: it.asNodeTest(xpathVersion) }
    }

    @OptIn(ExperimentalContracts::class)
    private fun parseEQNameOrWildcard(): QNameSpec? {
        val word = when {
            tryCurrentToken('*') -> "*"

            isNameStartChar(peekNextToken().toChar()) -> parseNCNameUndelim().name

            else -> return null
        }
        return parseEQNameOrWildcard(word)
    }

    @OptIn(ExperimentalContracts::class)
    private fun parseEQNameOrWildcard(initialWord: String): QNameSpec {

        if (initialWord == "*") {
            return when { // *: must start localname woildcard
                tryCurrent(':') -> QNameSpec.LocalNameWC(parseNCNameUndelim().name)
                else -> QNameSpec.Any
            }
        }

        /* EQName wildcards only allowed in 3.0+. But create them for semantic reasonsfrom the old syntax  */
        @OptIn(NeedsXPath3_0::class)
        if (isXPath30 && initialWord == "Q" && tryCurrent('{')) {
            val endBrace = str.indexOf('}', curPos)
            parseRequire(endBrace >= 0, "Missing closing brace in Braced URI literal")

            val namespace = readUntil('}', endBrace)
            require ('{' !in namespace) { "Extra open brace in namespace: '$namespace'" }

            if (tryCurrent('*')) { // note that whitespace is not allowed
                return QNameSpec.Namespace(namespace)
            } else {
                val localPart = parseNCNameUndelim().name
                return QNameSpec.UriQualifiedName(namespace, localPart)
            }
        } else if (tryCurrentToken(':')) { //namespace separator
            val ns = lookupNamespace(initialWord)
            return when {
                tryCurrent('*') -> QNameSpec.Namespace(ns, initialWord)

                else -> QNameSpec.ResolvedQName(XsdQNameImpl(ns, parseNCNameUndelim().name, initialWord))
            }
        } else {
            return QNameSpec.ResolvedQName(XsdQNameImpl(lookupNamespace(""), initialWord, ""))
        }
    }

    context(ctx: ParseContext)
    private fun maybeParseNodeTypeTest(name: QNameSpec): NodeTest? {
        val nodeType = when (name) {
            is QNameSpec.WildCard -> return name.asNodeTest()

            is QNameSpec.ResolvedQName if (name.prefix.isEmpty() && name.namespace.isEmpty()) ->
                NodeType.maybeValueOf(name.localName, xpathVersion) ?: return null

            else -> return null
        }
        return parseNodeTypeArgs(nodeType)
    }

    context(ctx: ParseContext)
    private fun parsePostfixExpr(primary: ExprSingle): FilterExpr {
        var current = FilterExpr(primary)
        while (true) {
            when (peekNextCharToken()) {
                '[' -> {
                    current = FilterExpr(primary, current.predicates + parsePredicates())
                }

                '(' if isXPath30 -> {
                    val newPrimary: ExprSingle = when {
                        current.predicates.isEmpty() -> current.primaryExpr
                        else -> LocationPath(false, listOf(current))
                    }

                    val args = parseArgs()
                    @OptIn(NeedsXPath3_0::class)
                    current = FilterExpr(DynamicFunctionCall(newPrimary, args))
                }

                '?' if (isXPath31) -> {
                    val _ = tryCurrent('?')
                    val newPrimary: ExprSingle = when {
                        current.predicates.isEmpty() -> current.primaryExpr
                        else -> LocationPath(false, listOf(current))
                    }
                    skipWhitespace()
                    @OptIn(NeedsXPath2::class, NeedsXPath3_1::class)
                    when (val c2 = peekNextChar()) {
                        '\u0000' -> parseError("Missing key specifier at end of expression")
                        '(' -> {
                            val params = parseParenthesizedExpressionList()
                            val newExpr = LookupExpr(newPrimary, LookupExpr.ParenKey(params))
                            current = FilterExpr(newExpr)
                        }

                        '*' -> {
                            val _ = tryCurrent('*')
                            val newExpr = LookupExpr(newPrimary, LookupExpr.AnyKey)
                            current = FilterExpr(newExpr)
                        }

                        in '0'..'9' -> {
                            val newExpr = LookupExpr(newPrimary, LookupExpr.IntegerKey(parseUnsignedInt()))
                            current = FilterExpr(newExpr)
                        }

                        else if isNameStartChar(c2) -> {
                            val name = parseNCNameUndelim().name
                            val newExpr = LookupExpr(newPrimary, LookupExpr.NCNameKey(name))
                            current = FilterExpr(newExpr)
                        }

                        else -> parseError("Invalid key specifier start: $c2", curPos - 1)
                    }
                }

                else -> return current

            }

        }
    }

    context(ctx: ParseContext)
    private fun parsePredicates(): List<Expr> = buildList {
        while (tryCurrentToken('[')) {
            add(parseExpr())
            parseRequire(tryCurrentToken(']'), "Predicate not closed by ']'")
        }
    }

    fun lookupNamespace(prefix: String?): String = when {
        prefix.isNullOrEmpty() -> namespaceContext.getNamespaceURI("") ?: ""
        else -> {
            return namespaceContext.getNamespaceURI(prefix)
                ?: XQUERY_BUILTIN_PREFIX_MAPPINGS[prefix]
                ?: parseError("Missing namespace for prefix '$prefix'")
        }
    }


    internal data class ParseContext(val isXQuery: Boolean)

    companion object {

        val STEP_DESCENDANT_OR_SELF = AxisStep(Axis.DESCENDANT_OR_SELF, NodeTest.node)

        private val XQUERY_BUILTIN_PREFIX_MAPPINGS = HashMap<String, String>().apply {
            put("xs", XMLConstants.XSD_NS_URI)
            put("fn", XMLConstants.XPATH_FUNCTIONS_NAMESPACE)
            put("map", "${XMLConstants.XPATH_FUNCTIONS_NAMESPACE}/map")
            put("array", "${XMLConstants.XPATH_FUNCTIONS_NAMESPACE}/array")
            put("math", "${XMLConstants.XPATH_FUNCTIONS_NAMESPACE}/math")
            put("err", "http://www.w3.org/2005/xqt-errors")
        }

    }
}


context(ctx: XQueryParser.ParseContext)
internal val isXQuery get() = ctx.isXQuery

