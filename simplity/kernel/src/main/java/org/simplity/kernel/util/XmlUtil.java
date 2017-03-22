/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.MapDetails;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.value.Value;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;



/***
 * Utility that binds xml to object graph. A simple substitute to JAXB. We use
 * it because we try to keep the objects as clean as possible. Using this
 * utility we get away without annotations(well almost) and setters. This is
 * meant specifically for the design components that are saved as xml. This
 * design is not suitable for binding domain/business data. Note that this is
 * one-way binding. We expect that the xmls are maintained using either
 * xsd-based editors, or eclipse plugins. Here we focus only on run-time.
 * Following conventions are used to keep it simple.
 *
 * 1. We consider number, boolean, char, string, date, enum, pattern (regex) and
 * Expr (our version of expression) as primitive, or more aptly values.
 *
 * 2. in the xml schema, all primitive/value attributes are preferably specified
 * as attributes. Like age="22". But not <age>22</age>. This is generally
 * simple. However, if the text contains special characters, a CDATA could be a
 * better alternative.
 *
 * e.g. <expression>[[-----------------------]]</expression>
 *
 * 3. Array of primitives are specified as a comma separated list. We do not use
 * comma as a valid character in any of the text values that we intend to put in
 * a list, and hence this is okay. for example colors="red,blue green".
 *
 * 4. We support Map<> with only String as key. This field MUST be initialized
 * in the class definition. Since Java does not retain the class of the Map
 * value, we assume that the class is in the same package as the container
 * class. In case it is different, use MapDetails annotation to specify. Also,
 * we use name as the attribute to index the object on. In case you have
 * something different use annotation. As of now you can not have sub-classes of
 * value that belong to different packages. (we will extend the design when we
 * reach there)
 *
 * like Map<String, MyAbstractClass> fieldName = new HashMap<String,
 * MyAbstractClass>();
 *
 * In the xml, use the field name as the wrapper element to the list of Map
 * member elements
 *
 * <pre>
 * <fieldName>
 * 	<concreteClass a1="v1" a2="v2"....... />
 * 		.......
 * 	.......
 * </fieldName>
 * </pre>
 *
 * 5. Arrays are not to be initialized in object. In case of array of objects
 * (non-primitive) use xml schema same as for map explained above.
 *
 * 6. Non-primitive field. If you expect the same class, and not any sub-class,
 * then you should initialize it in your class.
 *
 * MyClass myField = new MyClass();
 *
 *
 * 7. Non-primitive field - Super class declaration with a choice of sub-class.
 * In this case, obviously, you can not instantiate it in your class. Use a
 * wrapper element with fieldName as tag name, with exactly one child element
 * with the concrete class name as tag name. For elegance, you may use camel
 * case of the class name, and we take care of ClassCasing it.
 *
 * for example
 *
 * <pre>
 * <myDataType>
 * 		<textDataType ........ />
 * </myDataType>
 * </pre>
 */
public class XmlUtil {
	private static final String DEFAULT_MAP_KEY = "name";
	private static final String TRUE_VALUE = "true";
	private static final String FALSE_VALUE = "false";
	/**
	 * attribute name of an element that specifies the fully qualified class
	 * name to be mapped to the element
	 */
	private static final String CLASS_NAME = "_class";
	private static final String COMP_LIST = "_compList";
	private static final String COMPONENTS = "components";
	private static final String NAME_ATTRIBUTE = "name";
	private static final String ENTRY = "entry";
	private static final String CLASS_NAME_ATTRIBUTE = "className";
	private static final String UTF8 = "UTF-8";

	private static final DocumentBuilderFactory docFactory = getFactory();
	/**
	 * bind data from an xml stream into object
	 *
	 * @param stream
	 *            with valid xml
	 *
	 * @param object
	 *            instance to which the data from xml is to be loaded to
	 * @return true if all OK. false if the resource is not laoded
	 * @throws XmlParseException
	 */
	public static boolean xmlToObject(InputStream stream, Object object)
			throws XmlParseException {
		try{
			Element rootElement = getDocument(stream).getDocumentElement();
			elementToObject(rootElement, object);
			return true;
		}catch(Exception e){
			Tracer.trace("Error while reading resource " + e.getMessage());
			return false;
		}
	}

	/**
	 * bind data from an xml stream into object
	 *
	 * @param fileName
	 *            relative to file manager's root, and not absolute path
	 *
	 * @param object
	 *            instance to which the data from xml is to be loaded to
	 * @return true if we are able to load. false otherwise
	 * @throws XmlParseException
	 */
	public static boolean xmlToObject(String fileName, Object object)
			throws XmlParseException {
		InputStream stream = null;
		try {
			stream = FileManager.getResourceStream(fileName);
			if (stream == null) {
				Tracer.trace("Resource " + fileName + " not found.");
				return false;
			}
			Element rootElement = getDocument(stream).getDocumentElement();
			elementToObject(rootElement, object);
			return true;
		} catch (Exception e) {
			Tracer.trace(e, "Resource " + fileName + " failed to load.");
			return false;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (Exception e) {
					//
				}
			}
		}
	}

	/**
	 * load components or name-className maps into collection
	 *
	 * @param stream
	 *            xml
	 * @param objects
	 *            collection to which components/entries are to be added to
	 * @param packageName
	 *            package name ending with a '.', so that when we add simple
	 *            class name it becomes a fully-qualified class name
	 * @throws XmlParseException
	 */
	public static void xmlToCollection(InputStream stream,
			Map<String, Object> objects, String packageName)
					throws XmlParseException {
		Node node = getDocument(stream).getDocumentElement().getFirstChild();
		while (node != null) {
			if (node.getNodeName().equals(COMPONENTS) == false) {
				node = node.getNextSibling();
				continue;
			}
			/*
			 * we got the element we need
			 */
			Node firstNode = node.getFirstChild();
			if (firstNode == null) {
				break;
			}
			if (packageName == null) {
				/*
				 * list of name and className
				 */
				loadEntries(firstNode, objects);
			} else {
				/*
				 * list of components
				 */
				loadObjects(firstNode, objects, packageName);
			}
			return;
		}

		/*
		 * we did not get the component element at all
		 */
		Tracer.trace("XML has no components in it.");
	}

	/**
	 * load components or name-className maps into collection
	 *
	 * @param fileName
	 *            relative to FileManager root, and not absolute path of the
	 *            file xml
	 * @param objects
	 *            collection to which components/entries are to be added to
	 * @param packageName
	 *            package name ending with a '.', so that when we add simple
	 *            class name it becomes a fully-qualified class name
	 * @return true if we are able to load from the file. false otherwise
	 */
	public static boolean xmlToCollection(String fileName,
			Map<String, Object> objects, String packageName) {
		InputStream stream = null;
		try {
			stream = FileManager.getResourceStream(fileName);
			if (stream == null) {
				Tracer.trace("Unable to open file " + fileName
						+ " failed to load.");
				return false;
			}
			xmlToCollection(stream, objects, packageName);
			return true;
		} catch (Exception e) {
			Tracer.trace(e, "Resource " + fileName + " failed to load.");
			return false;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (Exception e) {
					//
				}
			}
		}
	}

	/**
	 * elements are loaded and added to objects collection
	 *
	 * @param firstNode
	 * @param objects
	 * @param packageName
	 *            includes a '.' at the end so that packageName + className is a
	 *            valid qualified classNAme
	 * @throws XmlParseException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void loadObjects(Node firstNode, Map objects,
			String packageName) throws XmlParseException {
		Node node = firstNode;
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element ele = (Element) node;
				String className = TextUtil.nameToClassName(node.getNodeName());
				try {
					Class klass = Class.forName(packageName + className);
					Object object = klass.newInstance();
					String compName = ele.getAttribute(NAME_ATTRIBUTE);
					if (objects.containsKey(compName)) {
						Tracer.trace(compName + " is a duplicate " + className
								+ ". Component definition skipped.");
					} else {
						elementToObject(ele, object);
						objects.put(compName, object);
					}
				} catch (ClassNotFoundException e) {
					Tracer.trace(className
							+ " is not a valid class in package " + packageName
							+ ". element ignored while loading components");
				} catch (InstantiationException e) {
					Tracer.trace(className + " in package " + packageName
							+ "Could not be instantiated: " + e.getMessage());
				} catch (IllegalAccessException e) {
					Tracer.trace(className + " in package " + packageName
							+ "Could not be instantiated: " + e.getMessage());
				}
			}
			node = node.getNextSibling();
		}
	}

	/**
	 * name-className pairs are added to objects collection
	 *
	 * @param firstNode
	 * @param objects
	 * @param initializeThem
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void loadEntries(Node firstNode, Map objects) {
		Node node = firstNode;
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = node.getNodeName();
				if (nodeName.equals(ENTRY)) {
					Element ele = (Element) node;
					String compName = ele.getAttribute(NAME_ATTRIBUTE);
					String className = ele.getAttribute(CLASS_NAME_ATTRIBUTE);
					if (compName == null || className == null) {
						Tracer.trace("We expect attributes " + NAME_ATTRIBUTE
								+ " and " + CLASS_NAME_ATTRIBUTE
								+ " as attributes of element " + ENTRY
								+ ". Element ignored");
					} else {
						if (objects.containsKey(compName)) {
							Tracer.trace(compName
									+ " is a duplicate entry. class name definition ignored.");
						} else {
							objects.put(compName, className);
						}
					}
				} else {
					Tracer.trace("Expecting an element named " + ENTRY
							+ " but found " + nodeName + ". Element ignored.");
				}
			}
			node = node.getNextSibling();
		}
	}

	/***
	 * parses a file into a DOM
	 *
	 * @param stream
	 * @return DOM for the xml that the file contains
	 */
	private static Document getDocument(InputStream stream)
			throws XmlParseException {
		Document doc = null;
		String msg = null;

		try {
			DocumentBuilder builder = docFactory.newDocumentBuilder();
			doc = builder.parse(stream);
		} catch (SAXParseException e) {
			msg = "Error while parsing xml text. " + e.getMessage()
					+ "\n At line " + e.getLineNumber() + " and column "
					+ e.getColumnNumber();
		} catch (Exception e) {
			msg = "Error while reading resource file. " + e.getMessage();
		}
		if (msg != null) {
			throw new XmlParseException(msg);
		}
		return doc;
	}

	/***
	 * create the factory, once and for all
	 * @return Factory to create DOM
	 */
	private static  DocumentBuilderFactory getFactory() {
		/*
		 * workaround for some APP servers that have classLoader related issue with using xrececs
		 */
		ClassLoader savedClassloader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(XmlUtil.class.getClassLoader());
		DocumentBuilderFactory factory = new DocumentBuilderFactoryImpl();
		Thread.currentThread().setContextClassLoader(savedClassloader);
		factory.setIgnoringComments(true);
		factory.setValidating(false);
		factory.setCoalescing(false);
		factory.setXIncludeAware(false);
		factory.setNamespaceAware(false);
		return factory;
	}

	/**
	 * bind data from an element to the object, provided you have followed our
	 * conventions
	 *
	 * @param element
	 * @param object
	 * @throws XmlParseException
	 */
	public static void elementToObject(Element element, Object object)
			throws XmlParseException {
		Map<String, Field> fields = ReflectUtil.getAllFields(object);

		/*
		 * attributes of the element are mapped to value/primitive fields
		 */
		setAttributes(object, fields, element);

		/*
		 * child elements could be either primitive or a class
		 */
		Node child = element.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Field field = fields.get(child.getNodeName());
				if (field != null) {
					Element childElement = (Element) child;
					String value = getElementValue(childElement);
					if (value != null) {
						/*
						 * element had a primitive value
						 */
						ReflectUtil.setPrimitive(object, field, value);
					} else {
						/*
						 * element represents another object
						 */
						setObject(object, field, childElement);
					}
				} else {
					Tracer.trace("xml element "
							+ child.getNodeName()
							+ " is ignored because there is no target field with that name");
				}
			}
			child = child.getNextSibling();
		}
	}

	/**
	 * copy objects from list into map
	 *
	 * @param map
	 *            to which objects are to be added
	 * @param objects
	 *            to be added to the map
	 * @throws XmlParseException
	 */
	private static void fillMap(Map<String, Object> map, List<?> objects,
			String keyFieldName) throws XmlParseException {
		if (objects.size() == 0) {
			return;
		}
		StringBuilder msg = new StringBuilder();
		try {
			for (Object object : objects) {
				/*
				 * get the field value for indexing this object
				 */
				Field field = getField(object.getClass(), keyFieldName);
				if (field != null) {
					field.setAccessible(true);
					Object key = field.get(object);
					if (key != null) {
						map.put(key.toString(), object);
						continue;
					}
				}
				msg.append("\nUnable to get value of attribute " + keyFieldName
						+ " for an instance of class "
						+ object.getClass().getName());
			}
		} catch (Exception e) {
			msg.append("\nError while adding a member into map using "
					+ keyFieldName + " as key\n" + e.getMessage());
		}
		if (msg.length() > 0) {
			throw new XmlParseException(msg.toString());
		}
	}

	/**
	 * @param type
	 * @param fieldName
	 * @return
	 */
	private static Field getField(Class<?> type, String fieldName) {
		Class<?> currentType = type;
		while (!currentType.equals(Object.class)) {
			for (Field field : currentType.getDeclaredFields()) {
				if (field.getName().equals(fieldName)) {
					return field;
				}
			}
			currentType = currentType.getSuperclass();
		}
		return null;
	}

	/**
	 * get all fields for a class
	 *
	 * @param type
	 * @return all fields indexed by their names
	 */
	private static Map<String, Field> getAllFields(Class<?> type) {
		Map<String, Field> fields = new HashMap<String, Field>();
		Class<?> currentType = type;
		while (!currentType.equals(Object.class)) {
			for (Field field : currentType.getDeclaredFields()) {
				int mod = field.getModifiers();
				/*
				 * by convention, our fields should not have any modifier
				 */
				if (mod == 0 || Modifier.isProtected(mod)
						&& !Modifier.isStatic(mod)) {
					fields.put(field.getName(), field);
				}
			}
			currentType = currentType.getSuperclass();
		}
		return fields;
	}

	/**
	 * parse element into an object and set it as value of the field.
	 *
	 * @param object
	 *            of which this is a field
	 * @param field
	 *            to which object value is to be assigned to
	 * @param element
	 *            from which object is to be parsed
	 * @throws XmlParseException
	 */
	@SuppressWarnings("unchecked")
	private static void setObject(Object object, Field field, Element element)
			throws XmlParseException {
		field.setAccessible(true);
		Object fieldObject = null;
		try {
			field.setAccessible(true);
			fieldObject = field.get(object);
			Class<?> fieldType = field.getType();
			if (fieldType.isArray()) {
				Class<?> componentType = fieldType.getComponentType();
				List<?> objects = elementToList(element, field, componentType,
						object);
				if (objects == null || objects.size() == 0) {
					return;
				}
				int nbr = objects.size();
				fieldObject = Array.newInstance(componentType, nbr);
				for (int i = 0; i < nbr; i++) {
					Array.set(fieldObject, i, objects.get(i));
				}
				field.setAccessible(true);
				field.set(object, fieldObject);
				return;
			}

			/*
			 * if the field is already initialized, it is Map or Concrete class
			 * object
			 */
			if (fieldObject != null) {

				if (fieldObject instanceof Map) {
					/*
					 * we have a special case of componentList
					 */
					if (field.getName().equals(COMP_LIST)) {
						fillMap(element, (Map<String, String>) fieldObject);
						return;
					}
					/*
					 * another special case of attr-value of element itself
					 * being saved as map
					 */
					if (fillAttsIntoMap(element,
							(Map<String, String>) fieldObject)) {
						return;
					}
					String mapKey = DEFAULT_MAP_KEY;
					MapDetails ante = field.getAnnotation(MapDetails.class);
					if (ante != null) {
						String txt = ante.indexFieldName();
						if (txt != null) {
							mapKey = txt;
						}
					}
					List<?> objects = elementToList(element, field,
							object.getClass(), object);
					fillMap((Map<String, Object>) fieldObject, objects, mapKey);
				} else {
					elementToObject(element, fieldObject);
				}
				return;
			}
			if (fieldType.isInterface()
					|| Modifier.isAbstract(fieldType.getModifiers())) {
				/*
				 * It is super class/interface. As per our syntax, this element
				 * would be wrapper for the concrete class-element
				 */
				fieldObject = elementWrapperToSubclass(element, field, object);
				if (fieldObject != null) {
					field.setAccessible(true);
					field.set(object, fieldObject);
				} else {
					Tracer.trace("No instance provided for field "
							+ field.getName());
				}
				return;
			}
			/*
			 * we have an object as the child
			 */
			fieldObject = fieldType.newInstance();
			elementToObject(element, fieldObject);
			field.setAccessible(true);
			field.set(object, fieldObject);
		} catch (Exception e) {
			throw new XmlParseException("Error while binding xml to object : "
					+ e.getMessage());
		}

	}

	/**
	 * @param element
	 * @param map
	 * @return
	 */
	private static boolean fillAttsIntoMap(Element element,
			Map<String, String> map) {
		/*
		 * this case is applicable only if there are no child elements
		 */
		NodeList children = element.getChildNodes();
		if (children != null && children.getLength() > 0) {
			return false;
		}

		/*
		 * in case this is not a special case, it will not have attributes, and
		 * hence we are safe
		 */
		NamedNodeMap attrs = element.getAttributes();
		if(attrs != null){
			int n = attrs.getLength();
			for (int i = 0; i < n; i++) {
				Node att = attrs.item(i);
				map.put(att.getNodeName(), att.getNodeValue());
			}
		}
		return true;
	}

	/**
	 * special map of string string
	 *
	 * @param element
	 *            whose child nodes are map entries
	 * @param map
	 *            map
	 */
	private static void fillMap(Element element, Map<String, String> map) {
		String valueName = null;
		Node child = element.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) child;
				if (valueName == null) {
					NamedNodeMap attribs = child.getAttributes();
					if (attribs == null || attribs.getLength() != 2) {
						throw new ApplicationError(
								"Special element "
										+ COMP_LIST
										+ " should have child nodes with just two attributes, name and value");
					}
					String attrName = attribs.item(0).getNodeName();
					if (attrName.equals(DEFAULT_MAP_KEY)) {
						valueName = attribs.item(1).getNodeName();
					} else {
						if (attribs.item(1).getNodeName()
								.equals(DEFAULT_MAP_KEY) == false) {
							throw new ApplicationError(
									"Special element "
											+ COMP_LIST
											+ " should have child nodes with just two attributes, name and value");
						}
						valueName = attrName;
					}
				}
				String key = childElement.getAttribute(DEFAULT_MAP_KEY);
				String value = childElement.getAttribute(valueName);
				if (key == null || value == null) {
					throw new ApplicationError(
							"key or value missing for a value map");
				}
				map.put(key, value);
			}
			child = child.getNextSibling();
		}
	}

	/**
	 * element with field name as tag followed with a child element with the
	 * concrete class name as tag
	 *
	 * @param wrapper
	 *            element with field name as tag
	 * @param field
	 *            to which this object is to be assigned to
	 * @return
	 * @throws XmlParseException
	 */
	private static Object elementWrapperToSubclass(Element wrapper,
			Field field, Object parentObject) throws XmlParseException {
		/*
		 * is this for a class?
		 */

		/*
		 * though we expect exactly one element, you never know about comments
		 */
		Node element = wrapper.getFirstChild();
		while (element != null) {
			if (element.getNodeType() == Node.ELEMENT_NODE) {
				return elementToSubclass((Element) element, field,
						field.getType(), parentObject);
			}
			element = element.getNextSibling();
		}
		return null;
	}

	/**
	 * @param element
	 *            with concrete class name as its tag. This class is expected to
	 *            be in the same package as referenceType, unless the field is
	 *            annotated with the package name. A special attribute name
	 *            "_class", if present would be the class name
	 * @param field
	 *            to which this is destined for. Used to check for parent class
	 *            as annotation
	 * @param referenceType
	 *            a class whose package may be shared with this class
	 *
	 * @param parentObject
	 *            parent of this object
	 * @return
	 * @throws XmlParseException
	 */
	private static Object elementToSubclass(Element element, Field field,
			Class<?> referenceType, Object parentObject)
					throws XmlParseException {

		Object thisObject = null;
		String elementName = element.getTagName();
		try {
			String className = element.getAttribute(CLASS_NAME);
			if (className == null || className.length() == 0) {
				String packageName = null;
				MapDetails ant = field.getAnnotation(MapDetails.class);
				if (ant != null) {
					packageName = ant.packgaeName();
				} else {
					packageName = referenceType.getPackage().getName();
				}
				/*
				 * we take package name either from annotation on the field or
				 * from the reference type
				 */
				className = packageName + '.' + TextUtil.nameToClassName(elementName);
				thisObject = Class.forName(className).newInstance();
			}
			elementToObject(element, thisObject);
			return thisObject;
		} catch (Exception e) {
			throw new XmlParseException("error while parsing " + elementName
					+ " element as a wrapped-element\n " + e.getMessage());
		}
	}

	/**
	 * parse child elements of the passed element into a List
	 *
	 * @param element
	 *            parent element
	 * @param field
	 *            to which this list is destined for. Used to check for
	 *            annotation for package.
	 * @param referenceType
	 *            class, super-class or parent class of the expected object.
	 *            Used to get the package name.
	 * @param parentObject
	 *            parent of this object
	 * @return
	 * @throws XmlParseException
	 */
	private static List<?> elementToList(Element element, Field field,
			Class<?> referenceType, Object parentObject)
					throws XmlParseException {
		List<Object> objects = new ArrayList<Object>();
		Node child = element.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Object object = elementToSubclass((Element) child, field,
						referenceType, parentObject);
				if (object == null) {
					throw new ApplicationError("Unable to parse element "
							+ child.getNodeName());
				}
				objects.add(object);
			}
			child = child.getNextSibling();
		}
		return objects;
	}

	/**
	 * set attributes from an element as primitive values of fields
	 *
	 * @param object
	 *            to which fields are assigned
	 * @param fields
	 *            collection of all fields for this class
	 * @param element
	 *            that has the attributes
	 * @throws XmlParseException
	 * @throws DOMException
	 */
	private static void setAttributes(Object object, Map<String, Field> fields,
			Element element) throws DOMException, XmlParseException {
		NamedNodeMap attributes = element.getAttributes();
		if(attributes == null){
			return;
		}
		int nbr = attributes.getLength();
		for (int i = 0; i < nbr; i++) {
			Node attribute = attributes.item(i);
			String fieldName = attribute.getNodeName();
			Field field = fields.get(fieldName);
			if (field != null) {
				ReflectUtil.setPrimitive(object, field, attribute
						.getNodeValue().trim());
			}
		}

	}

	/**
	 * Check if the element contains just a text/cdata, in which case return
	 * that value. Else return null;
	 *
	 * @param element
	 * @return null if this is not a textElement as we see it. Value of single
	 *         text/CData child otherwise
	 */
	private static String getElementValue(Element element) {
		NamedNodeMap attribs = element.getAttributes();
		if (attribs != null && attribs.getLength() > 0) {
			return null;
		}

		NodeList children = element.getChildNodes();
		if(children == null){
			return null;
		}
		String value = null;
		int nbrChildren = children.getLength();
		for (int i = 0; i < nbrChildren; i++) {
			Node child = children.item(i);
			short childType = child.getNodeType();
			if (childType == Node.ELEMENT_NODE) {
				return null;
			}
			if (childType == Node.CDATA_SECTION_NODE
					|| childType == Node.TEXT_NODE) {
				if (value != null) {
					return null;
				}
				value = child.getNodeValue();
			}
		}
		return value;
	}

	/**
	 * write serialized object as an xml as per our object-xml mapping
	 * convention
	 *
	 * @param outStream
	 * @param object
	 * @return true if we succeeded in writing to the stream.
	 */
	public static boolean objectToXml(OutputStream outStream, Object object) {
		String eleName = object.getClass().getSimpleName();
		eleName = eleName.substring(0, 1).toLowerCase() + eleName.substring(1);
		try {
			Document doc = docFactory.newDocumentBuilder().newDocument();
			Element ele = doc.createElementNS("http://www.simplity.org/schema",
					eleName);
			objectToEle(object, doc, ele);
			doc.appendChild(ele);
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(outStream);
			Transformer trans = TransformerFactory.newInstance()
					.newTransformer();
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.transform(source, result);
			return true;
		} catch (Exception e) {
			Tracer.trace(e, eleName + " could not be saved as xml. ");
			return false;
		}
	}

	/**
	 * add all fields of an object to the elements as attribute/elements
	 *
	 * @param object
	 * @param doc
	 * @param defaultEle
	 *            if null, new element is created for this object. Else this
	 *            element is used
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @return element defaultEle, or new one
	 */
	private static Element objectToEle(Object object, Document doc,
			Element defaultEle) throws IllegalArgumentException,
			IllegalAccessException {
		Class<?> objectType = object.getClass();
		Tracer.trace("Gong to create an element for a "
				+ objectType.getSimpleName());
		/*
		 * create element if required
		 */
		Element ele = null;
		if (defaultEle != null) {
			ele = defaultEle;
		} else {
			String eleName = objectType.getSimpleName();
			eleName = eleName.substring(0, 1).toLowerCase()
					+ eleName.substring(1);
			ele = doc.createElement(eleName);
		}

		for (Field field : getAllFields(object.getClass()).values()) {
			field.setAccessible(true);
			Object value = field.get(object);

			if (value == null) {
				Tracer.trace("Field " + field.getName() + " has no value.");
				continue;
			}

			String fieldName = field.getName();
			Class<?> type = field.getType();
			if (ReflectUtil.isValueType(type)) {
				Tracer.trace("Field " + fieldName
						+ " has a primitive value of " + value);
				String stringValue = nonDefaultPrimitiveValue(value);
				if (stringValue != null) {
					ele.setAttribute(fieldName, stringValue);
				}
				continue;
			}

			if (type.isArray()) {
				Object[] objects = (Object[]) value;
				Tracer.trace("Field " + fieldName
						+ " is an array with a length = " + objects.length);
				if (objects.length == 0) {
					continue;
				}

				/*
				 * array of primitives is converted into comma separated string
				 */
				if (ReflectUtil.isValueType(type.getComponentType())) {
					StringBuilder sbf = new StringBuilder(
							primitiveValue(objects[0]));
					for (int i = 1; i < objects.length; i++) {
						sbf.append(',').append(primitiveValue(objects[i]));
					}
					ele.setAttribute(fieldName, sbf.toString());
					continue;
				}
				/*
				 * an element with this field name is added. Objects in the
				 * array are added as child elements of that element
				 */
				Element objectEle = doc.createElement(fieldName);
				ele.appendChild(objectEle);
				Tracer.trace("field " + fieldName
						+ " is added as an element and not as an attribute");
				for (Object obj : objects) {
					if (obj == null) {
						Tracer.trace("An element of array " + fieldName
								+ " is null. Ignored.");
					} else {
						objectEle.appendChild(objectToEle(obj, doc, null));
					}
				}
				continue;
			}
			/*
			 * an element with field name with the map contents as child
			 * elements
			 */
			if (value instanceof Map) {
				Map<?, ?> objects = (Map<?, ?>) value;
				Tracer.trace("Field " + fieldName + " is a MAP with size = "
						+ objects.size());
				if (objects.size() == 0) {
					continue;
				}
				Element objectEle = doc.createElement(fieldName);
				ele.appendChild(objectEle);
				for (Object obj : objects.values()) {
					if (obj == null) {
						Tracer.trace("An element of array " + fieldName
								+ " is null. Ignored.");
					} else {
						objectEle.appendChild(objectToEle(obj, doc, null));
					}
				}
				continue;
			}
			/*
			 * it is another object. we have an element with the field name,
			 * with one child element for this object
			 */
			Element objectEle = doc.createElement(fieldName);
			Tracer.trace("Field " + fieldName
					+ " is an object. An element is added for that.");
			ele.appendChild(objectToEle(value, doc, objectEle));
		}
		return ele;
	}

	/**
	 * @param value
	 * @return text value of the primitive
	 */
	private static String primitiveValue(Object value) {
		/*
		 * just that we expect 80% calls where value is String.
		 */
		if (value instanceof String) {
			return value.toString();
		}

		if (value instanceof Date) {
			return DateUtil.format((Date) value);
		}

		Class<?> type = value.getClass();
		if (type.isEnum()) {
			return TextUtil.constantToValue(value.toString());
		}
		if (type.equals(boolean.class)) {
			if (((Boolean) value).booleanValue()) {
				return TRUE_VALUE;
			}
			return FALSE_VALUE;
		}
		/*
		 * no floats please. attributes have to be double.
		 */
		if (type.equals(double.class)) {
			/*
			 * piggyback on decimal value for formatting?
			 */
			return Value.newDecimalValue(((Double) value).doubleValue())
					.toString();
		}
		return value.toString();
	}

	/**
	 * @param value
	 * @return text value of the primitive, if it is not the default (empty,
	 *         false and 0)
	 */
	private static String nonDefaultPrimitiveValue(Object value) {
		if (value == null) {
			return null;
		}
		/*
		 * just that we expect 80% calls where value is String.
		 */
		if (value instanceof String) {
			String s = value.toString();
			if (s.isEmpty()) {
				return null;
			}
			return s;
		}

		if (value instanceof Number) {
			long nbr = ((Number) value).longValue();
			if (nbr == 0) {
				return null;
			}
			return value.toString();
		}
		if (value instanceof Boolean) {
			if (((Boolean) value).booleanValue()) {
				return TRUE_VALUE;
			}
			return null;
		}

		if (value.getClass().isEnum()) {
			return TextUtil.constantToValue(value.toString());
		}
		return value.toString();
	}

	/**
	 *
	 * @param object
	 * @return xml text for the object's data-state
	 */
	public static String objectToXmlString(Object object) {
		OutputStream out = new ByteArrayOutputStream();
		objectToXml(out, object);
		try {
			out.close();
		} catch (IOException ignore) {
			//
		}
		return out.toString();
	}

	/**
	 * extract all attributes from the root node. We extract attributes, as well
	 * as simple elements as fields. This is not suitable for arbitrary
	 * object/data structure with multiple levels
	 *
	 * @param xml
	 * @param fields
	 * @return number of fields extracted
	 */
	public static int extractAll(String xml, FieldsInterface fields) {
		try {
			byte[] bytes = xml.getBytes(UTF8);
			InputStream is = new ByteArrayInputStream(bytes);
			/*
			 * get the doc first
			 */
			Node node = getDocument(is).getDocumentElement();

			/*
			 * data could be modeled as attributes...
			 */
			int nbrExtracted = 0;
			NamedNodeMap attrs = node.getAttributes();
			if(attrs != null){
				int n = attrs.getLength();
				for (int i = 0; i < n; i++) {
					Node att = attrs.item(i);
					fields.setValue(att.getNodeName(),
							Value.newTextValue(att.getNodeValue()));
				}
				nbrExtracted += n;
			}
			/*
			 * data could also be modeled as elements with just text data.
			 */
			NodeList childs = node.getChildNodes();
			if(childs != null){
				int n = childs.getLength();
				for (int i = 0; i < n; i++) {
					Node child = childs.item(i);
					if (child.getNodeType() == Node.ELEMENT_NODE) {
						String value = ((Element) child).getTextContent();
						fields.setValue(child.getNodeName(),
								Value.newTextValue(value));
					}
				}
				nbrExtracted += n;
			}
			Tracer.trace(nbrExtracted + " fields extracted from root node. " + node.getNodeName());
			return nbrExtracted;
		} catch (Exception e) {
			throw new ApplicationError(e,
					" Error while extracting fields from an xml.\n" + xml);
		}
	}
}
