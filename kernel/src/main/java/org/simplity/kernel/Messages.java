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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel;

import org.simplity.kernel.comp.ComponentType;

/**
 * This class works as a wrapper on top of component manager to deal with
 * messages. It also serves as a place to define al internally used messages
 *
 * @author simplity.org
 *
 */
public class Messages {
	/*
	 * messages used internally
	 * 
	 * these are defined in kernel.xml under msg folder. we have to ensure that
	 * this list and that file are in synch.
	 */
	/**
	 * a min number of rows expected
	 */
	public static final String INVALID_VALUE = "kernel.invalidValue";
	/**
	 * a max nbr of rows expected
	 */
	public static final String INVALID_DATA = "kernel.invalidData";
	/**
	 *
	 */
	public static final String INVALID_FIELD = "kernel.invalidField";
	/**
	 *
	 */
	public static final String INVALID_COMPARATOR = "kernel.invalidComparator";

	/**
	 *
	 */
	public static final String INVALID_ENTITY_LIST = "kernel.invalidEntityList";

	/**
	 *
	 */
	public static final String VALUE_REQUIRED = "kernel.valueRequired";
	/**
	 *
	 */
	public static final String INVALID_FROM_TO = "kernel.invalidFromTo";
	/**
	 *
	 */
	public static final String INVALID_OTHER_FIELD = "kernel.invalidOtherField";
	/**
	 *
	 */
	public static final String INVALID_SORT_ORDER = "kernel.invalidSortOrder";
	/**
	 *
	 */
	public static final String INVALID_BASED_ON_FIELD = "kernel.invalidBasedOnField";
	/**
	 *
	 */
	public static final String INTERNAL_ERROR = "kernal.internalError";

	/**
	 *
	 */
	public static final String INVALID_TABLE_ACTION = "kernal.invalidTableAction";

	/**
	 *
	 */
	public static final String INVALID_SERIALIZED_DATA = "kernel.invalidInputStream";
	/**
	 *
	 */
	public static final String NO_SERVICE = "kernel.noService";
	/**
	 *
	 */
	public static final String SUCCESS = "kernel.success";

	/**
	 * get message text for this message after formatting based on parameters
	 *
	 * @param messageName
	 * @param parameters
	 * @return formatted message text
	 */
	public static FormattedMessage getMessage(String messageName,
			String... parameters) {
		Message message = (Message) ComponentType.MSG
				.getComponentOrNull(messageName);
		if (message == null) {
			message = defaultMessage(messageName);
		}
		return message.getFormattedMessage(parameters);
	}

	private static Message defaultMessage(String messageName) {
		Message msg = new Message();
		msg.name = messageName;
		msg.text = messageName
				+ " : description for this message is not found.";
		Tracer.trace("Missing mssage : " + messageName);
		return msg;
	}
}
