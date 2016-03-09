package com.reverb.api;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import org.json.*;

public class ReverbApi {
	//Constants
	public static final String WEB_URL = "https://reverb.com";
	public static final String API_URL = WEB_URL+"/api/";
	public static final String AUTH_URL = WEB_URL+"/oauth/token";
	public static final String BEARER = "Bearer";
	public static final String XAUTHTOKEN = "X-Auth-Token";
	public static int TIMEOUT = 40000;
	
	//Http Methods
	public static final String GET = HttpRequest.METHOD_GET;
	public static final String POST = HttpRequest.METHOD_POST;
	public static final String PUT = HttpRequest.METHOD_PUT;
	public static final String DELETE = HttpRequest.METHOD_DELETE;
	
	//Http Params
	public static final String QUERY = "query";
	
	//JSON Properties
	public static final String ERROR = "error";
	public static final String MESSAGE = "message";
	
	//Static variables
	private static final Logger log = Logger.getLogger(ReverbApi.class.getName());
	
	public JSONObject apiPublic(String authorization, String uri) throws IOException {
		return apiPublic(authorization, uri, null);
	}
	public JSONObject apiPublic(String authorization, String uri, Map<String,Object> params) throws IOException {
		return apiPublic(authorization, uri, params, GET);
	}
	public JSONObject apiPublic(String authorization, String uri, Map<String,Object> params, String method) throws IOException {
		return api(authorization, null, uri, params, method);
	}
	public JSONObject api(String authorization, String usertoken, String uri, Map<String,Object> params, String method) throws IOException {
		return request(authorization, usertoken, API_URL+uri, params, method);
	}
	public JSONObject request(String authorization, String usertoken, String url, Map<String,Object> params, String method) throws IOException {
		try {
			HttpRequest http = new HttpRequest(url,method,(GET.equals(method) ? null : HttpRequest.CONTENT_FORM),TIMEOUT,true);
			if (authorization != null) {
				//e.g. Authorization: Bearer {APPTOKEN}
				//https://dev.reverb.com/docs/application-only-authorization-client-credentials-flow
				http.setHeader(HttpRequest.HEADER_AUTHORIZATION, "Bearer "+authorization);
			} else if (usertoken != null) {
				//e.g. X-Auth-Token: {PERSONALTOKEN}
				//https://dev.reverb.com/docs/personal-access-tokens
				//https://dev.reverb.com/docs/how-should-i-authenticate
				http.setHeader(XAUTHTOKEN, usertoken);
			}
			JSONObject result;
			try {
				result = new JSONObject(http.send(params));
			} catch (IOException e) {
				log.warning(e.getMessage());
				throw new IOException("Could not get data from Reverb.com");
			}
			checkError(result);
			return result;
		} catch (JSONException e) {
			//Graph API returns false for not found/not permissioned. JSON parsing fails
			return null;
		}
	}
	public static void checkError(JSONObject result) throws IOException {
		//Incorrect token:
		//{"error":"You must log in to access this endpoint"}
		//No token for user function
		//{"message":"Please log in to see your wanted list.","details":"Invalid token. Please re-authenticate to obtain X-Auth-Token header.","_links":{"email_auth":{"href":"/api/auth/email"},"fb_auth":{"href":"/api/auth/facebook"}}}
		String error = result.optString(ERROR);
		error = blank(error) ? result.optString(MESSAGE) : error;
		if (!blank(error)) {
			throw new IOException(error);
		}
	}
	public JSONObject get(String authorization) throws IOException {
		return apiPublic(authorization, "");
	}
	public String getAuthorization(String key, String secret) throws IOException {
		//https://dev.reverb.com/docs/application-only-authorization-client-credentials-flow
		//curl -i -XPOST 'https://reverb.com/oauth/token' -d 'grant_type=client_credentials&client_id=foo&client_secret=bar'
		//{"access_token":"05bacc5f74e43e12498fb17b07375a1f0ce3304ef843291ae691d772e43f368a","token_type":"bearer","scope":"read","created_at":1457474389}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("grant_type", "client_credentials");
		params.put("client_id", key);
		params.put("client_secret", secret);
		return request(null, null, AUTH_URL, params, POST).optString("access_token");
	}
	public JSONObject getComparisonShopping(String authorization, String query) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		if (!blank(query)) {
			params.put(QUERY, query); 
		}
		return apiPublic(authorization, "comparison_shopping", params);
	}
	
	//Static methods
	public static boolean blank(String s) {
		return s == null || s.isEmpty();
	}
	public static void echo(Object obj) throws IOException {
		System.out.println(obj);
	}
	public static void main(String[] args) throws IOException {
		ReverbApi reverb = new ReverbApi();
		String token = args.length > 0 ? args[0] : null;
		token = "${reverb.token}".equals(token) ? null : token;
		echo("Using token: "+token);
		echo(reverb.get(token));
	}
}
