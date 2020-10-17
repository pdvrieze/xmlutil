# Example Soap tags
This example is based upon bug #42

## Output
```xml
<?xml version="1.0" ?>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
    <S:Body>
        <ns2:Ge xmlns:ns2="http://www.gxtlink.com/webservice/">
            <code>0</code>
            <data>
                <project>get</project>
                <unit>p</unit>
            </data>
        </ns2:Ge>
    </S:Body>
</S:Envelope>
```

## Data types
Envelope & Body
```kotlin
/**
 * The Envelope class is a very simple implementation of the SOAP Envelope (ignoring existence of headers). The
 * `@XmlSerialName` annotation specifies how the class is to be serialized, including namespace and prefix to try to use
 * (the serializer will try to reuse an existing prefix for the namespace if it already exists in the document).
 *
 * @property body `body` is a property that contains the body of the envelope. It merely wraps the data, but needs to exist
 *      for the purpose of generating the tag.
 * @param BODYTYPE SOAP is a generic protocol and the wrappers should not depend on a particular body data. That is why
 *      the type is parameterized (this works fine with Serialization).
 */
@Serializable
@XmlSerialName("Envelope", "http://schemas.xmlsoap.org/soap/envelope/", "S")
class Envelope<BODYTYPE> private constructor(
    private val body: Body<BODYTYPE>
                                            ) {

    /**
     * Actual constructor so users don't need to know about the body element
     */
    constructor(data: BODYTYPE) : this(Body(data))

    /**
     * Accessor to the data property that hides the body element.
     */
    val data: BODYTYPE get() = body.data

    override fun toString(): String {
        return "Envelope(body=$body)"
    }

    /**
     * The body class merely wraps a data element (the SOAP standard requires this to be a single element). There is no
     * need for this type to specify the serial name explicitly because:
     *  1. Body is a class, thus serialized as element. The name used is therefore (by default) determined by the name
     *     of the type (`Body`).
     *  2. The namespace (and prefix) used for a type is by default the namespace of the containing tag.
     *  3. Package names are normally elided in the naming
     *
     * The content of data is polymorphic to allow for different message types.
     *
     * @property data The data property contains the actual message content for the soap message.
     */
    @Serializable
//    @XmlSerialName("Body", "http://schemas.xmlsoap.org/soap/envelope/", "S")
    private data class Body<BODYTYPE>(@Polymorphic val data: BODYTYPE)

}
```

GeResult:
```kotlin
/**
 * This class represents an actual message in the gtxlink webservice. It carries its own namespace and has a predefined
 * prefix. There needs to be a prefix here as the content in the example uses the unnamed namespace.
 */
@Serializable
@XmlSerialName("Ge", "http://www.gxtlink.com/webservice/", "ns2")
data class GeResult<out T>(
    /**
     * Code is a primitive, so the name comes from here (the use site). The example data has this in the empty namespace
     * so we must specify this namespace to avoid the default (inheriting the namespace). We want to write this as
     * element so need to specify the `@XmlElement` annotation.
     */
    @XmlSerialName("code", "", "")
    @XmlElement(true)
    val code: Int,
    /**
     * The data property does not require annotation. It is not a primitive so by default the name comes from the actual
     * serialized type and is serialized as element (by default).
     */
    val data: T
                      )
```

GeResultData:
```kotlin
/**
 * This class represents the actual data payload of the message. The name needs to be specified, as well as the namespace
 * (and prefix).
 */
@Serializable
@XmlSerialName("data", "", "")
data class GeResultData(
    @XmlElement(true)
    val project: String,
    @XmlElement(true)
    val unit: String
                       ) {

}
```

## Example usage
```kotlin
/**
 * This is a simple example representing issue #42 on the parsing of soap messages. Note that it doesn't address
 * the idea of making
 */
fun main() {
    val data = Envelope(GeResult(0, GeResultData("get", "p")))
    val module = SerializersModule {
        polymorphic(Any::class) {
            subclass(GeResult::class as KClass<GeResult<GeResultData>>, serializer())
        }
    }
    val xml = XML(module) {
        indentString = "    " // Set indentation
        xmlDeclMode = XmlDeclMode.Minimal // Add an xml decl string
        autoPolymorphic = true // Enable the autopolymorphic mode
    }

    val serializer = serializer<Envelope<GeResult<GeResultData>>>()

    val descriptor = xml.xmlDescriptor(serializer)
    println("SOAP descriptor:\n${descriptor.toString().prependIndent("    ")}\n")

    val encodedString = xml.encodeToString(/*serializer, */data) // both versions are available
    println("SOAP output:\n${encodedString.prependIndent("    ")}\n")

    // the inline reified version is is also available
    val reparsedData = xml.decodeFromString(serializer, encodedString)
    println("SOAP input: $reparsedData")
}
```