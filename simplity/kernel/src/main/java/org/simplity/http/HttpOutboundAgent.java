/*
 * Copyright (c) 2017 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.http;

import java.net.HttpURLConnection;

import org.simplity.gateway.JsonRespWriter;
import org.simplity.gateway.OutboundAgent;
import org.simplity.service.InputData;
import org.simplity.service.OutputData;
import org.simplity.service.ServiceContext;

/**
 * make an http request for an external service
 *
 * @author simplity.org
 *
 */
public class HttpOutboundAgent  implements OutboundAgent{

	private static final String DEFULT_METHOD = "POST";
	private String appUrl;

	/* (non-Javadoc)
	 * @see org.simplity.gateway.OutboundAgent#server(java.lang.String, org.simplity.service.ServiceContext, org.simplity.service.OutputData, org.simplity.service.InputData)
	 */
	@Override
	public boolean server(String serviceId, ServiceContext ctx, OutputData dataTobeSent, InputData dataToBeReceived) {
		String payload = null;
		if(dataTobeSent != null){
			JsonRespWriter writer = new JsonRespWriter();
			dataTobeSent.write(writer, ctx);
			payload = writer.getFinalResponseText();
		}
		HttpURLConnection con = null;
		return false;
	}
}
