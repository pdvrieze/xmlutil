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

package io.github.pdvrieze.formats.xpath.data

import io.github.pdvrieze.formats.xpath.impl.Expr

class EvaluationException : Exception {
    val expr: Expr
    val errorCode: ErrorCodes?

    constructor(expr: Expr, message: String?) : super(message) {
        this.expr = expr
        this.errorCode = null
    }

    constructor(expr: Expr, message: String?, cause: Throwable?) : super(message, cause) {
        this.expr = expr
        this.errorCode = null
    }

    constructor(expr: Expr, cause: Throwable?) : super("Evaluation of expression $expr failed", cause) {
        this.expr = expr
        this.errorCode = null
    }

    constructor(expr: Expr) : super("Evaluation of expression $expr failed") {
        this.expr = expr
        this.errorCode = null
    }

    constructor(errorCode: ErrorCodes, expr: Expr, message: String?) : super(message ?: errorCode.message) {
        this.expr = expr
        this.errorCode = errorCode
    }

    constructor(errorCode: ErrorCodes, expr: Expr, message: String?, cause: Throwable?) : super(message ?: errorCode.message, cause) {
        this.expr = expr
        this.errorCode = errorCode
    }

    constructor(errorCode: ErrorCodes, expr: Expr, cause: Throwable?) : super(errorCode.message, cause) {
        this.expr = expr
        this.errorCode = errorCode
    }

    constructor(errorCode: ErrorCodes, expr: Expr) : super(errorCode.message) {
        this.expr = expr
        this.errorCode = errorCode
    }

    enum class ErrorCodes(val code: String, val message: String) {
        /**Raised when fn:apply is called and the arity of the supplied function is not the same as the number of members in the supplied array.*/
        FOAP0001_WRONG_ARG_CNT("FOAP0001", "Wrong number of arguments."),

        /**This error is raised whenever an attempt is made to divide by zero.*/
        FOAR0001_DIV_BY_ZERO("FOAR0001", "Division by zero."),

        /**This error is raised whenever numeric operations result in an overflow or underflow.*/
        FOAR0002_UNDER_OVER_FLOW("FOAR0002", "Numeric operation overflow/underflow."),

        /**This error is raised when an integer used to select a member of an array is outside the range of values for that array.*/
        FOAY0001_ARRAY_BOUNDS("FOAY0001", "Array index out of bounds."),

        /**This error is raised when the $length argument to array:subarray is negative.*/
        FOAY0002_NEG_ARRAY_LENGTH("FOAY0002", "Negative array length."),

        /**Raised when casting to xs:decimal if the supplied value exceeds the implementation-defined limits for the datatype.*/
        FOCA0001("FOCA0001", "Input value too large for decimal."),

        /**Raised by fn:resolve-QName and fn:QName when a supplied value does not have the lexical form of a QName or URI respectively; and when casting to decimal, if the supplied value is NaN or Infinity.*/
        FOCA0002("FOCA0002", "Invalid lexical value."),

        /**Raised when casting to xs:integer if the supplied value exceeds the implementation-defined limits for the datatype.*/
        FOCA0003("FOCA0003", "Input value too large for integer."),

        /**Raised when multiplying or dividing a duration by a number, if the number supplied is NaN.*/
        FOCA0005("FOCA0005", "NaN supplied as float/double value."),

        /**Raised when casting a string to xs:decimal if the string has more digits of precision than the implementation can represent (the implementation also has the option of rounding).*/
        FOCA0006("FOCA0006", "String to be cast to decimal has too many digits of precision."),

        /**Raised by fn:codepoints-to-string if the input contains an integer that is not the codepoint of a valid XML character.*/
        FOCH0001("FOCH0001", "Codepoint not valid."),

        /**Raised by any function that uses a collation if the requested collation is not recognized.*/
        FOCH0002("FOCH0002", "Unsupported collation."),

        /**Raised by fn:normalize-unicode if the requested normalization form is not supported by the implementation.*/
        FOCH0003("FOCH0003", "Unsupported normalization form."),

        /**Raised by functions such as fn:contains if the requested collation does not operate on a character-by-character basis.*/
        FOCH0004("FOCH0004", "Collation does not support collation units."),

        /**Raised by fn:id, fn:idref, and fn:element-with-id if the node that identifies the tree to be searched is a node in a tree whose root is not a document node.*/
        FODC0001("FODC0001", "No context document."),

        /**Raised by fn:doc, fn:collection, and fn:uri-collection to indicate that either the supplied URI cannot be dereferenced to obtain a resource, or the resource that is returned is not parseable as XML.*/
        FODC0002("FODC0002", "Error retrieving resource."),

        /**Raised by fn:doc, fn:collection, and fn:uri-collection to indicate that it is not possible to return a result that is guaranteed deterministic.*/
        FODC0003("FODC0003", "Function not defined as deterministic."),

        /**Raised by fn:collection and fn:uri-collection if the argument is not a valid xs:anyURI.*/
        FODC0004("FODC0004", "Invalid collection URI."),

        /**Raised (optionally) by fn:doc and fn:doc-available if the argument is not a valid URI reference.*/
        FODC0005("FODC0005", "Invalid argument to fn:doc or fn:doc-available."),

        /**Raised by fn:parse-xml if the supplied string is not a well-formed and namespace-well-formed XML document; or if DTD validation is requested and the document is not valid against its DTD.*/
        FODC0006("FODC0006", "String passed to fn:parse-xml is not a well-formed XML document."),

        /**Raised when fn:serialize is called and the processor does not support serialization, in cases where the host language makes serialization an optional feature.*/
        FODC0010("FODC0010", "The processor does not support serialization."),

        /**This error is raised if the decimal format name supplied to fn:format-number is not a valid QName, or if the prefix in the QName is undeclared, or if there is no decimal format in the static context with a matching name.*/
        FODF1280("FODF1280", "Invalid decimal format name."),

        /**This error is raised if the picture string supplied to fn:format-number or fn:format-integer has invalid syntax.*/
        FODF1310("FODF1310", "Invalid decimal format picture string."),

        /**Raised when casting to date/time datatypes, or performing arithmetic with date/time values, if arithmetic overflow or underflow occurs.*/
        FODT0001("FODT0001", "Overflow/underflow in date/time operation."),

        /**Raised when casting to duration datatypes, or performing arithmetic with duration values, if arithmetic overflow or underflow occurs.*/
        FODT0002("FODT0002", "Overflow/underflow in duration operation."),

        /**Raised by adjust-date-to-timezone and related functions if the supplied timezone is invalid.*/
        FODT0003("FODT0003", "Invalid timezone value."),

        /**Error code used by fn:error when no other error code is provided.*/
        FOER0000("FOER0000", "Unidentified error."),

        /**This error is raised if the picture string or calendar supplied to fn:format-date, fn:format-time, or fn:format-dateTime has invalid syntax.*/
        FOFD1340("FOFD1340", "Invalid date/time formatting parameters."),

        /**This error is raised if the picture string supplied to fn:format-date selects a component that is not present in a date, or if the picture string supplied to fn:format-time selects a component that is not present in a time.*/
        FOFD1350("FOFD1350", "Invalid date/time formatting component."),

        /**Raised by functions such as fn:json-doc, fn:parse-json or fn:json-to-xml if the string supplied as input does not conform to the JSON grammar (optionally with implementation-defined extensions).*/
        FOJS0001("FOJS0001", "JSON syntax error."),

        /**Raised by functions such as map:merge, fn:json-doc, fn:parse-json or fn:json-to-xml if the input contains duplicate keys, when the chosen policy is to reject duplicates.*/
        FOJS0003("FOJS0003", "JSON duplicate keys."),

        /**Raised by fn:json-to-xml if validation is requested when the processor does not support schema validation or typed nodes.*/
        FOJS0004("FOJS0004", "JSON: not schema-aware."),

        /**Raised by functions such as map:merge, fn:parse-json, and fn:xml-to-json if the $options map contains an invalid entry.*/
        FOJS0005("FOJS0005", "Invalid options."),

        /**Raised by fn:xml-to-json if the XML input does not conform to the rules for the XML representation of JSON.*/
        FOJS0006("FOJS0006", "Invalid XML representation of JSON."),

        /**Raised by fn:xml-to-json if the XML input uses the attribute escaped="true" or escaped-key="true", and the corresponding string or key contains an invalid JSON escape sequence.*/
        FOJS0007("FOJS0007", "Bad JSON escape sequence."),

        /**Raised by fn:resolve-QName and analogous functions if a supplied QName has a prefix that has no binding to a namespace.*/
        FONS0004("FONS0004", "No namespace found for prefix."),

        /**Raised by fn:resolve-uri if no base URI is available for resolving a relative URI.*/
        FONS0005("FONS0005", "Base-uri not defined in the static context."),

        /**Raised by fn:load-xquery-module if the supplied module URI is zero-length.*/
        FOQM0001("FOQM0001", "Module URI is a zero-length string."),

        /**Raised by fn:load-xquery-module if no module can be found with the supplied module URI.*/
        FOQM0002("FOQM0002", "Module URI not found."),

        /**Raised by fn:load-xquery-module if a static error (including a statically-detected type error) is encountered when processing the library module.*/
        FOQM0003("FOQM0003", "Static error in dynamically-loaded XQuery module."),

        /**Raised by fn:load-xquery-module if a value is supplied for the initial context item or for an external variable, and the value does not conform to the required type declared in the dynamically loaded module.*/
        FOQM0005("FOQM0005", "Parameter for dynamically-loaded XQuery module has incorrect type."),

        /**Raised by fn:load-xquery-module if no XQuery processor is available supporting the requested XQuery version (or if none is available at all).*/
        FOQM0006("FOQM0006", "No suitable XQuery processor available."),

        /**A general-purpose error raised when casting, if a cast between two datatypes is allowed in principle, but the supplied value cannot be converted: for example when attempting to cast the string "nine" to an integer.*/
        FORG0001("FORG0001", "Invalid value for cast/constructor."),

        /**Raised when either argument to fn:resolve-uri is not a valid URI/IRI.*/
        FORG0002("FORG0002", "Invalid argument to fn:resolve-uri()."),

        /**Raised by fn:zero-or-one if the supplied value contains more than one item.*/
        FORG0003("FORG0003", "fn:zero-or-one called with a sequence containing more than one item."),

        /**Raised by fn:one-or-more if the supplied value is an empty sequence.*/
        FORG0004("FORG0004", "fn:one-or-more called with a sequence containing no items."),

        /**Raised by fn:exactly-one if the supplied value is not a singleton sequence.*/
        FORG0005("FORG0005", "fn:exactly-one called with a sequence containing zero or more than one item."),

        /**Raised by functions such as fn:max, fn:min, fn:avg, fn:sum if the supplied sequence contains values inappropriate to this function.*/
        FORG0006("FORG0006", "Invalid argument type."),

        /**Raised by fn:dateTime if the two arguments both have timezones and the timezones are different.*/
        FORG0008("FORG0008", "The two arguments to fn:dateTime have inconsistent timezones."),

        /**A catch-all error for fn:resolve-uri, recognizing that the implementation can choose between a variety of algorithms and that some of these may fail for a variety of reasons.*/
        FORG0009("FORG0009", "Error in resolving a relative URI against a base URI in fn:resolve-uri."),

        /**Raised when the input to fn:parse-ietf-date does not match the prescribed grammar, or when it represents an invalid date/time such as 31 February.*/
        FORG0010("FORG0010", "Invalid date/time."),

        /**Raised by regular expression functions such as fn:matches and fn:replace if the regular expression flags contain a character other than i, m, q, s, or x.*/
        FORX0001("FORX0001", "Invalid regular expression flags."),

        /**Raised by regular expression functions such as fn:matches and fn:replace if the regular expression is syntactically invalid.*/
        FORX0002("FORX0002", "Invalid regular expression."),

        /**For functions such as fn:replace and fn:tokenize, raises an error if the supplied regular expression is capable of matching a zero length string.*/
        FORX0003("FORX0003", "Regular expression matches zero-length string."),

        /**Raised by fn:replace to report errors in the replacement string.*/
        FORX0004("FORX0004", "Invalid replacement string."),

        /**Raised by fn:data, or by implicit atomization, if applied to a node with no typed value, the main example being an element validated against a complex type that defines it to have element-only content.*/
        FOTY0012("FOTY0012", "Argument to fn:data() contains a node that does not have a typed value."),

        /**Raised by fn:data, or by implicit atomization, if the sequence to be atomized contains a function item.*/
        FOTY0013("FOTY0013", "The argument to fn:data() contains a function item."),

        /**Raised by fn:string, or by implicit string conversion, if the input sequence contains a function item.*/
        FOTY0014("FOTY0014", "The argument to fn:string() is a function item."),

        /**Raised by fn:deep-equal if either input sequence contains a function item.*/
        FOTY0015("FOTY0015", "An argument to fn:deep-equal() contains a function item."),

        /**A dynamic error is raised if the $href argument contains a fragment identifier, or if it cannot be used to retrieve a resource containing text.*/
        FOUT1170("FOUT1170", "Invalid \$href argument to fn:unparsed-text() (etc.)"),

        /**A dynamic error is raised if the retrieved resource contains octets that cannot be decoded into Unicode ·characters· using the specified encoding, or if the resulting characters are not permitted XML characters. This includes the case where the processor does not support the requested encoding.*/
        FOUT1190("FOUT1190", "Cannot decode resource retrieved by fn:unparsed-text() (etc.)"),

        /**A dynamic error is raised if $encoding is absent and the processor cannot infer the encoding using external information and the encoding is not UTF-8.*/
        FOUT1200("FOUT1200", "Cannot infer encoding of resource retrieved by fn:unparsed-text() (etc.)"),

        /**A dynamic error is raised if no XSLT processor suitable for evaluating a call on fn:transform is available.*/
        FOXT0001("FOXT0001", "No suitable XSLT processor available"),

        /**A dynamic error is raised if the parameters supplied to fn:transform are invalid, for example if two mutually-exclusive parameters are supplied. If a suitable XSLT error code is available (for example in the case where the requested initial-template does not exist in the stylesheet), that error code should be used in preference.*/
        FOXT0002("FOXT0002", "Invalid parameters to XSLT transformation"),

        /**A dynamic error is raised if an XSLT transformation invoked using fn:transform fails with a static or dynamic error. The XSLT error code is used if available; this error code provides a fallback when no XSLT error code is returned, for example because the processor is an XSLT 1.0 processor.*/
        FOXT0003("FOXT0003", "XSLT transformation failed"),

        /**A dynamic error is raised if the fn:transform function is invoked when XSLT transformation (or a specific transformation option) has been disabled for security or other reasons.*/
        FOXT0004("FOXT0004", "XSLT transformation has been disabled"),

        /**A dynamic error is raised if the result of the fn:transform function contains characters available only in XML 1.1 and the calling processor cannot handle such characters.*/
        FOXT0006("FOXT0006", "XSLT output contains non-accepted characters"),
    }
}
