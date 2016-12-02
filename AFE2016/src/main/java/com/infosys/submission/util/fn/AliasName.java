package com.infosys.submission.util.fn;

import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.fn.AbstractFunction;
import org.simplity.kernel.value.InvalidValueException;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

public class AliasName extends AbstractFunction{

	@Override
	public ValueType getReturnType() {
		return ValueType.TEXT;
	}

	@Override
	public ValueType[] getArgDataTypes() {
		return new ValueType[]{ValueType.TEXT,ValueType.INTEGER};
	}

	@Override
	public Value execute(Value[] arguments, FieldsInterface data) {
		if (arguments == null || arguments.length < 1) {
			return Value.VALUE_EMPTY;
		}
		String output = null;
		try {
			long count = arguments[1].toInteger()+1;
			output = arguments[0].toString()+"_"+count;
		} catch (InvalidValueException e) {
			e.printStackTrace();
		}		
		return Value.newTextValue(output);
	}

}
