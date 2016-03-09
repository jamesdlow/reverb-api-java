package com.reverb.api;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import org.json.*;

public class ReverbApi {
	//Constants
	public static final String WEB_URL = "https://reverb.com";
	public static final String API_URL = WEB_URL+"/api/";
	public static final String BEARER = "Bearer";
	public static int TIMEOUT = 40000;
	
	//Static variables
	private static final Logger log = Logger.getLogger(ReverbApi.class.getName());
	
	public static JSONObject api(String uri) throws IOException {
		return api(uri, (Map<String,Object>) null);
	}
	public static JSONObject api(String uri, Map<String,Object> params) throws IOException {
		return api(uri, params, HttpRequest.METHOD_GET);
	}
	public static JSONObject api(String uri, Map<String,Object> params, String method) throws IOException {
		return api(null, uri, params, method);
	}
	public static JSONObject api(String token, String uri) throws IOException {
		return api(token, uri, null);
	}
	public static JSONObject api(String token, String uri, Map<String,Object> params) throws IOException {
		return api(token, uri, params, HttpRequest.METHOD_GET);
	}
	public static JSONObject api(String token, String uri, Map<String,Object> params, String method) throws IOException {
		try {
			HttpRequest http = new HttpRequest(API_URL+uri,method,(HttpRequest.METHOD_GET.equals(method) ? null : HttpRequest.CONTENT_FORM),TIMEOUT,true);
			if (token != null) {
				//e.g. Authorization: Bearer 774c5112345abcd3f32e662e885e043672f6c5d36e14c1d98730170cea3
				http.setHeader(HttpRequest.HEADER_AUTHORIZATION, BEARER+' '+token);
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
		//{"error":{"message":"A user access token is required to request this resource.","code":102,"type":"OAuthException"}}
		//JSONObject error = result.optJSONObject(Facebook.ERROR);
		//if (error != null) {
		//	throw new FacebookException(result);
		//}
	}
	public static JSONObject get() throws IOException {
		return api("");
	}
	public static JSONObject get(String token) throws IOException {
		return api(token, "");
	}
	public static void main(String[] args) throws IOException {
		System.out.println(get());
	}
}
