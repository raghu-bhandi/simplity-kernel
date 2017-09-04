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
package org.simplity.tp;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbClientInterface;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Base class for all action that deal with db
 *
 * @author simplity.org
 */
public abstract class DbAction extends Action {
  /** schema name, different from the default schema, to be used specifically for this service */
  String schemaName;

  @Override
  public Value delegate(ServiceContext ctx, DbDriver driver) {
    int result = 0;
    if (this.useNewDriver(driver)) {
      /*
       * service has delegated transactions to its actions...
       * We have to directly deal with the driver for this
       */
      Worker worker = new Worker(ctx);
      if (DbDriver.workWithDriver(worker, this.getDataAccessType(), this.schemaName)) {
        result = worker.getResult();
      }
    } else {
      result = this.doDbAct(ctx, driver);
    }
    return Value.newIntegerValue(result);
  }

  private boolean useNewDriver(DbDriver driver) {
    /*
     * read-only is always fine with called driver
     */
    DbAccessType dat = this.getDataAccessType();
    if (dat == null || dat.updatesDb() == false) {
      return false;
    }
    /*
     * is the driver equipped to do updates?
     */
    if (driver == null) {
      return true;
    }

    dat = driver.getAccessType();
    if (dat == null || dat.updatesDb() == false) {
      return true;
    }
    /*
     * caller can update
     */
    return false;
  }
  /**
   * let the concrete action do its job.
   *
   * @param ctx
   * @param driver
   * @return
   */
  protected abstract int doDbAct(ServiceContext ctx, DbDriver driver);

  /*
   * (non-Javadoc)
   *
   * @see
   * org.simplity.tp.Action#validate(org.simplity.kernel.comp.
   * ValidationContext
   * , org.simplity.tp.Service)
   */
	@Override
	public int validate(ValidationContext ctx, Service service) {
		String tagName = TextUtil.classNameToName(this.getClass().getSimpleName());
		System.out.println(this.getClass().getSimpleName() + " - " + tagName);
		ctx.beginTag(tagName);
		int count = super.validate(ctx, service);
		if (this.failureMessageName != null) {
			ctx.addReference(ComponentType.MSG, this.failureMessageName);
		}
		if (this.successMessageName != null) {
			ctx.addReference(ComponentType.MSG, this.successMessageName);
		}
		ctx.endTag(tagName);
		return count;
	}

  /*
   * as per our design, this method should never be invoked
   * (non-Javadoc)
   *
   * @see org.simplity.tp.Action#doAct(org.simplity.service.ServiceContext)
   */
  @Override
  protected final Value doAct(ServiceContext ctx) {
    throw new ApplicationError("Design Error : DbActionis called without a driver");
  }

  /**
   * worker class to work with the driver
   *
   * @author simplity.org
   */
  protected class Worker implements DbClientInterface {
    private final ServiceContext ctx;
    private int result = 0;

    Worker(ServiceContext ctx) {
      this.ctx = ctx;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.simplity.kernel.db.DbClientInterface#workWithDriver(org.simplity.
     * kernel.db.DbDriver)
     */
    @Override
    public boolean workWithDriver(DbDriver driver) {
      this.result = DbAction.this.doDbAct(this.ctx, driver);
      if (this.ctx.isInError()) {
        return false;
      }
      return true;
    }

    int getResult() {
      return this.result;
    }
  }
}
