/*
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.comp;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Message;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.db.Sql;
import org.simplity.kernel.db.StoredProcedure;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.XmlParseException;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.service.ServiceInterface;
import org.simplity.tp.OnTheFlyServiceManager;
import org.simplity.tp.Service;

/**
 * class that knows where resources persist and how to get a stream to read them
 * We are still debating whether we are better-off writing individual classes
 * rather than this parameterized utility class
 * 
 * @author simplity.org
 * @param <T>
 *            this is internally used by this class. Users never deal with an
 *            instance of this.
 *
 */
public class ComponentManager<T extends Component> {
	private static final char DELIMITER = '.';

	private static final ComponentManager<Sql> SQL_MANAGER = new ComponentManager<Sql>(
			Sql.class, "sql", null, false, null);

	private static final ComponentManager<StoredProcedure> SP_MANAGER = new ComponentManager<StoredProcedure>(
			StoredProcedure.class, "sp", null, false, null);

	private static final ComponentManager<ServiceInterface> TP_MANAGER = new ComponentManager<ServiceInterface>(
			Service.class, "service/tp", null, false,
			new OnTheFlyServiceManager());

	private static final ComponentManager<ServiceInterface> SERVICE_MANAGER = new ComponentManager<ServiceInterface>(
			null, "service/list", new GroupManager<ServiceInterface>(
					Service.class), true, null);

	private static final ComponentManager<Message> MSG_MANAGER = new ComponentManager<Message>(
			null, "msg", new GroupManager<Message>(Message.class), false, null);

	private static final ComponentManager<DataType> DT_MANAGER = new ComponentManager<DataType>(
			null, "dt", new GroupManager<DataType>(DataType.class), false, null);

	private static final ComponentManager<Record> RECORD_MANAGER = new ComponentManager<Record>(
			Record.class, "rec", null, false, null);

	private static final ComponentManager<?>[] ALL_MANAGERS = { MSG_MANAGER,
		DT_MANAGER, RECORD_MANAGER, TP_MANAGER, SQL_MANAGER, SP_MANAGER,
		SERVICE_MANAGER };

	private static final char FOLDER_CHAR = '/';
	private static final String FOLDER_STR = "/";
	private static final String EXTN = ".xml";

	/**
	 * root folder where components are located, relative to applicaiton root.
	 */
	private static String componentFolder;

	/**
	 *
	 * @param sqlName
	 * @return sql failing which an application error is thrown
	 */
	public static Sql getSql(String sqlName) {
		Sql sql = SQL_MANAGER.get(sqlName);
		if (sql == null) {
			throw new ApplicationError("SQL  " + sqlName + " is not defined.");
		}
		return sql;
	}

	/**
	 *
	 * @param sqlName
	 * @return sql or null
	 */
	public static Sql getSqlOrNull(String sqlName) {
		return SQL_MANAGER.get(sqlName);
	}

	/**
	 *
	 * @param serviceName
	 * @return service failing which an application error is thrown
	 */
	public static ServiceInterface getService(String serviceName) {

		/*
		 * services are of two types. custom services that are java classes, and
		 * transaction processing services that are xmls
		 */
		ServiceInterface service = getServiceOrNull(serviceName);
		if (service == null) {
			throw new ApplicationError("Service " + serviceName
					+ " is not defined.");
		}
		return service;
	}

	/**
	 *
	 * @param serviceName
	 * @return service or null
	 */
	public static ServiceInterface getServiceOrNull(String serviceName) {

		/*
		 * services are of two types. custom services that are java classes, and
		 * transaction processing services that are xmls
		 */
		ServiceInterface service = SERVICE_MANAGER.get(serviceName);
		if (service != null) {
			return service;
		}
		return TP_MANAGER.get(serviceName);
	}

	/**
	 *
	 * @param recordName
	 * @return record failing which an application error is thrown
	 */
	public static Record getRecord(String recordName) {
		Record record = RECORD_MANAGER.get(recordName);
		if (record == null) {
			throw new ApplicationError("Record " + recordName
					+ " is not defined.");
		}
		return record;
	}

	/**
	 *
	 * @param recordName
	 * @return record or null
	 */
	public static Record getRecordOrNull(String recordName) {
		return RECORD_MANAGER.get(recordName);
	}

	/**
	 *
	 * @param messageName
	 * @return Message failing which an application error is thrown
	 */
	public static Message getMessage(String messageName) {
		Message msg = MSG_MANAGER.get(messageName);
		if (msg == null) {
			throw new ApplicationError("Message " + messageName
					+ " is not defined.");
		}
		return msg;
	}

	/**
	 *
	 * @param messageName
	 * @return Message or null if there is no message with this name
	 */
	public static Message getMessageOrNull(String messageName) {
		return MSG_MANAGER.get(messageName);
	}

	/**
	 *
	 * @param dataTypeName
	 * @return data type, failing which an application error is thrown
	 */
	public static DataType getDataType(String dataTypeName) {
		DataType dt = DT_MANAGER.get(dataTypeName);
		if (dt == null) {
			throw new ApplicationError("Data Type " + dataTypeName
					+ " is not defined.");
		}
		return dt;
	}

	/**
	 *
	 * @param dataTypeName
	 * @return data type, or null if no data type is found with that name
	 */
	public static DataType getDataTypeOrNull(String dataTypeName) {
		return DT_MANAGER.get(dataTypeName);
	}

	/**
	 *
	 * @param spName
	 * @return stored procedure failing which an application error is thrown
	 */
	public static StoredProcedure getStoredProcedure(String spName) {
		StoredProcedure sp = SP_MANAGER.get(spName);
		if (sp == null) {
			throw new ApplicationError("Stored Procedure" + spName
					+ " is not defined.");
		}
		return sp;
	}

	/**
	 *
	 * @param spName
	 *            stored procedure name
	 * @return stored procedure, or null if
	 */
	public static StoredProcedure getStoredProcedureOrNull(String spName) {
		return SP_MANAGER.get(spName);
	}

	/**
	 * to be called by the boot-strap after setting root folder for resource
	 * manager
	 */
	public static void initialLoad() {
		for (ComponentManager<?> mgr : ALL_MANAGERS) {
			mgr.loadAll();
		}
	}

	/**
	 * management console, during development/test can alter caching
	 */
	public static void startCaching() {
		for (ComponentManager<?> mgr : ALL_MANAGERS) {
			mgr.doStartCaching();
		}
	}

	/**
	 * management console, during development/test can alter caching
	 */
	public static void stopCaching() {
		for (ComponentManager<?> mgr : ALL_MANAGERS) {
			mgr.doStopCaching();
		}
	}

	/**
	 * management console, during development/test can alter caching
	 */
	public static void purge() {
		for (ComponentManager<?> mgr : ALL_MANAGERS) {
			mgr.doPurge();
		}
	}

	/*
	 * internals of this class instance.
	 */

	/**
	 * class for which this manager is instantiated. null implies that the
	 * component is not loaded on demand
	 */
	private final Class<? extends T> componentClass;

	private final GroupManager<T> groupManager;
	/**
	 * folder name under which these components are saved
	 */
	private final String folderName;
	/**
	 * Cached components
	 */
	private Map<String, T> components = null;

	private Map<String, T> lazyOnes = null;

	private final OnTheFlyManager<T> onTheFlyManager;

	private ComponentManager(Class<? extends T> compClass, String folder,
			GroupManager<T> groupManager, boolean lazy,
			OnTheFlyManager<T> flyManager) {
		this.componentClass = compClass;
		this.folderName = folder;
		this.groupManager = groupManager;
		if (lazy) {
			this.lazyOnes = new HashMap<String, T>();
			this.components = new HashMap<String, T>();
		}
		this.onTheFlyManager = flyManager;
	}

	private void loadAll() {
		if (this.groupManager != null) {
			this.components = new HashMap<String, T>();
			loadAllComponents(this.folderName, this.components,
					this.groupManager);
		}
	}

	/**
	 * typically called from start-up routine. Also useful in test environment
	 */
	private void doStartCaching() {
		if (this.groupManager == null) {
			this.components = new HashMap<String, T>();
		}
	}

	/**
	 * no need to use this, except in test environment
	 */
	private void doStopCaching() {
		if (this.components != null) {
			this.components.clear();
			this.components = null;
		}
	}

	/**
	 * remove all cached records
	 */
	private void doPurge() {
		if (this.components != null) {
			this.components.clear();
			this.loadAll();
		}
	}

	/**
	 * Get a component. Cache if required. throws ApplicationError in case the
	 * component is not defined.
	 *
	 * @param compName
	 *            fully qualified name of a component.
	 * @return component
	 */
	@SuppressWarnings("unchecked")
	private T get(String compName) {
		T component = null;
		/*
		 * lazy ones are misleading. They WERE lazy, but are now unlazied ;-)
		 */
		if (this.lazyOnes != null) {
			component = this.lazyOnes.get(compName);
			if (component != null) {
				return component;
			}
			/*
			 * see if this is still lazying in components list
			 */
			Object entry = this.components.remove(compName);
			if (entry != null) {
				/*
				 * so much for all our effort for type-safety. I am ditching it
				 * for the time-being. will re-visit to make this type-safe
				 */
				component = (T) ((Entry) entry).getObject();
				component.getReady();
				this.lazyOnes.put(compName, component);
				return component;
			}
		} else if (this.components != null) {
			component = this.components.get(compName);
			if (component != null) {
				return component;
			}
		}

		/*
		 * component not found in our cache. Let us load it
		 */
		component = this.load(compName);
		if (component != null) {
			component.getReady();
			/*
			 * is the component name in synch with its location?
			 */
			if (compName.equals(component.getQualifiedName()) == false) {
				throw new ApplicationError(
						component.getQualifiedName()
						+ " is saved as if it is "
						+ compName
						+ ". This needs to be corrected before the component can be used.");
			}
			if (this.components != null) {
				this.components.put(compName, component);
			}
		}
		return component;
	}

	private Collection<T> getAll() {
		return this.components.values();
	}

	/**
	 * load a component
	 *
	 * @param compName
	 * @return component instance or null
	 */
	private T load(String compName) {
		if (this.componentClass == null) {
			return null;
		}
		T component;
		if (this.onTheFlyManager != null) {
			component = this.onTheFlyManager.create(compName);
			if (component != null) {
				return component;
			}
		}
		try {
			component = this.componentClass.newInstance();
			if (loadComponent(this.folderName, compName, component)) {
				return component;
			}
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while loading "
					+ this.componentClass.getSimpleName());
		}
		return null;
	}

	/**
	 * set folder name where components are saved, relative to application root.
	 * This MUST be called after setApplicationRootFolder
	 *
	 * @param compRelativeToRoot
	 */
	public static void setComponentFolder(String compRelativeToRoot) {
		componentFolder = compRelativeToRoot;
		if (componentFolder.endsWith(FOLDER_STR) == false) {
			componentFolder += FOLDER_CHAR;
		}
	}

	/**
	 *
	 * @param compType
	 * @param compName
	 * @param component
	 *            instance to be loaded
	 * @return true if the component is loaded. False if no such component is
	 *         defined. ApplicationError in case of error in loading the
	 *         component
	 */
	private static boolean loadComponent(String compType, String compName,
			Component component) {
		String fileName = compName.replace(DELIMITER, FOLDER_CHAR) + EXTN;
		String nameToUse = componentFolder + compType + "/" + fileName;
		InputStream stream = null;
		try {
			stream = FileManager.getResourceStream(nameToUse);
		} catch (Exception e) {
			//
		}
		if (stream == null) {
			return false;
		}
		Exception exp = null;
		try {
			XmlUtil.xmlToObject(stream, component);
		} catch (XmlParseException e) {
			exp = e;
		}
		try {
			stream.close();
		} catch (Exception e) {
			//
		}
		if (exp != null) {
			throw new ApplicationError(exp, "error while loading component "
					+ component.getClass().getSimpleName() + " "
					+ component.getQualifiedName());
		}
		if (compName.equals(component.getQualifiedName()) == false) {
			throw new ApplicationError("Component has a qualified name of "
					+ component.getQualifiedName()
					+ " that is different from its storage name " + compName);
		}
		return true;
	}

	/**
	 * load all components inside folder. This is used by components that save
	 * several of them per file using a group manager
	 *
	 * @param compFolder
	 * @param components
	 * @param groupManager
	 */
	private static <T extends Component> void loadAllComponents(
			String compFolder, Map<String, T> components,
			GroupManager<T> groupManager) {
		String resFolder = componentFolder + compFolder;
		for (String resName : FileManager.getResources(resFolder)) {
			if (resName.endsWith(EXTN) == false) {
				Tracer.trace("Skipping Non-resource " + resName);
				continue;
			}
			Tracer.trace("Going to read " + resName);
			InputStream stream = null;
			try {
				stream = FileManager.getResourceStream(resName);
				XmlUtil.xmlToObject(stream, groupManager);
				groupManager.moveComponents(components);
				Tracer.trace(components.size() + " components loaded.");
			} catch (Exception e) {
				Tracer.trace(e, "Resource " + resName + " failed to load.");
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
	}

	/**
	 * @param object
	 * @param fileName
	 *            file name, including extension to be used for loading the
	 *            object
	 * @throws Exception
	 *             any exception while opening and loading the file
	 */
	public static void loadObject(Object object, String fileName)
			throws Exception {
		String nameToUse = componentFolder + fileName;
		InputStream stream = null;
		try {
			Tracer.trace("Going to load an object from " + nameToUse);
			stream = FileManager.getResourceStream(nameToUse);
			XmlUtil.xmlToObject(stream, object);
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
	 * get all data types
	 *
	 * @return all data types defined for this project
	 */
	public static Collection<Message> getAllMessages() {
		return MSG_MANAGER.getAll();
	}

	/**
	 * get all data types
	 *
	 * @return all data types defined for this project
	 */
	public static Collection<DataType> getAllDataTypes() {
		return DT_MANAGER.getAll();
	}

	/**
	 * @return folder relative to context root
	 */
	public static String getComponentFolder() {
		return componentFolder;
	}
}
