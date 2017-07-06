package org.simplity.ide;

import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.comp.ValidationResult;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

/**
 * We send the service.xml file as it is to client
 *
 * @author simplity.org
 */
public class ValidateComp implements LogicInterface {

  /*
   * (non-Javadoc)
   *
   * @see
   * org.simplity.tp.LogicInterface#execute(org.simplity.service.ServiceContext
   * )
   */
  @Override
  public Value execute(ServiceContext ctx) {

    ValidationContext vtx = new ValidationContext();
    ValidationResult result = vtx.validateAll();

    String[][] data = result.getAllComps();
    ValueType[] types = {ValueType.TEXT, ValueType.TEXT, ValueType.INTEGER};
    DataSheet ds = new MultiRowsSheet(data, types);
    ctx.putDataSheet("components", ds);
    int nbr = data.length - 1;

    data = result.getAllMessages();
    ds = new MultiRowsSheet(data);
    ctx.putDataSheet("messages", ds);

    data = result.getAllReferences();
    ds = new MultiRowsSheet(data);
    ctx.putDataSheet("references", ds);
    return Value.newIntegerValue(nbr);
  }
}
