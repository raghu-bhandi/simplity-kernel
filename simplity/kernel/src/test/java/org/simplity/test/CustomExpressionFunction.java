package org.simplity.test;

import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.fn.AbstractFunction;
import org.simplity.kernel.value.InvalidValueException;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

public class CustomExpressionFunction extends AbstractFunction {

  @Override
  public ValueType getReturnType() {
    return ValueType.INTEGER;
  }

  @Override
  public ValueType[] getArgDataTypes() {
    return new ValueType[] {ValueType.INTEGER, ValueType.INTEGER};
  }

  @Override
  public Value execute(Value[] arguments, FieldsInterface data) {
    if (arguments == null || arguments.length < 2) {
      return Value.VALUE_EMPTY;
    }
    int output = 0;
    try {
      output = (int) (arguments[0].toInteger() + arguments[1].toInteger());
    } catch (InvalidValueException e) {
      e.printStackTrace();
    }

    return Value.newIntegerValue(output);
  }
}
