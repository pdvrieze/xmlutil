package nl.adaptivity.util.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * Created by pdvrieze on 04/11/15.
 */
public interface SimpleXmlDeserializable extends XmlDeserializable {

  public boolean deserializeChild(final XMLStreamReader pIn) throws XMLStreamException;

  boolean deserializeChildText(String pElementText);
}
