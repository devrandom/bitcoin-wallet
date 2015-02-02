package de.schildbach.wallet.ui;

import co.cryptocorp.oracle.api.CryptocorpTransactionSigner;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;

import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.MarriedKeyChain;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.util.HttpJsonPostThread;
import de.schildbach.wallet_test.R;

public class MarryActivity extends AbstractWalletActivity {
	private static final int REQUEST_CODE_SCAN = 0;

	private WalletApplication application;
	private Wallet wallet;

	private TextView textView;

	private SocketIO socket;

	private DeterministicKeyChain keychain;
	private String channelId;

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		application = getWalletApplication();
		wallet = application.getWallet();

		setContentView(R.layout.marry_content);
		textView = (TextView)findViewById(R.id.marry_text);
		textView.setMovementMethod(new ScrollingMovementMethod());
		keychain = null;

		handleScan();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		if (socket != null) {
			socket.disconnect();
			socket = null;
		}
		super.onDestroy();
	}

	private void handleMarry(URL rendezvousUrl, List<String> keyStrings, String leadKey) {
		wallet.setKeychainLookaheadSize(10); // for now

		List<DeterministicKey> followingAccountKeys = Lists.newArrayList();
		String myKey = keychain.getWatchingKey().serializePubB58(wallet.getNetworkParameters());
		List<String> accountKeys = Lists.newArrayList(leadKey, myKey);

		for (String keyString : keyStrings) {
			if (!myKey.equals(keyString)) {
				log.info("Adding key {}", keyString);
				followingAccountKeys.add(DeterministicKey.deserializeB58(keyString, wallet.getNetworkParameters()));
			}
		}

		wallet.addTransactionSigner(new CryptocorpTransactionSigner(rendezvousUrl, channelId, myKey, accountKeys));

        MarriedKeyChain marriedChain = MarriedKeyChain.builder().seed(keychain.getSeed()).followingKeys(followingAccountKeys).build();
		wallet.addAndActivateHDChain(marriedChain);
		log.info("New chain mnemonic {}", wallet.getKeyChainSeed());
		wallet.freshAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);
		wallet.freshAddress(KeyChain.KeyPurpose.CHANGE);
		finish();
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.marry_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.marry_scan:
				handleScan();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void handleScan()
	{
		startActivityForResult(new Intent(this, ScanActivity.class), REQUEST_CODE_SCAN);
	}

	public void onActivityResult(final int requestCode, final int resultCode, final Intent intent)
	{
		if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK)
		{
			final String input = intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
			log.warn(input);
			if (input.startsWith("bitcoin:"))
			{
				try
				{
					URI uri = new URI(input);
					Map<String, String> nameValueMap = parseUriQuery(uri);

					handleScanResult(nameValueMap);
				}
				catch (URISyntaxException x)
				{
					log.info("got invalid bitcoin uri: '" + input + "'", x);

					error(R.string.input_parser_invalid_bitcoin_uri, input);
				}
			}
		}
	}

	private Map<String, String> parseUriQuery(URI uri) {
		Map<String, String> nameValueMap = Maps.newTreeMap();
		String[] parts = uri.getSchemeSpecificPart().split("\\?", 2);
		if (parts.length != 2)
			return nameValueMap;
		String[] nameValuePairTokens = parts[1].split("&");
		for (int i = 0 ; i < nameValuePairTokens.length ; i++) {
			String[] nameValue = nameValuePairTokens[i].split("=", 2);
			String value = nameValue.length > 1 ? URLDecoder.decode(nameValue[1]) : "";
			nameValueMap.put(nameValue[0], value);
		}
		return nameValueMap;
	}
	
	private void appendMessage(final String s) {
		runOnUiThread(new Runnable() {
			public void run() {
				textView.append(s);
				textView.append("\n");
//				final int scrollAmount = textView.getLayout().getLineTop(textView.getLineCount()) - textView.getHeight();
//				// if there is no need to scroll, scrollAmount will be <=0
//				if (scrollAmount > 0)
//					textView.scrollTo(0, scrollAmount);
//				else
//					textView.scrollTo(0, 0);
			}
		});
	}
	
	private void handleScanResult(Map<String, String> nameValueMap) {
		final String rendezvousUrl = nameValueMap.get("cryptocorp");
		log.info("rendezvous at " + rendezvousUrl);
		appendMessage("rendezvous at " + rendezvousUrl);

		if (keychain == null)
			keychain = new DeterministicKeyChain(new SecureRandom());
		DeterministicKey myKey = keychain.getWatchingKey();
		JSONObject req = new JSONObject();
		try {
			req.put("walletAgent", application.httpUserAgent());
			req.put("key", myKey.serializePubB58(wallet.getNetworkParameters()));
			JSONObject attributes = new JSONObject();
			attributes.put("role", "mobile");
			req.put("attributes", attributes);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		new HttpJsonPostThread(req, rendezvousUrl + "/keys", application.httpUserAgent()) {

			@Override
			protected void handleResponse(final Object head) {
				log.info(head.toString());
				runOnUiThread(new Runnable() {
					public void run() {
						appendMessage("rendezvous result: " + head);
						try {
							listenToRendezvous(new URL(rendezvousUrl));
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				});
			}

			@Override
			protected void handleException(@Nonnull final Exception x) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						error(R.string.input_parser_invalid_bitcoin_uri, "Failed to propose key: " + x.getMessage());
					}
				});
			}
		}.start();
	}

	private void listenToRendezvous(final URL url) {
		String[] pathEl = url.getPath().split("/");
		channelId = pathEl[pathEl.length - 1];
		URL base;
		try {
			SocketIO.setDefaultSSLSocketFactory(SSLContext.getDefault());
			base = new URL(url.getProtocol(), url.getHost(), url.getPort(), "");
		} catch (NoSuchAlgorithmException e1) {
			throw new RuntimeException(e1);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		
		socket = new SocketIO(base);
		socket.connect(new IOCallback() {
			@Override
			public void onMessage(JSONObject json, IOAcknowledge ack) {
				try {
					appendMessage("Server said:" + json.toString(2));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onMessage(String data, IOAcknowledge ack) {
				appendMessage("Server said: " + data);
			}

			@Override
			public void onError(SocketIOException socketIOException) {
				appendMessage("an Error occured");
			}

			@Override
			public void onDisconnect() {
				appendMessage("Connection terminated.");
			}

			@Override
			public void onConnect() {
				try {
					socket.emit("subscribe", new JSONObject().put("c", channelId));
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
				appendMessage("Connection established");
			}

			@Override
			public void on(String event, IOAcknowledge ack, Object... args) {
				appendMessage("Server triggered event '" + event + "'");
				try {
					if ("keys".equals(event)) {
						JSONObject keysJson = (JSONObject)args[0];
						Iterator<String> iter = keysJson.keys();
						final List<String> keyStrings = Lists.newArrayList();
						String foundLeadKey = null;

						boolean isComplete = false;
						while (iter.hasNext()) {
							String key = iter.next();
							final JSONObject attrsJson = keysJson.getJSONObject(key);
							final String role = attrsJson.getString("role");
							appendMessage(role + ": " + key);
							log.info(role + ": " + key);
							keyStrings.add(key);
							if ("risk".equals(role)) {
								isComplete = true;
							} else if ("lead".equals(role)) {
								foundLeadKey = key;
							}
						}

						final String leadKeyString = foundLeadKey;
						
						if (isComplete) {
							appendMessage("complete!");
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									socket.disconnect();
									handleMarry(url, keyStrings, leadKeyString);
								}
							});
						}
					}
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
			}
		});

	}

	protected void error(final int messageResId, final Object... messageArgs)
	{
		dialog(this, null, R.string.address_book_options_paste_from_clipboard_title, messageResId, messageArgs);
	}

	protected void dialog(final Context context, @Nullable final OnClickListener dismissListener, final int titleResId, final int messageResId,
			final Object... messageArgs)
	{
		final DialogBuilder dialog = new DialogBuilder(context);
		if (titleResId != 0)
			dialog.setTitle(titleResId);
		dialog.setMessage(context.getString(messageResId, messageArgs));
		dialog.singleDismissButton(dismissListener);
		dialog.show();
	}
}
