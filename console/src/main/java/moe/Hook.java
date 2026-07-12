package moe;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.io.ByteArrayOutputStream;
import org.json.JSONObject;
import java.io.InputStream;
import org.json.JSONException;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import java.io.OutputStream;
import java.util.HashMap;
public class Hook implements IHook {
	private HookCallback callback;
	@Override
	public void onCreate(HookCallback callback) {
		this.callback = callback;
	}

	public WebResourceResponse hook(WebResourceRequest request) throws IOException {
		if (request.getUrl().toString().startsWith("https://pan.quark.cn/1/clouddrive/task")) {
			HttpURLConnection huc = (HttpURLConnection) new URL(request.getUrl().toString()).openConnection();
			if (huc instanceof HttpsURLConnection) {
				HttpsURLConnection hsuc = (HttpsURLConnection) huc;
				hsuc.setSSLSocketFactory(getSSLSocketFactory());
			}
			Iterator<Map.Entry<String, String>> i = request.getRequestHeaders().entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<String, String> entry = i.next();
				huc.setRequestProperty(entry.getKey(), entry.getValue());
			}
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				int len = -1;
				byte[] buff = new byte[256];
				InputStream in = huc.getInputStream();
				while ((len = in.read(buff)) != -1)
					baos.write(buff, 0, len);
				baos.flush();
				try {
					JSONObject jo = new JSONObject(baos.toString());
					if (jo.has("data")) {
						jo = jo.getJSONObject("data");
						if (jo.has("save_as")) {
							jo = jo.getJSONObject("save_as");
							if (jo.has("save_as_top_fids")) {
								JSONArray fids = jo.getJSONArray("save_as_top_fids");
								JSONObject fid = new JSONObject();
								fid.put("fids", fids);
								download(fid, request.getRequestHeaders());
							}
						}
					}
				} catch (JSONException e) {
				}
				return new WebResourceResponse(null, null, new ByteArrayInputStream(baos.toByteArray()));
			}
		}
		return null;
	}
	private void download(JSONObject data, Map<String, String> header) throws IOException {
		HttpURLConnection huc = (HttpURLConnection) new URL(
			"https://drive-pc.quark.cn/1/clouddrive/file/download?pr=ucpro&fr=pc&uc_param_str=").openConnection();
		if (huc instanceof HttpsURLConnection) {
			HttpsURLConnection hsuc = (HttpsURLConnection) huc;
			hsuc.setSSLSocketFactory(getSSLSocketFactory());
		}
		Iterator<Map.Entry<String, String>> i = header.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<String, String> entry = i.next();
			huc.setRequestProperty(entry.getKey(), entry.getValue());
		}
		huc.setRequestMethod("POST");
		huc.setRequestProperty("Content-Type", "application/json");
		huc.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) quark-cloud-drive/2.5.20 Chrome/100.0.4896.160 Electron/18.3.5.4-b478491100 Safari/537.36 Channel/pckk_other_ch");
		byte[] body = data.toString().getBytes();
		huc.setRequestProperty("Content-Length", String.valueOf(body.length));
		OutputStream out = huc.getOutputStream();
		out.write(body);
		out.flush();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int len = -1;
		byte[] buff = new byte[256];
		InputStream in = huc.getInputStream();
		while ((len = in.read(buff)) != -1)
			baos.write(buff, 0, len);
		baos.flush();
		try {
			JSONObject jo = new JSONObject(baos.toString());
			if (jo.has("data")) {
				jo = jo.getJSONArray("data").getJSONObject(0);
				if (jo.has("download_url")) {
					String download_url = jo.getString("download_url");
					String name = jo.getString("file_name");
					if (callback != null) {
						Map<String, String> headers = new HashMap<>();
						headers.put("User-Agent",
								"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) quark-cloud-drive/2.5.20 Chrome/100.0.4896.160 Electron/18.3.5.4-b478491100 Safari/537.36 Channel/pckk_other_ch");
						headers.put("Cookie", header.get("Cookie"));
						callback.download(download_url, name, headers);
					}
				}
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		baos.close();
	}
	private static SSLSocketFactory mSSLSocketFactory;
	public static SSLSocketFactory getSSLSocketFactory() {
		if (mSSLSocketFactory == null)
			synchronized (SSLSocketFactory.class) {
				if (mSSLSocketFactory == null)
					try {
						SSLContext sslc = SSLContext.getInstance("TLS");
						sslc.init(null, new TrustManager[]{new X509TrustManager() {

							@Override
							public void checkClientTrusted(X509Certificate[] p1, String p2)
									throws CertificateException {
								// TODO: Implement this method
							}

							@Override
							public void checkServerTrusted(X509Certificate[] p1, String p2)
									throws CertificateException {
								// TODO: Implement this method
							}

							@Override
							public X509Certificate[] getAcceptedIssuers() {
								// TODO: Implement this method
								return new X509Certificate[0];
							}
						}}, new SecureRandom());
						mSSLSocketFactory = (sslc.getSocketFactory());
					} catch (Exception e) {
					}
			}
		return mSSLSocketFactory;
	}

}

