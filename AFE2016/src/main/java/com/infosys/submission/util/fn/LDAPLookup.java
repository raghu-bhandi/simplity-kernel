package com.infosys.submission.util.fn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.fn.AbstractFunction;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

public class LDAPLookup extends AbstractFunction{

	@Override
	public ValueType getReturnType() {
		return ValueType.TEXT;
	}

	@Override
	public ValueType[] getArgDataTypes() {
		return new ValueType[] {ValueType.TEXT,ValueType.TEXT,ValueType.TEXT};
	}

	@Override
	public Value execute(Value[] arguments, FieldsInterface data) {
		String urlString = arguments[0].toText();
		String mailidPart = arguments[1].toText();
		String param = arguments[2].toText();
		try {
			URL url = new URL(urlString+mailidPart);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			if (conn.getResponseCode() != 200) {
				return null;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuffer output = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {
				output.append(line);
			}
			return Value.newTextValue(new JSONObject(output.toString()).getJSONArray("employees").getJSONObject(0).getString(param));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
