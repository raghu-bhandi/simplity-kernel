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

/**
 * data structure that holds results of validation of components
 *
 * @author simplity.org
 *
 */
public class ValidationResult {
	private final String[][] allMessages;
	private final String[][] allComps;
	private final String[][] allReferences;

	/**
	 * this being a data structure, we want all data right at he beginning. We
	 * would have loved to make it immutable, but you know we can not do that
	 * with arrays :-(
	 *
	 * @param allMessages
	 * @param allComps
	 * @param allReferences
	 */
	public ValidationResult(String[][] allMessages, String[][] allComps,
			String[][] allReferences) {
		this.allMessages = allMessages;
		this.allComps = allComps;
		this.allReferences = allReferences;
	}

	/**
	 * @return the allComps
	 */
	public String[][] getAllComps() {
		return this.allComps;
	}

	/**
	 * @return the allMessages
	 */
	public String[][] getAllMessages() {
		return this.allMessages;
	}

	/**
	 * @return the allReferences
	 */
	public String[][] getAllReferences() {
		return this.allReferences;
	}
}
