package de.schildbach.wallet.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import android.content.res.AssetManager;
import de.schildbach.wallet.Constants;

public abstract class HttpThread extends Thread {

	protected final AssetManager assets;
	protected final String url;
	@CheckForNull
	protected final String userAgent;

	public HttpThread(final AssetManager assets, @Nonnull final String url, @Nullable final String userAgent) {
		this.assets = assets;
		this.url = url;
		this.userAgent = userAgent;
	}

	protected HttpURLConnection getConnection() throws IOException,
			MalformedURLException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, KeyManagementException {
				HttpURLConnection connection;
				connection = (HttpURLConnection) new URL(url).openConnection();
			
				if (connection instanceof HttpsURLConnection && assets != null)
				{
					final InputStream keystoreInputStream = assets.open("ssl-keystore");
			
					final KeyStore keystore = KeyStore.getInstance("BKS");
					keystore.load(keystoreInputStream, "password".toCharArray());
					keystoreInputStream.close();
			
					final TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
					tmf.init(keystore);
			
					final SSLContext sslContext = SSLContext.getInstance("TLS");
					sslContext.init(null, tmf.getTrustManagers(), null);
			
					((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
				}
			
				connection.setInstanceFollowRedirects(false);
				connection.setConnectTimeout(Constants.HTTP_TIMEOUT_MS);
				connection.setReadTimeout(Constants.HTTP_TIMEOUT_MS);
				connection.setRequestProperty("Accept-Charset", "utf-8");
				if (userAgent != null)
					connection.addRequestProperty("User-Agent", userAgent);
				return connection;
			}

	protected abstract void handleException(@Nonnull Exception x);
}