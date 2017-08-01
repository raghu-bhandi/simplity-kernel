package org.simplity.ide;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.kernel.Application;
import org.simplity.kernel.comp.Component;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.util.XmlParseException;
import org.simplity.kernel.util.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * manage comps during IDE
 *
 * @author simplity.org
 *
 */
public class CompsManager {
	private static final Logger logger = LoggerFactory.getLogger(CompsManager.class);
	private static final String EXTN = ".xml";
	private static final String FOLDER = "/";
	/*
	 * single instance
	 */
	private static final CompsManager instance = new CompsManager();

	/**
	 * load all resources under the comp root folder. This MUST be called before
	 * using other methods
	 *
	 * @param compRoot
	 */
	public static void loadResources(String compRoot) {
		instance.loadAll(compRoot);
	}

	/**
	 *
	 * @return the manager to manage comps for you
	 */
	public static CompsManager getManager() {
		return instance;
	}

	/**
	 *
	 * @param fileName
	 * @return true if this file is an .xml inside a folder meant for a simplity
	 *         resource. false otherwise
	 */
	public static boolean isSimplityResource(String fileName) {
		return instance.isOurFile(fileName);
	}

	/**
	 * comp is created if it is not existing
	 *
	 * @param compType
	 * @param compId
	 * @return non-null always, as it is created as dummy if it is not already
	 *         parsed
	 */
	static Comp getRefComp(ComponentType compType, String compId) {
		if (compType == null) {
			logger.error("No reference to Application allowed");
			return null;
		}

		Comp comp = instance.comps[compType.getIdx()].get(compId);
		if (comp != null) {
			return comp;
		}
		/*
		 * create a dummy one and keep it in undefined collection, if it not
		 * already there
		 */
		String id = compType + compId;
		comp = instance.undefinedComps.get(id);
		if (comp == null) {
			comp = new Comp(compType, compId);
			instance.undefinedComps.put(id, comp);
		}
		return comp;
	}

	/**
	 * validate a file and return error messages.
	 *
	 * @param fileName
	 * @return array of error messages. null if there are no errors. We will
	 *         re-factor this later to get markers..
	 */
	public static String[] validate(String fileName) {
		CompilationUnit cpu = instance.validateFile(fileName);
		List<String> errs = cpu.errors;
		if (errs == null || errs.isEmpty()) {
			return null;
		}
		return errs.toArray(new String[0]);
	}

	/**
	 *
	 * @param compType
	 *            null for application
	 * @param compId
	 * @return comp, or null if no such comp of type with the key
	 */
	public static Comp getComp(ComponentType compType, String compId) {
		if (compType == null) {
			return instance.apps.get(compId);
		}
		return instance.comps[compType.getIdx()].get(compId);
	}

	/**
	 * folder name of root of components where we find applicaiton.xml
	 */
	String compRootFolder;
	/**
	 * typically project should have only one applicaiton.xml. But it is
	 * possible that a dev environment may keep more than one.
	 */
	private Map<String, Comp> apps;

	/**
	 * we keep all comps in an array, using the index provided by componentType
	 */
	private Map<String, Comp>[] comps;

	/**
	 * keep track of all compilation units
	 */
	private Map<String, CompilationUnit> compilationUnits = new HashMap<String, CompilationUnit>();

	/**
	 * keep track of referred, but undefined comps. We expect less load, and
	 * hence we are using type=id as key
	 */
	private Map<String, Comp> undefinedComps;

	private CompsManager() {
		// private
	}

	/**
	 * this is to be called to load all existing resources, before doing any
	 * incremental operations
	 *
	 * @param compFolderName
	 *            that ends with
	 */
	@SuppressWarnings("unchecked")
	public void loadAll(String compFolderName) {
		String rootFolder = compFolderName;
		if (rootFolder.endsWith(FOLDER) == false) {
			rootFolder += FOLDER;
		}

		File folder = new File(rootFolder);
		if (folder.exists() == false) {
			logger.error("Root folder " + rootFolder + " is not valid. Comps are not loaded");
			return;
		}

		if (folder.isDirectory() == false) {
			logger.error("Root folder " + rootFolder + " is not a folder. Comps are not loaded");
			return;
		}

		this.compRootFolder = rootFolder;
		/*
		 * we have to create maps up front to create dummy entries for
		 * references for comp before the comp is parsed
		 */
		int nbr = ComponentType.values().length;
		/*
		 * type safety requires us to do this unchecked assignment :-) :-)
		 */
		Object[] objs = new Object[nbr];
		this.comps = (Map<String, Comp>[]) objs;
		for (int i = 0; i < this.comps.length; i++) {
			this.comps[i] = new HashMap<String, Comp>();
		}

		this.apps = new HashMap<String, Comp>();
		this.compilationUnits = new HashMap<String, CompilationUnit>();
		this.undefinedComps = new HashMap<String, Comp>();

		ValidationContext vtx = new ValidationContext();
		this.loadApp(rootFolder, vtx);

		for (ComponentType ct : ComponentType.values()) {
			if (ct.isGrouped()) {
				this.loadGroups(ct, compFolderName, vtx);
			} else {
				this.loadSingles(ct, compFolderName, vtx);
			}
		}
	}

	/**
	 * load resources from files that define more than one component as a
	 * collection/group
	 *
	 * @param ct
	 * @param rootFolder
	 * @param vtx
	 */
	private void loadGroups(ComponentType ct, String rootFolder, ValidationContext vtx) {
		Class<?> cls = ct.getClass();
		String packageName = cls.getPackage().getName();
		String prefix = ct.getFolderPrefix();
		Map<String, Comp> map = this.comps[ct.getIdx()];
		/**
		 * holds comps within a compiltionUnit
		 */
		Map<String, Component> temp = new HashMap<String, Component>();

		String compFolder = rootFolder + prefix;
		/**
		 * for each resource file in the comp-root folder
		 */
		for (String fn : FileManager.getResources(compFolder)) {
			String id = prefix + fn;
			CompilationUnit cpu = new CompilationUnit(id, ct, true);
			this.compilationUnits.put(id, cpu);
			temp.clear();

			if (XmlUtil.xmlToCollection(compFolder + fn, temp, packageName) == false) {
				logger.info("File " + fn
						+ " is not parsed because of errors in it or it is meant for different type of resource.");
				continue;
			}

			for (Map.Entry<String, Component> entry : temp.entrySet()) {
				String key = entry.getKey();
				map.put(key, this.createComp(ct, key, entry.getValue(), cpu, vtx));
			}

			logger.info("File " + fn + " parsed for resource type " + ct);
		}
	}

	/**
	 * @param ct
	 * @param key
	 * @param value
	 * @param cpu
	 * @param vtx
	 * @return
	 */
	private Comp createComp(ComponentType ct, String key, Object value, CompilationUnit cpu, ValidationContext vtx) {
		/*
		 * if this was referred earlier, it would have been created as dummy
		 */
		Comp comp = this.undefinedComps.remove(ct + key);
		Set<Comp> dependents = null;
		if (comp != null) {
			dependents = comp.getDependentComps();
		}

		comp = new Comp(ct, key, value, cpu, dependents, vtx);
		cpu.addComp(comp);
		return comp;
	}

	/**
	 * load components from files that have one component per file
	 *
	 * @param ct
	 * @param rootFolder
	 * @param vtx
	 */
	private void loadSingles(ComponentType ct, String rootFolder, ValidationContext vtx) {
		Class<?> cls = ct.getClass();
		String prefix = ct.getFolderPrefix();
		String elementName = TextUtil.classNameToName(cls.getSimpleName());
		Map<String, Comp> map = this.comps[ct.getIdx()];

		String compFolder = rootFolder + prefix;
		for (String fn : FileManager.getResources(compFolder)) {
			String id = prefix + fn;
			CompilationUnit cpu = new CompilationUnit(id, ct, false);
			this.compilationUnits.put(id, cpu);
			try {
				Component obj = (Component) cls.newInstance();
				if (XmlUtil.xmlToObject(compFolder + fn, obj, elementName) == false) {
					logger.info(
							"File " + fn + " in the root comp folder is not parsed as it is not an application file.");
					continue;
				}
				String fullName = obj.getQualifiedName();
				if (fn.equals(fullName + EXTN) == false) {
					logger.error("file " + fn + " contins a resource with its qualified name as " + fullName
							+ ". this violates naming rule that is required to manage the rresources at run time. Resource not parsed.");
					continue;
				}
				Comp comp = this.createComp(ct, fullName, obj, cpu, vtx);
				map.put(fullName, comp);
				logger.info("File " + fn + " parsed as resource " + ct);
			} catch (XmlParseException e) {
				logger.error("file " + fn + " is not a valid xml. Not parsed");
			} catch (InstantiationException e) {
				logger.error("Unable to create an instance from class " + cls.getName() + " " + e.getMessage());
			} catch (IllegalAccessException e) {
				logger.error("Unable to create an instance from class " + cls.getName() + " " + e.getMessage());
			}
		}
	}

	/**
	 * load applications in the root folder
	 *
	 * @param rootFolder
	 * @param vtx
	 */
	private void loadApp(String rootFolder, ValidationContext vtx) {
		for (String fn : new File(rootFolder).list()) {
			if (fn.endsWith(EXTN) == false) {
				continue;
			}
			String id = fn.substring(0, fn.length() - EXTN.length());
			CompilationUnit cpu = new CompilationUnit(id, null, false);
			this.compilationUnits.put(id, cpu);
			Application app = new Application();
			try {
				if (XmlUtil.xmlToObject(rootFolder + fn, app, "application") == false) {
					logger.info(
							"File " + fn + " in the root comp folder is not parsed as it is not an application file.");
					continue;
				}
			} catch (XmlParseException e) {
				logger.info("File " + fn + " has xml syntax errors.");
				continue;
			}
			Comp comp = this.createComp(null, id, app, cpu, vtx);
			this.apps.put(id, comp);
		}
	}

	/**
	 * @param fileName
	 */
	private CompilationUnit validateFile(String fileName) {
		if (this.isOurFile(fileName) == false) {
			return null;
		}

		String relName = fileName.substring(this.compRootFolder.length());
		String prefix;
		ComponentType ct;
		if (relName.isEmpty()) {
			/*
			 * files in root have to be application
			 */
			ct = null;
			prefix = "";

		} else {
			ct = ComponentType.getTypeByFolder(relName);
			if (ct == null) {
				logger.info("file " + fileName
						+ " is inside comp folder, but sub-folder is not for any simplity resource. File not validated");
				return null;
			}
			prefix = ct.getFolderPrefix();
		}
		/*
		 * file name is rootName + prefix + resource-name. We have t get
		 * resource name from this as fileIs. for example fileName =
		 * "comp/service/tp/mod/s.xml" then fileId would be "mod/s.xml"
		 */
		String folderPrefix = this.compRootFolder + prefix;
		String fileId = fileName.substring(folderPrefix.length());

		CompilationUnit cpu = new CompilationUnit(fileId, ct, true);
		CompilationUnit oldCpu = this.compilationUnits.put(fileId, cpu);
		Map<String, Comp> map = ct == null ? this.apps : this.comps[ct.getIdx()];
		this.removeComps(map, oldCpu);

		if (ct == null) {
			this.validateApp(fileName, cpu);
		} else if (ct.isGrouped()) {
			this.validateGroup(ct, fileName, cpu);
		} else {
			this.validateSingle(ct, fileName, cpu);
		}
		return cpu;
	}

	/**
	 * @param ct
	 * @param fileName
	 */
	private void validateGroup(ComponentType ct, String fileName, CompilationUnit cpu) {
		Map<String, Component> objects = new HashMap<String, Component>();
		if (XmlUtil.xmlToCollection(fileName, objects, ct.getClass().getPackage().getName()) == false) {
			cpu.addError("Not a valid xml, or this is not for resource type " + ct);
			return;
		}

		ValidationContext vtx = new ValidationContext();
		Map<String, Comp> map = this.comps[ct.getIdx()];
		for (Map.Entry<String, Component> entry : objects.entrySet()) {
			String key = entry.getKey();
			Comp comp = this.createComp(ct, key, entry.getValue(), cpu, vtx);
			cpu.addComp(comp);
			map.put(key, comp);
		}
	}

	/**
	 * @param ct
	 * @param fileName
	 */
	private void validateSingle(ComponentType ct, String fileName, CompilationUnit cpu) {
		Class<?> cls = ct.getClass();
		try {
			Object object = cls.newInstance();
			String eleName = TextUtil.classNameToName(cls.getSimpleName());

			if (XmlUtil.xmlToObject(fileName, object, eleName)) {
				String compName = ((Component) object).getQualifiedName();
				if (fileName.endsWith(compName + EXTN) == false) {
					cpu.addError("resource full name should match folder/file name.");
					return;
				}
				Comp comp = this.createComp(ct, compName, object, cpu, new ValidationContext());
				cpu.addComp(comp);
				this.comps[ct.getIdx()].put(compName, comp);
				return;
			}
			cpu.addError("xml root element should be for component type " + ct);
		} catch (XmlParseException e) {
			cpu.addError("XML Syntax errors");
		} catch (InstantiationException e) {
			cpu.addError("Unable to create an instance from class " + cls.getName() + " " + e.getMessage());
		} catch (IllegalAccessException e) {
			cpu.addError("Unable to create an instance from class " + cls.getName() + " " + e.getMessage());
		}
	}

	/**
	 * @param ct
	 * @param fileName
	 */
	private void validateApp(String fileName, CompilationUnit cpu) {
		try {
			Application app = new Application();
			// chop-off .xml
			String appId = fileName.substring(0, fileName.length() - EXTN.length());
			/// remove folder names
			appId = appId.substring(appId.lastIndexOf('/') + 1);
			if (XmlUtil.xmlToObject(fileName, app, "application")) {
				Comp comp = this.createComp(null, appId, app, cpu, new ValidationContext());
				cpu.addComp(comp);
				this.apps.put(appId, comp);
				return;
			}
			cpu.addError("error while loading atttributes from xml to application.");
			return;

		} catch (XmlParseException e) {
			cpu.addError("XML Syntax errors");
		}
	}

	/**
	 * remove comps, and their references from our repository
	 *
	 * @param map
	 * @param cpu
	 */
	private void removeComps(Map<String, Comp> map, CompilationUnit cpu) {
		for (Comp comp : cpu.comps) {
			if (map.remove(comp.compId) == null) {
				this.undefinedComps.remove(comp);
				continue;
			}
			Set<Comp> uses = comp.compsUsed;
			if (uses == null) {
				continue;
			}
			for (Comp ref : uses) {
				Set<Comp> usedBy = ref.usedByComps;
				if (usedBy != null) {
					usedBy.remove(ref);
				}
			}
		}
	}

	/**
	 *
	 * @param fileName
	 * @return true if this file is an xml inside one of the designated folders
	 *         for simplity component. false otherwise
	 */
	private boolean isOurFile(String fileName) {
		if (fileName == null || fileName.endsWith(EXTN) == false || this.resLoaded() == false) {
			return false;
		}
		/*
		 * is it inside our area?
		 */
		if (fileName.startsWith(this.compRootFolder) == false) {
			return false;
		}
		String compName = fileName.substring(this.compRootFolder.length());
		int idx = compName.lastIndexOf('/');
		if (idx == -1) {
			/*
			 * in comp root folder.
			 */
			return true;
		}

		ComponentType ct = ComponentType.getTypeByFolder(compName);
		return ct != null;
	}

	/**
	 *
	 * @return true if loadAll() was called, false otherwise
	 */
	private boolean resLoaded() {
		if (this.compRootFolder == null) {
			logger.error("Resources are not loaded but there are calls regarding resources");
			return false;
		}
		return true;
	}

}
