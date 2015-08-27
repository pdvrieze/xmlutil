package nl.adaptivity.util.xml;

/**
 * Created by pdvrieze on 27/08/15.
 */
public @interface XmlDeserializer {

  Class<? extends XmlDeserializerFactory> value();

}
