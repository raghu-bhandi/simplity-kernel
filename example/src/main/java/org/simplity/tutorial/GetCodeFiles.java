package org.simplity.tutorial;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceData;
import org.simplity.tp.LogicInterface;

public class GetCodeFiles implements LogicInterface {
	final static String SUBPATH = "snippets/";
	final static String XMPSTART = "<xmp>";
	final static String XMPEND = "</xmp>";

	@Override
	public Value execute(ServiceContext ctx) {
		String filename = ctx.getTextValue("filename");
		filename = SUBPATH + filename;
		StringBuilder out = new StringBuilder();

		File reqfile = new File(getClass().getClassLoader().getResource(filename).getFile());
		try {
			BufferedReader bfr = new BufferedReader(new FileReader(reqfile));
			String line;
			out.append(XMPSTART);
			while ((line = bfr.readLine()) != null) {
				out.append(line);
				out.append(System.getProperty("line.separator"));
			}
			out.append(XMPEND);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ctx.setTextValue("filecontents", out.toString());
		return  Value.newTextValue("Returned contents for "+filename);
	}

}
