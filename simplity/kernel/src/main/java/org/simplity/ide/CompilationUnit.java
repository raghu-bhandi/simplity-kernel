/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.ide;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.simplity.kernel.comp.ComponentType;

/**
 * content of a file that contains a simplity component
 *
 * @author simplity.org
 *
 */
public class CompilationUnit {

	/**
	 * file name relative to the base. Relevant only when instantiated in the
	 * context of an editor.
	 */
	String fileName;

	/**
	 * what type of components does this contain?
	 */
	ComponentType componentType;
	/**
	 * does this unit contain single or multiple components. For example
	 * dataType.xml file contains multiple while service.xml has only one
	 */
	boolean hasMultipleComps;

	/**
	 * list of all error in this file. this is to be replaced with eclipse
	 * related object.
	 * TODO: how do we link this to eclipse error, so that we can remove them as
	 * and when this is fixed
	 */
	List<String> errors;

	/**
	 * list of all warnings in this file. this is to be replaced with eclipse
	 * related object.
	 * TODO: how do we link this to eclipse so that we can remove them when this
	 * is fixed
	 */
	List<String> warnings;

	/**
	 * content of this file
	 */
	Set<Comp> comps = new HashSet<Comp>();

	/**
	 * @param fileName
	 * @param componentType
	 * @param hasMultiple
	 */
	public CompilationUnit(String fileName, ComponentType componentType, boolean hasMultiple) {
		this.fileName = fileName;
		this.componentType = componentType;
		this.hasMultipleComps = hasMultiple;
	}


	/**
	 * @param comp
	 */
	public void addComp(Comp comp) {
		this.comps.add(comp);
	}

	/**
	 * @param warning
	 */
	public void addWarning(String warning) {
		this.warnings.add(warning);
	}


	/**
	 * @param error
	 */
	public void addError(String error) {
		this.errors.add(error);
	}

	/**
	 * @return collection of errors. Never null, could be empty
	 */
	public Collection<String> getErrors(){
		return this.errors;
	}

	/**
	 * @return collection of errors. Never null, could be empty
	 */
	public Collection<String> getWarnings(){
		return this.warnings;
	}

}
