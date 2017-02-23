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

package org.simplity.jms;

/**
 * how the JMS message body is used
 * @author simplity.org
 *
 */
public enum MessageBodyType {
	/**
	 * single field value as text.Actual value could be numeric etc..
	 */
	TEXT
	/**
	 * field values in a predefined sequence and fixed width for each field
	 */
	,FIXED_WIDTH
	/**
	 * field values in a predefined sequence separated by comma
	 */
	,COMMA_SEPARATED
	/**
	 * name1=value1,name2=value2...
	 */
	,COMMA_SEPARATED_PAIRS
	/**
	 * JSONtext with field and value
	 */
	,JSON
	/**
	 * xml with each field as an attribute
	 */
	,XML_ATTRIBUTES
	/**
	 * xml with each field as an element
	 */
	,XML_ELEMENTS
	/**
	 * text contains data as in an http form submit name1=value1&name2=value2 etc.. but is already unescaped
	 */
	,FORM_DATA
	/**
	 * text is yaml
	 */
	,YAML
	/**
	 * serialized object
	 */
	,OBJECT
	/**
	 * map of field values
	 */
	,MAP
}