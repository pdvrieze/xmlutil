package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xml.serialization.PlatformTestBase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Test for null value returned by a serializer. Test taken (and adapted) from contributed code
 *
 * @see https://github.com/pdvrieze/xmlutil/issues/53
 * @see https://gist.github.com/conorfarrell-coats/f6a6970dd74196b3ab47a289a2dc99c9
 */
class ReturningNullTest : PlatformTestBase<TestDto>(
    TestDto(null, 25),
    TestDto.serializer()
                                           ) {
    override val expectedXML: String
        get() = "<ScheduleLine><DeliveryDate>00.00.0000</DeliveryDate><QuantityAvailable>25</QuantityAvailable></ScheduleLine>"
    override val expectedJson: String
        get() = "{\"DeliveryDate\":\"00.00.0000\",\"QuantityAvailable\":25}"
}

@Serializable
@SerialName("ScheduleLine")
data class TestDto(
    @XmlElement
    @SerialName("DeliveryDate")
    @Serializable(with = OptionalDateSerializerImpl::class)
    val deliveryDate: LocalDate? = null,
    @XmlElement
    @SerialName("QuantityAvailable")
    val quantityAvailable: Int
)

open class OptionalDateSerializer(
    format: String,
    private val nullMarker: String,
    private val replacement: LocalDate? = null
) : KSerializer<LocalDate?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate:$format", PrimitiveKind.STRING).nullable

    private val formatter = DateTimeFormatter.ofPattern(format)

    override fun serialize(encoder: Encoder, value: LocalDate?) {
        encoder.encodeString(value?.format(formatter) ?: nullMarker)
    }

    override fun deserialize(decoder: Decoder): LocalDate? {
        val dateString = decoder.decodeString()
        return if (dateString.isBlank() || (dateString == nullMarker)) replacement else LocalDate.parse(
            dateString,
            formatter
                                                                                                       )
    }
}

object OptionalDateSerializerImpl : OptionalDateSerializer("dd.MM.yyyy", "00.00.0000")
