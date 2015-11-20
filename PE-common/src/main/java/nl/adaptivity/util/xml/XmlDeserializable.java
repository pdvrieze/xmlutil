package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;

import javax.xml.namespace.QName;


/**
 * Created by pdvrieze on 04/11/15.
 */
public interface XmlDeserializable extends XmlSerializable {

  boolean deserializeAttribute(CharSequence attributeNamespace, CharSequence attributeLocalName, CharSequence attributeValue);

  /** Listener called just before the children are deserialized. After attributes have been processed. */
  void onBeforeDeserializeChildren(XmlReader in) throws XmlException;

  QName getElementName();
}
