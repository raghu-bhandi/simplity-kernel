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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.tp;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.dm.SaveActionType;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Save (add/modify/delete) a row from a record, and possibly save relevant rows
 * from related records
 *
 *
 * @author simplity.org
 *
 */
public class Save extends DbAction {

	/**
	 * qualified record name
	 */
	String recordName;

	/**
	 * if this is for more than one rows, and the data is to be received in a
	 * sheet
	 */
	String inputSheetName;

	/**
	 * add/update/delete/auto/replace . Auto means update if primary key is
	 * specified, else add
	 */
	SaveActionType saveAction = SaveActionType.SAVE;

	/**
	 * do we save child records
	 */
	RelatedRecord[] childRecords;

	/**
	 * at times, you may design an insert operation that will try to insert,
	 * failing which you may want to update. In such cases, you may get a sql
	 * error on key-violation. By default we would raise an exception. You may
	 * alter this behavior with this keyword.
	 */
	boolean treatSqlErrorAsNoResult;

	/**
	 * default constructor
	 */
	public Save() {
		//
	}

	/**
	 * get a save action for this record
	 *
	 * @param record
	 * @param children
	 */
	public Save(Record record, RelatedRecord[] children) {
		this.actionName = "save_" + record.getSimpleName();
		this.inputSheetName = record.getDefaultSheetName();
		this.recordName = record.getQualifiedName();
		this.childRecords = children;
	}

	@Override
	protected int doDbAct(ServiceContext ctx, DbDriver driver) {
		if(driver==null){
			throw new ApplicationError("Save requires the service dbAccessType to be set to read-write");
		}		
		if (this.inputSheetName == null) {
			return this.saveFromFields(ctx, driver);
		}
		DataSheet inSheet = ctx.getDataSheet(this.inputSheetName);
		if (inSheet == null) {
			return this.saveFromFields(ctx, driver);
		}
		int nbrRows = inSheet.length();
		Tracer.trace("Starting save for a sheet with " + nbrRows);
		if (nbrRows == 0) {
			return nbrRows;
		}

		Record record = ComponentManager.getRecord(this.recordName);
		int nbrRowsAffected = 0;
		SaveActionType[] actions = null;
		Value userId = ctx.getUserId();
		if (this.saveAction == SaveActionType.MODIFY) {
			nbrRowsAffected = record.update(inSheet, driver, userId, false);
		} else if (this.saveAction == SaveActionType.ADD) {
			nbrRowsAffected = record.insert(inSheet, driver, userId, false);
		} else if (this.saveAction == SaveActionType.DELETE) {
			nbrRowsAffected = record.delete(inSheet, driver, false);
		} else {
			actions = record.saveMany(inSheet, driver, userId, false);
			nbrRowsAffected = actions.length;
		}
		if (this.childRecords != null) {
			Tracer.trace("Child records are valid only when parent is for a single row. Data if any, ignored.");
		}
		return nbrRowsAffected;
	}

	/**
	 * TODO: this is a clone of the above. we have to think and re-factor this
	 * to avoid this duplication
	 *
	 * @param ctx
	 * @param driver
	 * @return
	 */
	private int saveFromFields(ServiceContext ctx, DbDriver driver) {
		Record record = ComponentManager.getRecord(this.recordName);
		int nbrRowsAffected = 0;
		Value userId = ctx.getUserId();
		/*
		 * if action is 'save' it will be set to either add or modify later
		 */
		SaveActionType action = this.saveAction;
		if (this.saveAction == SaveActionType.DELETE) {
			if (this.childRecords == null) {
				return record.delete(ctx, driver, this.treatSqlErrorAsNoResult);
			}
			/*
			 * it is quite likely that the rdbms designer would have put
			 * constraints on child rows. So let us check with child rows before
			 * deleting this row
			 */
			int nbrChildrenSaved = this.saveChildRows(ctx, driver, userId,
					action);
			nbrRowsAffected = record.delete(ctx, driver,
					this.treatSqlErrorAsNoResult);
			if (nbrRowsAffected == 0 && nbrChildrenSaved > 0) {
				/*
				 * ooops. we deleted child rows, but parent is not deleted, may
				 * be because of time-stamp..
				 */
				throw new ApplicationError(
						"Data was changed by some one else while you were editing it. Please cancel this operation and redo it with latest data.");
			}
			return nbrRowsAffected + nbrChildrenSaved;
		}
		/*
		 * it is either update or insert, implicitly or explicitly
		 */
		if (this.saveAction == SaveActionType.MODIFY) {
			nbrRowsAffected = record.update(ctx, driver, userId,
					this.treatSqlErrorAsNoResult);
		} else if (this.saveAction == SaveActionType.ADD) {
			nbrRowsAffected = record.insert(ctx, driver, userId,
					this.treatSqlErrorAsNoResult);
		} else {
			action = record.saveOne(ctx, driver, userId,
					this.treatSqlErrorAsNoResult);
			nbrRowsAffected = action == null ? 0 : 1;
		}
		if (nbrRowsAffected > 0 && this.childRecords != null) {
			nbrRowsAffected += this.saveChildRows(ctx, driver, userId, action);
		}
		return nbrRowsAffected;
	}

	/**
	 * process child rows when a parent is affected
	 *
	 * @param ctx
	 * @param driver
	 * @param userId
	 * @param action
	 * @return
	 */
	private int saveChildRows(ServiceContext ctx, DbDriver driver,
			Value userId, SaveActionType action) {
		int nbr = 0;
		for (RelatedRecord rr : this.childRecords) {
			Record record = ComponentManager.getRecord(rr.recordName);
			DataSheet relatedSheet = ctx.getDataSheet(rr.sheetName);
			if (rr.replaceRows) {
				nbr += record.deleteWithParent(ctx, driver, userId);
				if (relatedSheet == null || relatedSheet.length() == 0) {
					Tracer.trace("Rows in record "
							+ rr.recordName
							+ " deleted, as there were no rows to replace them.");
					continue;
				}
				nbr += record.insertWithParent(relatedSheet, ctx, driver,
						userId);
				Tracer.trace("Rows in record " + rr.recordName
						+ " replaced based on " + relatedSheet.length()
						+ " rows of data in sheet " + rr.sheetName);
				continue;
			}

			if (relatedSheet == null || relatedSheet.length() == 0) {
				Tracer.trace("Related record " + rr.recordName
						+ " not saved as there is no data in sheet "
						+ rr.sheetName);
				continue;
			}


			Tracer.trace("Saving children is a noble cause!! Going to save child record "
					+ rr.recordName + " for action = " + action);
			if (action == SaveActionType.ADD) {
				nbr += record.insertWithParent(relatedSheet, ctx, driver,
						userId);
			} else if (action == SaveActionType.DELETE) {
				nbr += record.deleteWithParent(relatedSheet, driver, userId);
			} else if (rr.replaceRows) {
				nbr += record.deleteWithParent(ctx, driver, userId);
				nbr += record.insertWithParent(relatedSheet, ctx, driver,
						userId);
			} else {
				/*
				 * when the parent is modified, child rows could be update as
				 * well as insert/delete!!
				 */
				SaveActionType[] actions = { action };
				nbr += record.saveWithParent(relatedSheet, ctx, actions,
						driver, userId);
			}
		}
		return nbr;
	}

	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.READ_WRITE;
	}

	@Override
	public void getReady(int idx) {
		super.getReady(idx);
		if (this.childRecords != null) {
			for (RelatedRecord rec : this.childRecords) {
				rec.getReady();
			}
		}
	}

	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);
		if (this.recordName == null) {
			ctx.addError("Record name is required for save action");
			count++;
		} else {
			ctx.addReference(ComponentType.REC, this.recordName);
		}
		if (this.childRecords != null) {
			for (RelatedRecord rec : this.childRecords) {
				count += rec.validate(ctx);
			}
		}
		return count;
	}
}
