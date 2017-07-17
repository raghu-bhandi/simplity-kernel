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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceInterface;

/**
 * Service that is to be executed as a step/action in side another service.
 *
 * @author simplity.org
 */
public class SubService extends Action {
  static final Logger logger = LoggerFactory.getLogger(SubService.class);

  String serviceName;

  private boolean transactionIsDelegated;

  @Override
  protected Value delegate(ServiceContext ctx, DbDriver driver) {
    ServiceInterface service = ComponentManager.getService(this.serviceName);

    logger.info("service " + this.serviceName + " started as sub service.");

    Value result = service.executeAsAction(ctx, driver, this.transactionIsDelegated);

    logger.info("service " + this.serviceName + " returned control back.");

    return result;
  }

  /* (non-Javadoc)
   * @see org.simplity.tp.Action#getReady(int)
   */
  @Override
  public void getReady(int idx, Service service) {
    super.getReady(idx, service);
    if (service.dbAccessType == DbAccessType.SUB_SERVICE) {
      this.transactionIsDelegated = true;
    }
  }
  /*
   * (non-Javadoc)
   *
   * @see
   * org.simplity.tp.Action#validate(org.simplity.kernel.comp.ValidationContext
   * , org.simplity.tp.Service)
   */
  @Override
  public int validate(ValidationContext ctx, Service service) {
    int count = super.validate(ctx, service);
    if (this.serviceName == null) {
      ctx.addError("subService action requires serviceName");
      count++;
    } else {
      if (ctx.checkServiceName(this.serviceName, "serviceName")) {
        count++;
      }
    }
    return count;
  }
}
