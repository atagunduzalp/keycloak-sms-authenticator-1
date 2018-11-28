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

public class SMSSendVerify {

	private static final Logger logger = Logger.getLogger(SMSSendVerify.class.getPackage().getName());

	public static final String DEFAULT_API_URI = "https://api.authy.com";
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
		data.setAttribute("phone_number", telNum);
		data.setAttribute("country_code", "81"); // JAPAN
		data.setAttribute("via", "sms");
		data.setAttribute("code_length", codeLen);

		return request(METHOD_POST, PHONE_VERIFICATION_API_PATH + "start", data);
	}

	public boolean verifySMS(String telNum, String code) {

		SMSParams data = new SMSParams();
		data.setAttribute("phone_number", telNum);
		data.setAttribute("country_code", "81");
		data.setAttribute("verification_code", code);
		data.setAttribute("code_length", codeLen);

		return request(METHOD_GET, PHONE_VERIFICATION_API_PATH + "check", data);
	}

	private boolean request(String method, String path, SMSParams data) {
		boolean result = false;

		HttpsURLConnection conn;

		try {
			StringBuilder sb = new StringBuilder();

			if (method.equals(METHOD_GET)) {
				sb.append(prepareGet(data));
			}

			URL url = new URL(DEFAULT_API_URI + path + sb.toString());

			if (isProxy) {
				Proxy proxy = new Proxy(Proxy.Type.HTTP,
						new InetSocketAddress(this.proxyUrl, Integer.parseInt(this.proxyPort)));
				conn = (HttpsURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpsURLConnection) url.openConnection();
			}
			conn.setRequestMethod(method);
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);
			conn.setRequestProperty("X-Authy-API-Key", apiKey);

			if (method.equals(METHOD_POST)) {
				writeJson(conn, data);
			}

			final int resStatus = conn.getResponseCode();

			logger.infov("RESPONSE STATUS : {0}", resStatus);

			if (resStatus == HttpURLConnection.HTTP_OK) {
				InputStream in = conn.getInputStream();

				BufferedReader reader = new BufferedReader(new InputStreamReader(in));

				StringBuilder output = new StringBuilder();
				String line;

				while ((line = reader.readLine()) != null) {
					logger.infov("RESPONSE DETAIL : {0}", line);
					output.append(line);
				}
				result = true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	private void writeJson(HttpURLConnection connection, SMSParams data) throws IOException {
		if (data == null)
			return;

		OutputStream os = connection.getOutputStream();
		BufferedWriter output = new BufferedWriter(new OutputStreamWriter(os));
		output.write(data.toJSON());
		output.flush();
		output.close();
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
				System.out.println("Encoding not supported" + e.getMessage());
			}
		}

		return sb.toString();
	}
}