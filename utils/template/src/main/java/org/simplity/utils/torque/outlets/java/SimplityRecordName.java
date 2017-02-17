package org.simplity.utils.torque.outlets.java;

import org.apache.torque.generator.GeneratorException;
import org.apache.torque.generator.control.ControllerState;
import org.apache.torque.generator.outlet.OutletImpl;
import org.apache.torque.generator.outlet.OutletResult;
import org.apache.torque.generator.qname.QualifiedName;
import org.simplity.utils.utils.Utils;

public class SimplityRecordName extends OutletImpl {

	public SimplityRecordName(QualifiedName name) {
		super(name);
	}

	@Override
	public OutletResult execute(ControllerState controllerState) throws GeneratorException {
		return new OutletResult(Utils.toCamelCase(controllerState.getSourceElement().getAttribute("name").toString())+".xml");
	}

}
