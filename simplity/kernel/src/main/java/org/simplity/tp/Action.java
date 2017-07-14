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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.MessageType;

import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * An action inside a service
 *
 * @author simplity.org
 */
public abstract class Action {
  static final Logger logger = LoggerFactory.getLogger(Action.class);

  private static final String ACTION_NAME_PREFIX = "_a";

  /** unique name within a service */
  protected String actionName = null;

  /** precondition to be met for this step to be executed. */
  protected Expression executeOnCondition = null;

  /** if you want to execute this step if a sheet exists and has at least one row */
  protected String executeIfRowsInSheet;

  /** execute if there is no sheet, or sheet has no rows */
  protected String executeIfNoRowsInSheet;

  /**
   * if the sql succeeds in extracting at least one row, or affecting one update, do we need to put
   * a message?
   */
  String successMessageName;
  /** comma separated list of parameters, to be used to populate success message */
  String[] successMessageParameters;

  /** if the sql fails to extract/update even a single row, should we flash any message? */
  String failureMessageName;
  /** parameters to be used to format failure message */
  String[] failureMessageParameters;

  /** should we stop this service in case the message added is of type error. */
  boolean stopIfMessageTypeIsError;

  /**
   * name of action to navigate to within this block, (_stop, _continue and _break are special
   * commands, as in jumpTo)
   */
  String actionNameOnSuccess;
  /**
   * name of action to navigate to within this block, (_stop, _continue and _break are special
   * commands, as in jumpTo)
   */
  String actionNameOnFailure;

  private int serviceIdx;

  private boolean requiresPostProcessing;

  /**
   * main method called by service.
   *
   * @param ctx
   * @param driver
   * @return an indicator of what the action did. This value is saved as a field named
   *     actionNameResult that can be used by subsequent actions. null implies no such feature
   */
  public final Value act(ServiceContext ctx, DbDriver driver) {
    /*
     * is this a conditional step? i.e. to be executed only if the condition
     * is met
     */

    if (this.executeOnCondition != null) {
      try {
        Value val = this.executeOnCondition.evaluate(ctx);
        if (Value.intepretAsBoolean(val)) {

          logger.info(
              "Cleared the condition " + this.executeOnCondition + " for action to proceed.");

        } else {

          logger.info("Condition " + this.executeOnCondition + " and hence skipping this action.");

          return null;
        }
      } catch (Exception e) {
        throw new ApplicationError(
            "Action "
                + this.actionName
                + " has an executOnCondition="
                + this.executeOnCondition.toString()
                + " that is invalid. \nError : "
                + e.getMessage());
      }
    }
    if (this.executeIfNoRowsInSheet != null
        && ctx.nbrRowsInSheet(this.executeIfNoRowsInSheet) > 0) {
      return null;
    }
    if (this.executeIfRowsInSheet != null && ctx.nbrRowsInSheet(this.executeIfRowsInSheet) == 0) {
      return null;
    }
    Value result = this.delegate(ctx, driver);
    if (this.requiresPostProcessing == false) {
      return result;
    }
    boolean ok = Value.intepretAsBoolean(result);
    if (ok) {
      if (this.actionNameOnSuccess != null) {
        return Value.newTextValue(this.actionNameOnSuccess);
      }
      if (this.successMessageName != null) {
        MessageType msgType =
            ctx.addMessage(this.successMessageName, this.successMessageParameters);
        if (msgType == MessageType.ERROR && this.stopIfMessageTypeIsError) {
          return Service.STOP_VALUE;
        }
      }
      return result;
    }
    if (this.actionNameOnFailure != null) {
      return Value.newTextValue(this.actionNameOnFailure);
    }
    if (this.failureMessageName != null) {
      MessageType msgType = ctx.addMessage(this.failureMessageName, this.failureMessageParameters);
      if (msgType == MessageType.ERROR && this.stopIfMessageTypeIsError) {
        return Service.STOP_VALUE;
      }
    }
    return result;
  }

  /**
   * This is the intermediate method that can be implemented by actions that actually use db driver.
   * We provide a default delegate that does not use driver
   *
   * @param ctx
   * @param driver
   * @return value from action
   */
  protected Value delegate(ServiceContext ctx, DbDriver driver) {
    return this.doAct(ctx);
  }

  /**
   * Method to be implemented by actions that do not use a driver. We provide it as a dummy, rather
   * than making it abstract to provide concrete actions to wither have a method with driver or
   * without
   *
   * @param ctx
   * @return
   */
  protected Value doAct(ServiceContext ctx) {
    return Value.VALUE_TRUE;
  }

  /**
   * * what type of data access does this action require?
   *
   * @return data base access type required by this step.
   */
  public DbAccessType getDataAccessType() {
    return DbAccessType.NONE;
  }

  /** @return name of this action */
  public String getName() {
    return this.actionName;
  }

  /**
   * if there is anything this class wants to do after loading its attributes, but before being
   * used, here is the method to do that.
   *
   * @param idx 0 based index of actions in service
   * @param service to which this action belongs to
   */
  public void getReady(int idx, Service service) {
    this.serviceIdx = idx;
    if (this.actionName == null) {
      this.actionName = ACTION_NAME_PREFIX + this.serviceIdx;
    }
    this.requiresPostProcessing =
        this.actionNameOnFailure != null
            || this.actionNameOnSuccess != null
            || this.failureMessageName != null
            || this.successMessageName != null;
  }

  /**
   * validate this action
   *
   * @param vtx
   * @param service parent service
   * @return number of errors added to the list
   */
  public int validate(ValidationContext vtx, Service service) {
    return 0;
  }
}
