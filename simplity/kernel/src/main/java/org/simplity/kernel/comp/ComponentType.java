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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.kernel.comp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.simplity.job.Jobs;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Message;
import org.simplity.kernel.db.Sql;
import org.simplity.kernel.db.StoredProcedure;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.fn.Concat;
import org.simplity.kernel.fn.Function;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.service.ServiceInterface;
import org.simplity.test.TestRun;
import org.simplity.tp.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * components are the basic building blocks of application. This is an enumeration of them. Types
 * come with utility methods to load the components
 *
 * @author simplity.org
 */
public enum ComponentType {
  /** Data Type */
  DT(0, DataType.class, "dt/", true),
  /** Message */
  MSG(1, Message.class, "msg/", true),
  /** Record */
  REC(2, Record.class, "rec/", false),
  /** service */
  SERVICE(3, Service.class, "service/tp/", false) {

    /*
     * (non-Javadoc)
     *
     * @see
     * org.simplity.kernel.comp.ComponentType#generateComp(java.lang.String)
     */
    @Override
    protected Component generateComp(String compName) {
      /*
       * is this service an alias?
       */
      Object entry = serviceAliases.get(compName);
      if (entry != null) {
        return this.getComponentOrNull(entry.toString());
      }
      /*
       * try on-the-fly service generation
       */
      return Service.generateService(compName);
    }
  },
  /** Sql */
  SQL(4, Sql.class, "sql/", false),
  /** Stored procedure */
  SP(5, StoredProcedure.class, "sp/", false),
  /** function */
  FUNCTION(6, Function.class, "fn/", true) {
    @Override
    protected void loadAll() {
      try {
        loadGroups(this.folder, null, this.cachedOnes);
        /*
         * we have to initialize the components
         */
        for (Map.Entry<String, Object> entry : this.cachedOnes.entrySet()) {
          String fname = entry.getValue().toString();
          Object obj = null;
          try {
            obj = Class.forName(fname).newInstance();
          } catch (Exception e) {

            logger.error("Unable to create an instance of Function based on class " + fname, e);
          }
          if (obj != null) {
            if (obj instanceof Function) {
              entry.setValue(obj);
            } else {

              logger.info(
                  fname
                      + " is a valid class but not a sub-class of Function. Function entry ignored.");
            }
          }
        }

        logger.info(this.cachedOnes.size() + " " + this + " loaded.");

      } catch (Exception e) {
        this.cachedOnes.clear();

        logger.error(
            this
                + " pre-loading failed. No component of this type is available till we successfully pre-load them again.",
            e);
      }
      for (Function fn : BUILT_IN_FUNCTIONS) {
        String fname = fn.getSimpleName();
        if (this.cachedOnes.get(fname) != null) {

          logger.info(
              fname
                  + " is a built-in function and can not be over-ridden. User defined function with the same name is discarded.");
        }
        this.cachedOnes.put(fname, fn);
      }
    }
  },

  /** test cases for service */
  TEST_RUN(7, TestRun.class, "test/", false),

  /** test cases for service */
  JOBS(8, Jobs.class, "batch/", false);
	protected static final Logger logger = LoggerFactory.getLogger(ComponentType.class);

  /*
   * constants
   */
  private static final char FOLDER_CHAR = '/';
  private static final String FOLDER_STR = "/";
  private static final char DELIMITER = '.';
  private static final String EXTN = ".xml";
  private static final String CLASS_FOLDER = "service/list/";
  /*
   * list of built-in functions
   */
  protected static final Function[] BUILT_IN_FUNCTIONS = {new Concat()};

  /** root folder where components are located, relative to file-manager's root. */
  private static String componentFolder = "/comp/";
  /**
   * service has a way to generate rather than load.. One way is to have a class associated with
   * that
   */
  protected static final Map<String, Object> serviceAliases = new HashMap<String, Object>();
  /*
   * attributes of component type
   */
  /** allows us to use array instead of map while dealing with componentType based collections */
  private final int idx;

  /** class associated with this type that is used for loading component/s */
  protected final Class<?> cls;

  /** folder name under which components are saved */
  protected final String folder;

  /** is this loaded on a need basis or pre-loaded? */
  private final boolean preLoaded;

  protected Map<String, Object> cachedOnes;

  /**
   * @param idx
   * @param cls
   * @param folder
   */
  ComponentType(int idx, Class<? extends Component> cls, String folder, boolean preLoaded) {
    this.idx = idx;
    this.cls = cls;
    this.folder = folder;
    this.preLoaded = preLoaded;
    if (this.preLoaded) {
      this.cachedOnes = new HashMap<String, Object>();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Enum#toString()
   */
  @Override
  public String toString() {
    return this.cls.getSimpleName();
  }

  /** @return idx associated with this comp type */
  public int getIdx() {
    return this.idx;
  }

  /** @return true if it is pre-loaded. false if it is loaded on a need basis */
  public boolean isPreloded() {
    return this.preLoaded;
  }

  /**
   * @param compName qualified component name
   * @return instance of the desired component. Throws ApplicationError if this component is not
   *     found. use getComponentOrNull() if you do not want an error
   */
  public Component getComponent(String compName) {
    Component comp = this.getComponentOrNull(compName);
    /*
     * look no further if this is always cached
     */
    if (comp == null) {
      throw new MissingComponentError(this, compName);
    }
    return comp;
  }

  /**
   * @param compName qualified component name
   * @return instance of the desired component. Throws ApplicationError if this component is not
   *     found. use getComponentOrNull() if you do not want an error
   */
  public Component getComponentOrNull(String compName) {
    /*
     * do we have it in our cache?
     */
    Object object = null;
    if (this.cachedOnes != null) {
      object = this.cachedOnes.get(compName);
      if (object != null) {
        return (Component) object;
      }
    }

    /*
     * look no further if this is always cached
     */
    if (this.preLoaded) {
      return null;
    }

    object = this.generateComp(compName);
    if (object == null) {
      object = this.load(compName);
    }

    if (object == null) {
      return null;
    }

    Component comp = (Component) object;
    comp.getReady();
    if (this.cachedOnes != null) {
      this.cachedOnes.put(compName, comp);
    }
    return comp;
  }

  /**
   * get all pre-loaded Components
   *
   * @return map of all pre-loaded components
   * @throws ApplicationError in case this type is not pre-loaded
   */
  public Collection<Object> getAll() {
    if (this.preLoaded) {
      return this.cachedOnes.values();
    }
    throw new ApplicationError(
        this + " is not pre-loaded and hence we can not respond to getAll()");
  }

  /**
   * replace the component in the cache.
   *
   * @param comp
   */
  public void replaceComponent(Component comp) {
    if (this.cachedOnes == null || comp == null) {
      return;
    }

    if (this.cls.isInstance(comp)) {
      this.cachedOnes.put(comp.getQualifiedName(), comp);
    } else if (this == ComponentType.SERVICE && comp instanceof ServiceInterface) {
      /*
       * that was bit clumsy, but the actual occurrence is rare, hence we
       * live with that
       */
      this.cachedOnes.put(comp.getQualifiedName(), comp);
    } else {
      throw new ApplicationError(
          "An object of type "
              + comp.getClass().getName()
              + " is being passed as component "
              + this);
    }
  }

  /**
   * remove the component from cache.
   *
   * @param compName fully qualified name
   */
  public void removeComponent(String compName) {
    if (this.cachedOnes != null) {
      this.cachedOnes.remove(compName);
    }
  }

  /**
   * service has a way to generate. We may have similar ones for other components in the future.
   * Default is to return null, so that the actual type can override this
   *
   * @param compName
   * @return
   */
  protected Component generateComp(String compName) {
    return null;
  }

  /**
   * load a component from storage into an instance
   *
   * @param compName
   * @return un-initialized component, or null if it is not found
   */
  public Object load(String compName) {
    if (this.preLoaded) {
      return this.cachedOnes.get(compName);
    }
    String fileName =
        componentFolder + this.folder + compName.replace(DELIMITER, FOLDER_CHAR) + EXTN;
    Exception exp = null;
    Object obj = null;
    try {
      obj = this.cls.newInstance();
      if (XmlUtil.xmlToObject(fileName, obj) == false) {
        /*
         * load failed. obj is not valid any more.
         */
        obj = null;
      }
    } catch (Exception e) {
      exp = e;
    }

    if (exp != null) {

      logger.error("error while loading component " + compName, exp);

      return null;
    }
    if (obj == null) {

      logger.info("error while loading component " + compName);

      return null;
    }
    /*
     * we insist that components be stored with the right naming convention
     */
    Component comp = (Component) obj;
    String fullName = comp.getQualifiedName();

    if (compName.equalsIgnoreCase(fullName) == false) {

      logger.info(
          "Component has a qualified name of "
              + fullName
              + " that is different from its storage name "
              + compName);

      return null;
    }
    return comp;
  }

  /**
   * load all components inside folder. This is used by components that are pre-loaded. These are
   * saved as collections, and not within their own files
   *
   * @param folder
   * @param packageName
   * @param objects
   */
  protected void loadAll() {
    try {
      loadGroups(this.folder, this.cls, this.cachedOnes);
      /*
       * we have to initialize the components
       */
      for (Object obj : this.cachedOnes.values()) {
        ((Component) obj).getReady();
      }

      logger.info(this.cachedOnes.size() + " " + this + " loaded.");

    } catch (Exception e) {
      this.cachedOnes.clear();

      logger.error(
          this
              + " pre-loading failed. No component of this type is available till we successfully pre-load them again.",
          e);
    }
  }

  /**
   * load all components inside folder. This is used by components that are pre-loaded. These are
   * saved as collections, and not within their own files
   *
   * @param folderName
   * @param rootClass this is typically the abstract class or the main class. Actual components
   *     would be sub-class of this. However, they should be part of the same package. we use
   *     package of this root class as the package for all components to be loaded. null if the
   *     group is to be loaded as name-value pairs.
   * @param objects
   */
  protected static void loadGroups(
      String folderName, Class<?> rootClass, Map<String, Object> objects) {
    String packageName = null;
    if (rootClass != null) {
      packageName = rootClass.getPackage().getName() + '.';
    }
    for (String resName : FileManager.getResources(componentFolder + folderName)) {
      if (resName.endsWith(EXTN) == false) {

        logger.info("Skipping Non-resource " + resName);

        continue;
      }

      logger.info("Going to load components from " + resName);

      try {
        XmlUtil.xmlToCollection(resName, objects, packageName);
      } catch (Exception e) {

        logger.error("Resource " + resName + " failed to load.", e);
      }
    }
  }

  /*
   * static methods that are used by infra-set up to load/cache components
   */
  /**
   * MUST BE CALLED AS PART OF APPLICATION INIT. Initial load, or reload of components that are
   * pre-loaded. It also resets and cached components that are not pre-loaded
   */
  private static void preLoad() {
    serviceAliases.clear();
    loadGroups(CLASS_FOLDER, null, serviceAliases);

    logger.info(serviceAliases.size() + " java class names loaded as services.");

    /*
     * clean and pre-load if required
     */
    for (ComponentType aType : ComponentType.values()) {
      if (aType.cachedOnes != null) {
        aType.cachedOnes.clear();
      }
      if (aType.preLoaded) {
        aType.loadAll();
      }
    }
  }

  /** let components be cached once they are loaded. Typically used in production environment */
  public static void startCaching() {
    /*
     * component caching happens if the collection exists
     */
    for (ComponentType aType : ComponentType.values()) {
      if (aType.preLoaded == false) {
        aType.cachedOnes = new HashMap<String, Object>();
      }
    }
  }

  /** purge cached components, and do not cache any more. During development. */
  public static void stopCaching() {
    /*
     * remove existing cache. Also, null implies that they are not be cached
     */
    for (ComponentType aType : ComponentType.values()) {
      if (aType.preLoaded == false) {
        aType.cachedOnes = null;
      }
    }
  }

  /**
   * set the root folder for components
   *
   * @param folder
   * @return actual folder being used. Possible that the missing folder character is added at the
   *     end
   */
  public static String setComponentFolder(String folder) {
    componentFolder = folder;
    if (folder.endsWith(FOLDER_STR) == false) {
      componentFolder += FOLDER_CHAR;
    }

    logger.info("component folder set to " + componentFolder);

    /*
     * Some components like data-type are to be pre-loaded for the app to
     * work.
     */
    preLoad();

    logger.info("components pre-loaded");

    return componentFolder;
  }

  /**
   * @return return the component folder. This is absolute folder in case of non-web environment,
   *     and relative to web-root in case of web environment. FileManager has the web-context if
   *     required.
   */
  public static String getComponentFolder() {
    return componentFolder;
  }
}
