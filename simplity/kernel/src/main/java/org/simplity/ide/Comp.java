package org.simplity.ide;

import java.util.HashSet;
import java.util.Set;

import org.simplity.kernel.Application;
import org.simplity.kernel.comp.Component;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author simplity.org
 *
 */
public class Comp {
	private static final Logger logger = LoggerFactory.getLogger(Comp.class);
	/**
	 * what type of component is this
	 */
	final ComponentType componentType;
	/**
	 * unique id. compType + compId woudl be unique
	 */
	final String compId;
	/**
	 * whenever a reference is made, we may create a conceptual comp, but the
	 * actual component may or may not be defined
	 */
	boolean compExists;

	/**
	 * in which file is this defined?
	 */
	final CompilationUnit compilationUnit;

	/**
	 * set of other components that are required by this comp. This is one-step,
	 * and does not take care of transitive dependencies. That is, if this comp
	 * requires A and A requires B we do not store B here
	 */
	Set<Comp> compsUsed;

	/**
	 * set of other comps that require this comp. Does not store transitively
	 * dependent comps.
	 */
	Set<Comp> usedByComps;

	/**
	 * @param componentType
	 * @param compId
	 * @param underlyingObject
	 * @param cu
	 * @param dependents
	 *            Any reference accumulated before this is actually read
	 * @param vtx
	 */
	public Comp(ComponentType componentType, String compId, Object underlyingObject, CompilationUnit cu,
			Set<Comp> dependents, ValidationContext vtx) {
		this.componentType = componentType;
		this.compId = compId;
		this.compilationUnit = cu;
		this.usedByComps = dependents;

		/*
		 * validate this object
		 */
		vtx.beginComp(this);
		this.compExists = true;
		if (underlyingObject instanceof Component) {
			((Component) underlyingObject).validate(vtx);
		} else if (underlyingObject instanceof Application) {
			((Application) underlyingObject).validate(vtx);
		} else {
			logger.error(" We do not know how to validate an object of lass " + underlyingObject.getClass().getName());

			this.compExists = false;
		}
		vtx.endComp();
	}

	/**
	 * construct a comp for reference before it is parsed. This is temp, and
	 * will be replaced, if found with the parsed on later
	 *
	 * @param componentType
	 * @param compId
	 */
	public Comp(ComponentType componentType, String compId) {
		this.componentType = componentType;
		this.compId = compId;
		this.compilationUnit = null;
		this.usedByComps = null;

		this.compExists = false;
		this.compsUsed = null;
		return;
	}

	/**
	 * method called-back by validation context whenever an error is detected
	 *
	 * @param error
	 */
	public void addError(String error) {
		this.compilationUnit.addError(error);
	}

	/**
	 * method called-back by validation context whenever a warning is detected
	 *
	 * @param warning
	 */
	public void addWarning(String warning) {
		this.compilationUnit.addWarning(warning);
	}

	/**
	 * method called-back by validation context whenever a reference from one
	 * component to another is detected
	 *
	 * @param refType
	 * @param refName
	 */
	public void addReferredComp(ComponentType refType, String refName) {
		this.compsUsed.add(CompsManager.getRefComp(refType, refName));
	}

	/**
	 *
	 * @return comps that use/depend-on this comp. null if no such reference
	 */
	public Set<Comp> getDependentComps() {
		return this.usedByComps;
	}

	/**
	 *
	 * @param comp
	 *            that depends on this comp
	 */
	public void setDependentComp(Comp comp) {
		if (this.usedByComps == null) {
			this.usedByComps = new HashSet<Comp>();
		}
		this.usedByComps.add(comp);
	}
}
