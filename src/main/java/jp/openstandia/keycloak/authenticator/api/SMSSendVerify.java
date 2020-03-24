package jp.openstandia.keycloak.authenticator.api;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import org.jboss.logging.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SMSSendVerify {

	private static final Logger logger = Logger.getLogger(SMSSendVerify.class.getPackage().getName());

	private static String ID;

//	public static final String DEFAULT_API_URI = "https://api.authy.com";
	public static final String DEFAULT_API_URI = "https://svc.vkk.io/otp-v1-dev/generate";
	public static final String VERIFY_API_URI = "https://svc.vkk.io/otp-v1-dev/verify";

	public static final String PHONE_VERIFICATION_API_PATH = "/protected/json/phones/verification/";

	public static final String METHOD_POST = "POST";
	public static final String METHOD_GET = "GET";

	private final boolean isProxy;
	private final String proxyUrl;
	private final String proxyPort;
	private final String apiKey;
	private final String codeLen;

	public SMSSendVerify(String apiKey, String isProxy, String proxyUrl, String proxyPort, String codeLen) {
		this.apiKey = apiKey;
		this.isProxy = Boolean.parseBoolean(isProxy);
		this.proxyUrl = proxyUrl;
		this.proxyPort = proxyPort;
		this.codeLen = codeLen;
	}

	public boolean sendSMS(String telNum) {

		SMSParams data = new SMSParams();
//		data.setAttribute("phone_number", telNum);
//		data.setAttribute("country_code", "90"); // JAPAN
//		data.setAttribute("via", "sms"); // SMS
//		data.setAttribute("code_length", codeLen);

		data.setAttribute("type", "mail");
		data.setAttribute("verificationMode", "strict");
		data.setAttribute("recipients", "mustafa.dumlupinar@iys.org.tr");
		data.setAttribute("originService", "hs-preapp/verify-user");

		logger.info("before request..." + data.toJSON());
		return request(METHOD_POST, PHONE_VERIFICATION_API_PATH + "start", data);
	}

	public boolean verifySMS(String telNum, String code) {

		SMSParams data = new SMSParams();

//		data.setAttribute("phone_number", telNum);
//		data.setAttribute("country_code", "81");
//		data.setAttribute("verification_code", code);
//		data.setAttribute("code_length", codeLen);

		data.setAttribute("code", code);
		logger.info("ID: " + ID);
		data.setAttribute("id", ID);
		data.setAttribute("type", "mail");
		data.setAttribute("verificationMode", "strict");
		data.setAttribute("recipients", "mustafa.dumlupinar@iys.org.tr");
		data.setAttribute("originService", "hs-preapp/verify-user");

		return request(METHOD_POST, PHONE_VERIFICATION_API_PATH + "check", data);
	}

	private boolean request(String method, String path, SMSParams data) {
		boolean result = false;
		logger.info("request method.");
		HttpsURLConnection conn;
		InputStream in = null;
		BufferedReader reader = null;
		try {
			StringBuilder sb = new StringBuilder();

			if (method.equals(METHOD_GET)) {
				sb.append(prepareGet(data));
			}

//			URL url = new URL(DEFAULT_API_URI + path + sb.toString());
			logger.info("sb to string: " + sb.toString());

			URL url = new URL(DEFAULT_API_URI + sb.toString());
			if (path.contains("check")) {
				url = new URL(VERIFY_API_URI + sb.toString());
			}

			logger.info("url: " + url);

			if (isProxy) {
				Proxy proxy = new Proxy(Proxy.Type.HTTP,
						new InetSocketAddress(this.proxyUrl, Integer.parseInt(this.proxyPort)));
				conn = (HttpsURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpsURLConnection) url.openConnection();
			}
			logger.info("api-key: " + apiKey);
			conn.setRequestMethod(method);
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);
//			conn.setRequestProperty("X-Authy-API-Key", apiKey); // API-KEY

			if (method.equals(METHOD_POST)) {
				writeJson(conn, data);
			}

			final int resStatus = conn.getResponseCode();
			logger.infov("RESPONSE STATUS : {0}", resStatus);

			if (resStatus == HttpURLConnection.HTTP_OK) {
				in = conn.getInputStream();
				reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

				if (!path.contains("check")) {
					String line;
					while ((line = reader.readLine()) != null) {
						logger.info("reader: " + reader.toString());
						logger.infov("RESPONSE DETAIL : {0}", line);
						JsonObject parse = new JsonParser().parse(line).getAsJsonObject();
						JsonObject payloadObject = parse.get("payload").getAsJsonObject();
						logger.info("payload object: " + payloadObject);
						ID = payloadObject.get("id").getAsString();
					}
				}
				result = true;
			}

		} catch (IOException e) {
			logger.error(e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}

		return result;
	}

	private void writeJson(HttpURLConnection connection, SMSParams data) {
		if (data == null) {
			return;
		}

		OutputStream os = null;
		BufferedWriter output = null;
		try {
			os = connection.getOutputStream();
			output = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			String dataJson = data.toJSON();
			logger.info("data json: " + dataJson);
			output.write(dataJson);
			output.flush();
			output.close();
		} catch (IOException e) {
			logger.error(e);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}

	}

	public String prepareGet(SMSParams data) {

		if (data == null)
			return "";

		StringBuilder sb = new StringBuilder("?");
		Map<String, String> params = data.toMap();

		boolean first = true;

		for (Entry<String, String> s : params.entrySet()) {

			if (first) {
				first = false;
			} else {
				sb.append('&');
			}
			try {
				sb.append(URLEncoder.encode(s.getKey(), "UTF-8")).append("=")
						.append(URLEncoder.encode(s.getValue(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				logger.error("Encoding not supported" + e.getMessage());
			}
		}

		return sb.toString();
	}
}
