package nl.adaptivity.util.xml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;


/**
 * Created by pdvrieze on 04/11/15.
 */
public interface XmlDeserializable extends XmlSerializable {

  boolean deserializeAttribute(String attributeNamespace, String attributeLocalName, String attributeValue);

  /** Listener called just before the children are deserialized. After attributes have been processed. */
  void onBeforeDeserializeChildren(XMLStreamReader in);

  QName getElementName();
}
