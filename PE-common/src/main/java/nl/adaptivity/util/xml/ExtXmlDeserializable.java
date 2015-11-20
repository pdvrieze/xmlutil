package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;


/**
 * Interface that allows more customization on child deserialization than {@link SimpleXmlDeserializable}.
 * Created by pdvrieze on 04/11/15.
 */
public interface ExtXmlDeserializable extends XmlDeserializable{

  /**
   * Called to have all children of the current node deserialized. The attributes have already been parsed. The expected
   * end state is that the streamreader is at the corresponding endElement.
   * @param in The streamreader that is the source of the events.
   */
  void deserializeChildren(XmlReader in) throws XmlException;
}
