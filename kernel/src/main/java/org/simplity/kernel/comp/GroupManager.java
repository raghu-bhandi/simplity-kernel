package org.simplity.kernel.comp;

import java.util.HashMap;
import java.util.Map;

import org.simplity.kernel.util.XmlParseException;

/**
 * some components are organized into a collection that is saved in a file.
 * (like message). This interface represents such a group that can add its
 * individual components into collection.
 *
 * @author simplity.org
 * @param <T>
 *            component being managed
 *
 */
public class GroupManager<T extends Component> implements Loadable {
	private static final String ENTRY = "entry";
	/*
	 * fields that are to be loaded are package-private
	 */
	private final String packageName;
	/**
	 *
	 */
	String name;
	/**
	 * collection to which components are loaded
	 */
	Map<String, T> components = new HashMap<String, T>();

	/**
	 * instantiate a group manager for a Component type
	 *
	 * @param componentClass
	 *            Base class, whose package is to be used to instantiate objects
	 *            for loading xml elements
	 */
	public GroupManager(Class<?> componentClass) {
		this.packageName = componentClass.getPackage().getName() + '.';
	}

	/**
	 * add components from this group into the collection.
	 *
	 * @param collection
	 */
	public void moveComponents(Map<String, T> collection) {
		collection.putAll(this.components);
		this.components.clear();
	}

	@Override
	public Object getObjectToLoad(String tagName) throws XmlParseException {
		if (tagName.equals(ENTRY)) {
			return new Entry();
		}
		String className = this.packageName
				+ tagName.substring(0, 1).toUpperCase() + tagName.substring(1);
		try {
			return Class.forName(className).newInstance();
		} catch (Exception e) {
			throw new XmlParseException(
					"Unable to create an instance for class " + className);
		}
	}
}
