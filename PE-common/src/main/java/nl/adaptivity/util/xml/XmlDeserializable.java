package nl.adaptivity.util.xml;

import javax.xml.namespace.QName;


/**
 * Created by pdvrieze on 04/11/15.
 */
public interface XmlDeserializable extends XmlSerializable {

  boolean deserializeAttribute(String pAttributeNamespace, String pAttributeLocalName, String pAttributeValue);

  QName getElementName();
}
