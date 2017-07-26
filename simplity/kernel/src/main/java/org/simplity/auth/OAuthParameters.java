package org.simplity.auth;

public class OAuthParameters {
	String checkTokenURL;
	public String getCheckTokenURL() {
		return checkTokenURL;
	}
	public String getClientId() {
		return clientId;
	}
	public String getClientSecret() {
		return clientSecret;
	}
	String clientId;
	String clientSecret;
}
