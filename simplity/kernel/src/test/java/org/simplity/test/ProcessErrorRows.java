package org.simplity.test;

import java.util.List;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.AbstractService;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class ProcessErrorRows extends AbstractService implements LogicInterface {

  @Override
  public Value executeAsAction(
      ServiceContext ctx, DbDriver driver, boolean useOwnDriverForTransaction) {

    System.out.println("Hello");
    List<FormattedMessage> msgs = ctx.getMessages();
    for (FormattedMessage msg : msgs) {
      System.out.println(msg.text);
      System.out.println(msg.data[0]);
      System.out.println(msg.fieldName);
    }

    return super.executeAsAction(ctx, driver, useOwnDriverForTransaction);
  }

  @Override
  public Value execute(ServiceContext ctx) {
    return this.executeAsAction(ctx, null, false);
  }
}
