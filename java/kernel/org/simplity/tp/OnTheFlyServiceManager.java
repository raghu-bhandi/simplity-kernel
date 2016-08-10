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
package org.simplity.tp;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.OnTheFlyManager;
import org.simplity.kernel.dm.Record;
import org.simplity.service.ServiceInterface;

/**
 * Create a service on-the fly for some simple operations. we had created a
 * plug-in arrangement, but it was over-engineering and hence we have some back
 * to simple look-up. Will re-factor back to plugin in case we get too complex
 * with this. service.
 *
 * @author simplity.org
 *
 */
public class OnTheFlyServiceManager implements
OnTheFlyManager<ServiceInterface> {
	private static final char PREFIX_DELIMITER = '_';
	private static final String GET = "get";
	private static final String FILTER = "filter";
	private static final String SAVE = "save";
	/**
	 * used by suggestion service as well
	 */
	public static final String SUGGEST = "suggest";

	/**
	 * list service needs to use this
	 */
	private static final String LIST = "list";

	@Override
	public ServiceInterface create(String serviceName) {
		int idx = serviceName.indexOf(PREFIX_DELIMITER);
		if (idx == -1) {
			return null;
		}

		String operation = serviceName.substring(0, idx);
		String recordName = serviceName.substring(idx + 1);

		Record record = ComponentManager.getRecordOrNull(recordName);
		if (record == null) {
			return null;
		}
		if (operation.equals(LIST)) {
			return ListService.getService(serviceName, record);
		}

		if (operation.equals(FILTER)) {
			return Service.getFilterService(serviceName, record);
		}
		if (operation.equals(GET)) {
			return Service.getReadService(serviceName, record);
		}
		if (operation.equals(SAVE)) {
			return Service.getSaveService(serviceName, record);
		}
		if (operation.equals(SUGGEST)) {
			return SuggestionService.getService(serviceName, record);
		}

		Tracer.trace("We have no on-the-fly servce generator for operation "
				+ operation);
		return null;
	}
}
