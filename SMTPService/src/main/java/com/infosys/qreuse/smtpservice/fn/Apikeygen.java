package com.infosys.qreuse.smtpservice.fn;

import java.util.UUID;

import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.fn.AbstractFunction;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

public class Apikeygen extends AbstractFunction{

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
		return Value.newTextValue(UUID.randomUUID().toString());
	}

}
