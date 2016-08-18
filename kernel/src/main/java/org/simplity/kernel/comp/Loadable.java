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

import org.simplity.kernel.util.XmlParseException;

/**
 * class that is designed to be loaded from an xml file using XMlUtils
 *
 * @author simplity.org
 *
 */
public interface Loadable {
	/**
	 * If an object that is loaded from an xml has a field that is a collection
	 * of objects, specifically an array of objects, or Map of objects, then the
	 * xml loader uses tag as the name of class.(with first character UPPed). By
	 * default it assumes that theses classes are in the same package as the
	 * parent object. If this is not true, this method should return an instance
	 * of the object to be used to load the element
	 *
	 *
	 * @param tagName
	 * 			Tagname
	 * @return object to be used to load this element
	 * @throws XmlParseException
	 *             in case of any error in loading
	 */
	public Object getObjectToLoad(String tagName) throws XmlParseException;
}
