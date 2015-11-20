package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;


/**
 * Created by pdvrieze on 04/11/15.
 */
public interface SimpleXmlDeserializable extends XmlDeserializable {

  boolean deserializeChild(final XmlReader in) throws XmlException;

  boolean deserializeChildText(CharSequence elementText);
}
