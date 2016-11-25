package com.infosys.submission.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.simplity.json.JSONException;
import org.simplity.json.JSONObject;


public class NetClientGet {

	public static void main(String[] args) {

		try {

			URL url = new URL("http://BLRKECIC03:8080/SMTPService/a._s");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("application", "AFE2016");
			conn.setRequestProperty("apikey", "76d15092-19eb-4072-ac15-7fca5e8b25c1");
			conn.setRequestProperty("_serviceName", "smtp.mailer");
			conn.setDoOutput(true);

			JSONObject json = new JSONObject();
			try {
				json.put("toIds","Shalinireddy_B@infosys.com");
				json.put("ccIds","");
				json.put("bccIds","");
				json.put("subject","Hello World");
				json.put("content","<html><body><div><p>Dear ${submitter},</p><p>&nbsp;</p><p>Your nomination titled  for the AFE 2016 is approved by ${sponsor}.</p><p>&nbsp;</p><p>Regards,</p><p>AFE Team</p></div></body></html>");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			OutputStream wr = conn.getOutputStream();
			wr.write(json.toString().getBytes("UTF-8"));
			wr.close();
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				System.out.println(output);
			}

			conn.disconnect();

		} catch (MalformedURLException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

}