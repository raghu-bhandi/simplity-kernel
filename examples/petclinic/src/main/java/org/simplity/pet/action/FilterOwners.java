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

package org.simplity.pet.action;

import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.service.ServiceContext;
import org.simplity.tp.ComplexLogicInterface;

/**
 * @author simplity.org
 *
 */
public class FilterOwners implements ComplexLogicInterface {
	private static final String OWNERS = "owners";
	private static final String PETS = "petDetails";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.tp.ComplexLogicInterface#execute(org.simplity.service.
	 * ServiceContext, org.simplity.kernel.db.DbDriver)
	 */
	@Override
	public int execute(ServiceContext ctx, DbDriver driver) {
		/*
		 * get rows from table owners using corresponding record
		 */
		Record owners = ComponentManager.getRecord(OWNERS);
		DataSheet ds = owners.filter(owners, ctx, driver, ctx.getUserId());
		ctx.putDataSheet(OWNERS, ds);

		/*
		 * read rows from pets for the filtered owners
		 */
		int nbrRows = ds.length();
		if (nbrRows > 0) {
			Record pets = ComponentManager.getRecord(PETS);
			pets.filterForParents(ds, driver, PETS, false, ctx);
		}
		return nbrRows;
	}
}
