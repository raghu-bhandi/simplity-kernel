package com.infosys.submission.util.fn;

import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.fn.AbstractFunction;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

public class Lowercase extends AbstractFunction{

	@Override
	public ValueType getReturnType() {
		return ValueType.TEXT;
	}

	@Override
	public ValueType[] getArgDataTypes() {
		return new ValueType[]{ValueType.TEXT};
	}

	@Override
	public Value execute(Value[] arguments, FieldsInterface data) {
		if (arguments == null || arguments.length < 1) {
			return Value.VALUE_EMPTY;
		}
		String output;
		output = arguments[0].toString().toLowerCase();
		
		return Value.newTextValue(output);
	}

}
