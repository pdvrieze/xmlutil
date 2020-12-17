
@XmlSerialName("StringWithMarkup", "http://pubchem.ncbi.nlm.nih.gov/pug_view", "")
@Serializable
data class StringWithMarkup(
    @XmlElement(true) @SerialName("String") val string: String = "",
    val markup: List<String> = emptyList()
                           )
