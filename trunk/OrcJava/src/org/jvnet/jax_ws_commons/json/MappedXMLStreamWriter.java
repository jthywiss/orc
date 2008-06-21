package org.jvnet.jax_ws_commons.json;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;

import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.NullNamespaceContext;

/**
 * ORC: Convert XML events into a JSON object.
 * 
 * <p>
 * The org.codehaus.jettison.mapped.MappedXMLStreamWriter has several major
 * bugs. It's also inefficient. And ugly. So I'm rewriting it.
 * 
 * @author quark
 */
public class MappedXMLStreamWriter extends AbstractXMLStreamWriter {
	private MappedNamespaceConvention convention;
	protected Writer writer;
	private NamespaceContext namespaceContext = new NullNamespaceContext();
	/**
	 * What key is used for text content, when an element has both text and
	 * other content?
	 */
	private String textKey = "$";
	/** Stack of open elements. */
	private Stack<JSONProperty> stack = new Stack<JSONProperty>();
	/** Element currently being processed. */
	private JSONProperty current;

	/**
	 * JSON property currently being constructed. For efficiency, this is
	 * concretely represented as either a property with a String value or an
	 * Object value.
	 */
	private abstract class JSONProperty {
		private String key;
		public JSONProperty(String key) {
			this.key = key;
		}
		/** Get the key of the property. */
		public String getKey() {
			return key;
		}
		/** Get the value of the property */
		public abstract Object getValue();
		/** Add text */
		public abstract void addText(String text);
		/** Return a new property object with this property added */
		public abstract JSONPropertyObject withProperty(JSONProperty property, boolean add);
		public JSONPropertyObject withProperty(JSONProperty property) {
			return withProperty(property, true);
		}
	}

	/**
	 * Property with a String value.
	 */
	private final class JSONPropertyString extends JSONProperty {
		private StringBuilder object = new StringBuilder();
		public JSONPropertyString(String key) {
			super(key);
		}
		public Object getValue() {
			return object.toString();
		}
		public void addText(String text) {
			object.append(text);
		}
		public JSONPropertyObject withProperty(JSONProperty property, boolean add) {
			// Duplicate some code from JSONPropertyObject
			// because we can do things with fewer checks, and
			// therefore more efficiently.
			JSONObject jo = new JSONObject();
			try {
				// only add the text property if it's non-empty
				if (object.length() > 0) jo.put(textKey, getValue());
				jo.put(property.getKey(), property.getValue());
			} catch (JSONException e) {
				// Impossible by construction
				throw new AssertionError(e);				
			}
			return new JSONPropertyObject(getKey(), jo);
		}
	}

	/**
	 * Property with a JSONObject value.
	 */
	private final class JSONPropertyObject extends JSONProperty {
		private JSONObject object;
		public JSONPropertyObject(String key, JSONObject object) {
			super(key);
			this.object = object;
		}
		public Object getValue() {
			return object;
		}
		public void addText(String text) {
			try {
				// append to existing text
				// FIXME: should we store text segments in an array
				// when they are separated by child elements? That
				// would be an easy feature to add but we can worry
				// about that later.
				text = object.getString(textKey) + text;
			} catch (JSONException e) {
				// no existing text, that's fine
			}
			try {
				object.put(textKey, text);
			} catch (JSONException e) {
				// Impossible by construction
				throw new AssertionError(e);
			}
		}
		public JSONPropertyObject withProperty(JSONProperty property, boolean add) {
			try {
				Object old = object.get(property.getKey());
				if (!add) return this;
				JSONArray values;
				// Convert an existing property to an array
				// and append to the array
				if (old instanceof JSONArray) {
					values = (JSONArray)old;
				} else {
					values = new JSONArray();
					values.put(old);
				}
				values.put(property.getValue());
				object.put(property.getKey(), values);
			} catch (JSONException e) {
				// Add the property directly.
				try {
					object.put(property.getKey(), property.getValue());
				} catch (JSONException e2) {
					// Impossible by construction
					throw new AssertionError(e2);
				}
			}
			return this;
		}
	}

	public MappedXMLStreamWriter(MappedNamespaceConvention convention, Writer writer) {
		super();
		this.convention = convention;
		this.writer = writer;
	}

	public NamespaceContext getNamespaceContext() {
		return namespaceContext;
	}

	public void setNamespaceContext(NamespaceContext context)
			throws XMLStreamException {
		this.namespaceContext = context;
	}

	public String getTextKey() {
		return textKey;
	}

	public void setTextKey(String textKey) {
		this.textKey = textKey;
	}

	public void writeStartDocument() throws XMLStreamException {
		// The document is an object with one property -- the root element
		current = new JSONPropertyObject(null, new JSONObject());
		stack.clear();
	}
	
	public void writeStartElement(String prefix, String local, String ns) throws XMLStreamException {
		stack.push(current);
		String key = convention.createKey(prefix, ns, local);
		current = new JSONPropertyString(key);
	}
	
	public void writeAttribute(String prefix, String ns, String local, String value) throws XMLStreamException {
		String key = convention.isElement(prefix, ns, local)
			? convention.createKey(prefix, ns, local)
			: convention.createAttributeKey(prefix, ns, local);
		JSONPropertyString prop = new JSONPropertyString(key);
		prop.addText(value);
		current = current.withProperty(prop, false);
	}

	public void writeAttribute(String ns, String local, String value) throws XMLStreamException {
		writeAttribute(null, ns, local, value);
	}

	public void writeAttribute(String local, String value) throws XMLStreamException {
		writeAttribute(null, local, value);
	}

	public void writeCharacters(String text) throws XMLStreamException {
		current.addText(text);
	}
	
	public void writeEndElement() throws XMLStreamException {
		if (stack.isEmpty()) throw new XMLStreamException("Too many closing tags.");
		current = stack.pop().withProperty(current);
	}

	public void writeEndDocument() throws XMLStreamException {
		if (!stack.isEmpty()) throw new XMLStreamException("Missing some closing tags.");
		// We know the root is a JSONPropertyObject so this cast is safe
		writeJSONObject((JSONObject)current.getValue());
		try {
			writer.flush();
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
	}
	
	protected void writeJSONObject(JSONObject root) throws XMLStreamException {
		try {
			if (root == null) writer.write("null");
			else root.write(writer);
		} catch (JSONException e) {
			throw new XMLStreamException(e);
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
	}

	public void close() throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	public void flush() throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	public String getPrefix(String arg0) throws XMLStreamException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getProperty(String arg0) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDefaultNamespace(String arg0) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	public void setPrefix(String arg0, String arg1) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	public void writeDefaultNamespace(String arg0) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	public void writeEntityRef(String arg0) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	public void writeNamespace(String arg0, String arg1) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	public void writeProcessingInstruction(String arg0) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	public void writeProcessingInstruction(String arg0, String arg1) throws XMLStreamException {
		// TODO Auto-generated method stub

	}
}