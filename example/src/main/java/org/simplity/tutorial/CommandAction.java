package org.simplity.tutorial;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

/**
 * An example to send filtered list from ps -e command to client
 * 
 * @author simplity.org
 *
 */
public class CommandAction implements LogicInterface {

	@Override
	public Value execute(ServiceContext ctx) {
		try {
			Value value = ctx.getValue("match");
			String match = value == null ? null : value.toString();
			/*
			 * for windows : use dir
			 */
			String[] command = { "cmd.exe", "/c", "dir" };
			ProcessBuilder builder = new ProcessBuilder(command);
			Process process = builder.start();
			/*
			 * for unix use ps-e
			 */
			// Process process = Runtime.getRuntime().exec("ps -e");
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			List<String[]> ps = new ArrayList<String[]>();
			String[] hdr = { "ps" };
			ps.add(hdr); // this is the column header
			Tracer.trace("Going to read output from process");
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				Tracer.trace(line);
				if (line.length() > 0) {
					if (match == null || line.contains(match)) {
						String[] row = { line.replace('\r', ' ').replace('\n',
								' ') };
						ps.add(row);
					}
				}
			}
			int n = ps.size();
			String[][] data = new String[n][];
			for (int i = 0; i < n; i++) {
				data[i] = ps.get(i);
			}
			ValueType[] types = { ValueType.TEXT };
			DataSheet sheet = new MultiRowsSheet(data, types);
			ctx.putDataSheet("ps", sheet);
			return Value.VALUE_TRUE;
		} catch (Exception e) {
			ctx.addInternalMessage(MessageType.ERROR,
					"Error while getting ps -e :\n " + e.getMessage());
			return Value.VALUE_FALSE;
		}
	}
}
