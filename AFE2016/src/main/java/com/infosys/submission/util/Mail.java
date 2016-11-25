package com.infosys.submission.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.simplity.json.JSONException;
import org.simplity.json.JSONObject;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class Mail implements LogicInterface {

	public static Configuration cfg;

	public Value execute(ServiceContext ctx) {

		String urlstring = ctx.getTextValue("urlString");
		String application = ctx.getTextValue("application");
		String apikey = ctx.getTextValue("apikey");
		String submitterId = ctx.getTextValue("submitterMail");
		String sponsormailid = ctx.getTextValue("sponsorMail");
		String nomination = ctx.getTextValue("nomination");
		String status = ctx.getTextValue("status");

		String toIds = null;
		String ccIds = null;
		String bccIds = null;
		String subject = null;
		String content = null;

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("submitter", submitterId);
		parameters.put("sponsor", sponsormailid);
		parameters.put("title", nomination);
		parameters.put("status", status);

		Template template = null;
		Writer outputStringWriter = new StringWriter();
		try {
			switch (parameters.get("status").toString()) {
			case "Submitted":
				template = cfg.getTemplate("submit.ftlh");
				toIds = sponsormailid;
				ccIds = submitterId;
				break;
			case "Approved":
				template = cfg.getTemplate("approve.ftlh");
				toIds = submitterId;
				ccIds = sponsormailid;
				break;
			case "Rejected":
				template = cfg.getTemplate("reject.ftlh");
				toIds = submitterId;
				ccIds = sponsormailid;
				break;
			}
			template.process(parameters, outputStringWriter);
			subject = "AFE application is " + status;
			content = outputStringWriter.toString();
			outputStringWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TemplateException e) {
			e.printStackTrace();
		}

		sendMail(urlstring, application, apikey, toIds, ccIds, bccIds, subject, content);
		return Value.newBooleanValue(true);
	}

	private void sendMail(String urlstring, String application, String apikey, String toIds, String ccIds,
			String bccIds, String subject, String content) {
		try {
			URL url = new URL(urlstring);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("application", application);
			conn.setRequestProperty("apikey", apikey);
			conn.setRequestProperty("_serviceName", "smtp.mailer");
			conn.setDoOutput(true);

			JSONObject json = new JSONObject();
			try {
				json.put("toIds", toIds);
				json.put("ccIds", ccIds);
				json.put("bccIds", bccIds);
				json.put("subject", subject);
				json.put("content", content);
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
