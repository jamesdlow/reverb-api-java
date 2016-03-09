package com.reverb.api;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class HttpRequest {
	//Content types
	public static final String CONTENT_JSON = "application/json";
	public static final String CONTENT_FORM = "application/x-www-form-urlencoded"; //Google app engine defaults to this anwyay
	public static final String CONTENT_MULTI = "multipart/form-data";
	
	//Methods
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_PUT = "PUT";
	public static final String METHOD_DELETE = "DELETE";
	
	//Headers
	public static final String HEADER_CONTENTTYPE = "Content-Type";
	public static final String HEADER_CONTENTLENGTH = "Content-Length";
	public static final String HEADER_CONTENTDISPOSITION = "Content-Disposition";
	public static final String HEADER_AUTHORIZATION = "Authorization";
	public static final String HEADER_USERAGENT = "User-Agent";
	
	//Other
	public static final String LINE = "\r\n";
	public static final String HYPHENS = "--";
	public static final String UTF8 = "UTF-8";
	public static final String FILENAME = "filename";
	
	protected Map<String,Object> params = new HashMap<String,Object>();
	protected Map<String,String> headers = new HashMap<String,String>();
	protected Map<String,List<String>> responseheaders;
	protected String baseurl;
	protected String method;
	protected int timeout;
	protected boolean contentonerror;
	protected HttpURLConnection conn;
	protected String charset = UTF8;
	
	public static class HttpFile {
		private byte[] data;
		private String filename;
		private String mimetype;
		
		public HttpFile(File file) throws IOException {
			this(file, getContentType(file));
		}
		public HttpFile(File file, String mimetype) throws IOException {
			this(toByteArray(file), file.getName(), mimetype);
		}
		public HttpFile(byte[] data, String filename) throws IOException {
			this(data, filename, getContentType(filename));
		}
		public HttpFile(byte[] data, String filename, String mimetype) {
			this.data = data;
			this.filename = filename;
			this.mimetype = mimetype;
		}
		
		public byte[] getData() {
			return data;
		}
		public String getFilename() {
			return filename;
		}
		public String getMimetype() {
			return mimetype;
		}
		public static String getContentType(String path) throws IOException {
			return Files.probeContentType((new File(path)).toPath());
		}
		public static String getContentType(File file) throws IOException {
			return Files.probeContentType(file.toPath());
		}
	}
	
	public HttpRequest(String baseurl) {
		this(baseurl,METHOD_GET);
	}
	public HttpRequest(String baseurl, String method) {
		this(baseurl,method,null);
	}
	public HttpRequest(String baseurl, String method, String contenttype) {
		this(baseurl,method,contenttype,-1);
	}
	public HttpRequest(String baseurl, String method, String contenttype, int timeout) {
		this(baseurl,method,contenttype,timeout, false);
	}
	public HttpRequest(String baseurl, String method, String contenttype, int timeout, boolean contentonerror) {
		this.baseurl = baseurl;
		this.method = method;
		setContentType(contenttype);
		this.timeout = timeout;
		this.contentonerror = contentonerror;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	public void setContentType(String contenttype) {
		setHeader(HEADER_CONTENTTYPE, contenttype);
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public void setContentOnError(boolean contentonerror) {
		this.contentonerror = contentonerror;
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}
	public void setParams(Map<String,Object> params) {
		this.params = params;
	}
	public void addParams(String name, String value){
		params.put(name, value);
	}
	public byte[] encodeParams() throws IOException {
		return encodeParams(params);
	}
	public static boolean hasFile(Map<String,Object> params) {
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof HttpFile || value instanceof File) {
				return true;
			} else if (value instanceof Iterable) {
				Iterable list = (Iterable) value;
				for (Object o : list) {
					if (o instanceof HttpFile) {
						return true;
					}
				}
			}
		}
		return false;
	}
	public byte[] encodeParams(Map<String,Object> params) throws IOException {
		if (params != null) {
			if (hasFile(params)) {
				byte[] result = null;
				//Create multipart
				String boundary =  "*****"+Long.toString(System.currentTimeMillis())+"*****";
				method = METHOD_GET.equals(method) ? METHOD_POST : method; //Force post
				setContentType(CONTENT_MULTI+"; boundary="+boundary);
				
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				for (Map.Entry<String, Object> param : params.entrySet()) {
					Object value = param.getValue();
					if (value instanceof Iterable) {
						Iterable list = (Iterable) value;
						for (Object o : list) {
							write(os, boundary, param.getKey(), o);
						}
					} else {
						write(os, boundary, param.getKey(), value);
					}
				}
				write(os, HYPHENS + boundary + HYPHENS);
				os.flush();
				return os.toByteArray();
				
			} else {
				if(!METHOD_GET.equals(method)) {
					setContentType(CONTENT_FORM);
				}
				return encode(params).getBytes();
			}
		} else {
			return null;
		}
	}
	public static String encode(Map<String,Object> params) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, Object> param : params.entrySet()) {
			Object value = param.getValue();
			if (builder.length() > 0){
				builder.append('&');
			}
			if (value instanceof Iterable) {
				Iterable list = (Iterable) value;
				for (Object o : list) {
					builder.append(encode(param.getKey(), value.toString()));
				}
			} else {
				builder.append(encode(param.getKey(), value.toString()));
			}
		}
		return builder.toString();
	}
	public static String encode(String key, String value) {
		return encode(key)+'='+encode(value);
	}
	public static String encode(String value) {
		try {
			return URLEncoder.encode(value, UTF8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	public Map<String, List<String>> getResponseHeaders() {
		return responseheaders;
	}
	public String getResponseHeader(String key) {
		List<String> headers = responseheaders.get(key);
		if (headers != null && headers.size() > 0) {
			return headers.get(0);
		} else {
			return null;
		}
	}
	public String getResponseContentType() {
		return getResponseHeader(HEADER_CONTENTTYPE);
	}
	public String getResponseFilename() {
		String disposition = getResponseHeader(HEADER_CONTENTDISPOSITION);
		return disposition == null || disposition.length() == 0 ? null : getFilename(disposition);
	}
	public static String getFilename(String disposition) {
		return getEquals(disposition, FILENAME);
	}
	private static String getEquals(String part) {
		//This mighe need to escape other characters like = or at least filenames with a quotation in them, but this is rare
		//https://en.wikipedia.org/wiki/Quoted-printable
		String sub[] = part.split("=",2);
		if (sub.length > 1) {
			String equals = sub[1];
			if (equals.startsWith("\"")) {
				return equals.substring(1, equals.length()-1);
			}
			return equals;
		}
		return null;
	}
	private static String getEquals(String mime, String key) {
		String parts[] = getParts(mime);
		for (String part : parts) { //We might be able to hard code this to always being the second part
			if (part.startsWith(key+'=')) {
				return getEquals(part);
			}
		}
		return null;
	}
	public static String[] getParts(String mime) {
		return mime.split("; *",2); //For now only handle 2
	}
	public String send() throws IOException{
		return send(params);
	}
	public String send(Map<String,Object> params) throws IOException{
		return send(encodeParams(params));
	}
	public String send(String content) throws IOException{
		return send(content.getBytes());
	}
	public String send(byte[] content) throws IOException {
		return new String(sendRaw(content), charset);
	}
	public byte[] sendRaw() throws IOException{
		return sendRaw(params);
	}
	public byte[] sendRaw(Map<String,Object> params) throws IOException{
		return sendRaw(encodeParams(params));
	}
	public byte[] sendRaw(String content) throws IOException{
		return sendRaw(content.getBytes());
	}
	public byte[] sendRaw(byte[] content) throws IOException {
		OutputStream os = null;
		try {
			if(METHOD_GET.equals(method)){
				baseurl = baseurl + (content !=null && content.length > 0 ? (baseurl.indexOf('?') < 0 ? '?' : '&') + new String(content) : "");
			}
			ReverbApi.echo(baseurl);
			conn = (HttpURLConnection) new URL(baseurl).openConnection();
			conn.setRequestMethod(method);
			for (Map.Entry<String, String> header : headers.entrySet()) {
				conn.setRequestProperty(header.getKey(), header.getValue());
			}
			
			conn.setRequestProperty(HEADER_CONTENTLENGTH, ""+(content == null ? 0 : content.length));
			setTimeout(conn);
			/*if (content != null) {
				conn.setRequestProperty(HEADER_CONTENTLENGTH, ""+content.length);
			}*/
			
			//Post
			if(!METHOD_GET.equals(method) && content != null && content.length > 0) {
				//I think we need only need to do this if we have content?
				conn.setDoOutput(true);
				os = conn.getOutputStream();
				os.write(content);
				os.flush();
			}
			return read(conn);
		} finally {
			if (os != null) { os.close(); }
		}
	}
	public HttpURLConnection getConnection() {
		return conn;
	}
	public void setHeader(String key, String value) {
		if (value != null) {
			headers.put(key, value);
		}
	}
	public void setUserAgent(String value) {
		setHeader(HEADER_USERAGENT, value);
	}
	public String getHeader(String key) {
		return getConnection().getHeaderField(key);
	}
	public String getContentType() {
		return getHeader(HEADER_CONTENTTYPE);
	}
	public String send(Map<String,Object> params, String filename, String filefield, String filemime, byte[] file) throws IOException {
		return new String(sendRaw(params, filename, filefield, filemime, file), charset);
	}
	public byte[] sendRaw(Map<String,Object> params, String filename, String filefield, String filemime, byte[] file) throws IOException {
		params.put(filefield, new HttpFile(file, filename, filemime));
		return sendRaw(params);
	}
	/*public byte[] sendRaw(Map<String,String> params, String filename, String filefield, String filemime, byte[] file) throws IOException {
		OutputStream os = null;
		try {
			//http://www.androidsnippets.com/multipart-http-requests
			String boundary =  "*****"+Long.toString(System.currentTimeMillis())+"*****";
			
			conn = (HttpURLConnection) new URL(baseurl).openConnection();
			conn.setRequestMethod(METHOD_POST);
			conn.setRequestProperty(HEADER_CONTENTTYPE, CONTENT_MULTI+"; boundary="+boundary);
			setTimeout(conn);
			
			//File
			conn.setDoOutput(true);
			os = conn.getOutputStream();
			write(os, HYPHENS + boundary);
			write(os, "Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + filename +"\"");
			write(os, "Content-Type: " + filemime);
			write(os, "Content-Transfer-Encoding: binary");
			write(os);
			os.write(file);
			write(os);
			
			//Post
			for (Map.Entry<String, String> param : params.entrySet()) {
				write(os, HYPHENS + boundary);
				write(os, "Content-Disposition: form-data; name=\"" + param.getKey() + "\"");
				write(os, "Content-Type: text/plain");
				write(os);
				write(os, param.getValue());
			}
			write(os, HYPHENS + boundary + HYPHENS);
			os.flush();
			return read(conn);
		} finally {
			if (os != null) { os.close(); }
		}
	}*/
	private void setTimeout(HttpURLConnection conn) {
		if (timeout >= 0) {
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
		}
	}
	private void write(OutputStream os) throws IOException {
		write(os,"");
	}
	private void write(OutputStream os, String s) throws IOException {
		os.write((s+LINE).getBytes());
	}
	private void write(OutputStream os, String boundary, String key, Object value) throws IOException {
		//http://www.androidsnippets.com/multipart-http-requests
		if (value instanceof HttpFile || value instanceof File) {
			HttpFile file = value instanceof File ? new HttpFile((File) value) : (HttpFile) value;
			write(os, HYPHENS + boundary);
			write(os, "Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + file.getFilename() +"\"");
			write(os, "Content-Type: " + file.getMimetype());
			write(os, "Content-Transfer-Encoding: binary");
			write(os);
			os.write(file.getData()); write(os); //Add line feed after writing raw data
		} else {
			write(os, HYPHENS + boundary);
			write(os, "Content-Disposition: form-data; name=\"" + key + "\"");
			write(os, "Content-Type: text/plain");
			write(os);
			write(os, value.toString()); //Auto adds line feed
		}
	}
	private byte[] read(HttpURLConnection conn) throws IOException {
		InputStream is  = null;
		try {
			int code = conn.getResponseCode();
			is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
			if (code >= 400 && !contentonerror) {
				throw new IOException(new String(toByteArray(is)));
			//TODO: Handle redirect
			//} else if (code >= 300) {
			} else {
				responseheaders = conn.getHeaderFields();
				return toByteArray(is);
			}
		} finally {
			if (is != null) { is.close(); }
		}
	}
	public static byte[] toByteArray(File file) throws IOException {
		return toByteArray(new FileInputStream(file));
	}
	public static byte[] toByteArray(InputStream in) throws IOException {
		int c;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while ((c = in.read()) != -1) {
			out.write(c);
		}
		out.flush();
		return out.toByteArray();
	}
}
