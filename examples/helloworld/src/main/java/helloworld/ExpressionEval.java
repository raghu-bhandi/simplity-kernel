package helloworld;

import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.fn.AbstractFunction;
import org.simplity.kernel.value.InvalidValueException;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

public class ExpressionEval extends AbstractFunction{

	@Override
	public ValueType getReturnType() {
		return ValueType.TEXT;
	}

	@Override
	public ValueType[] getArgDataTypes() {
		return new ValueType[]{ValueType.INTEGER,ValueType.TEXT};
	}

	@Override
	public Value execute(Value[] arguments, FieldsInterface data) {
		try {
			System.out.println(arguments[0].toInteger());
			System.out.println(arguments[1].toString());
			
		} catch (InvalidValueException e) {
			e.printStackTrace();
		}		
		return Value.newTextValue("success");
	}

}
