/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;

import javax.xml.namespace.QName;


/**
 * Created by pdvrieze on 04/11/15.
 */
public interface XmlDeserializable {

  /**
   * Handle the given attribue.
   * @param attributeNamespace The namespace of the the attribute.
   * @param attributeLocalName The local name of the attribute
   * @param attributeValue The value of the attribute
   * @return <code>true</code> if handled, <code>false</code> if not. (The caller may use this for errors)
   */
  boolean deserializeAttribute(CharSequence attributeNamespace, CharSequence attributeLocalName, CharSequence attributeValue);

  /** Listener called just before the children are deserialized. After attributes have been processed. */
  void onBeforeDeserializeChildren(XmlReader in) throws XmlException;

  QName getElementName();
}
