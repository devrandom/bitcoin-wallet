package co.cryptocorp.oracle.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.signers.CustomTransactionSigner;
import com.google.bitcoin.signers.TransactionSigner;
import com.google.bitcoin.wallet.KeyBag;
import com.google.bitcoin.wallet.RedeemData;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by devrandom.
 */
public class CryptocorpTransactionSigner implements TransactionSigner {
	private static final Logger log = LoggerFactory.getLogger(CustomTransactionSigner.class);
	public static final Joiner PATH_JOINER = Joiner.on("/");
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@JsonProperty
	private String id;

	@JsonProperty
	private String myAccountKeyString;

	@JsonProperty
	private List<String> accountKeyStrings;

	@JsonProperty
	private String url;

	public CryptocorpTransactionSigner() {
	}

	@JsonIgnore
	public CryptocorpTransactionSigner(URL url, String id, String myAccountKey, List<String> accountKeyStrings) {
		Preconditions.checkArgument(accountKeyStrings.contains(myAccountKey));
		this.id = id;
		this.myAccountKeyString = myAccountKey;
		this.accountKeyStrings = accountKeyStrings;
		this.url = url.toString();

	}

	@JsonIgnore
	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public byte[] serialize() {
		try {
			return new ObjectMapper().writeValueAsBytes(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deserialize(byte[] data) {
		try {
			CryptocorpTransactionSigner o = new ObjectMapper().readValue(data, CryptocorpTransactionSigner.class);
			this.url = o.url;
			this.id = o.id;
			this.myAccountKeyString = o.myAccountKeyString;
			this.accountKeyStrings = o.accountKeyStrings;

			// FIXME remove
			if (this.url == null) {
				this.url = "https://s.digitaloracle.co/keychains/" + id;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static public class SigningException extends Wallet.CompletionException {
		private String msg;
		public SigningException(String msg) {
			this.msg = msg;
		}

		@Override
		public String toString() {
			return msg;
		}
	}

	@Override
	public boolean signInputs(ProposedTransaction propTx, KeyBag keyBag) {
		Transaction tx = propTx.partialTx;
		int numInputs = tx.getInputs().size();
		int numOutputs = tx.getOutputs().size();
		List<byte[]> redeemScripts = Lists.newArrayList();
		List<String> chainPaths = Lists.newArrayList();
		List<byte[]> inputTransactions = Lists.newArrayList();
		List<String> outputChainPaths = Lists.newArrayList();

		for (int i = 0; i < numInputs; i++) {
			TransactionInput txIn = tx.getInput(i);
			TransactionOutput txOut = txIn.getConnectedOutput();
			if (txOut == null) {
				throw new RuntimeException("Missing input transaction");
			}

			inputTransactions.add(txOut.getParentTransaction().bitcoinSerialize());

			Script scriptPubKey = txOut.getScriptPubKey();
			if (!scriptPubKey.isPayToScriptHash()) {
				log.warn("CustomTransactionSigner works only with P2SH transactions");
				return false;
			}
			Script inputScript = txIn.getScriptSig();
			checkNotNull(inputScript);
			log.info("scriptSig {}", inputScript);
			RedeemData redeemData = txIn.getConnectedRedeemData(keyBag);
			if (redeemData == null) {
				log.warn("No redeem data found for input {}", i);
				redeemScripts.add(null);
				chainPaths.add(null);
				continue;
			}

			redeemScripts.add(redeemData.redeemScript.getProgram());
			List<ChildNumber> path = Lists.newArrayList(propTx.keyPaths.get(txOut.getScriptPubKey()));
			path.remove(0); // FIXME
			String pathString = PATH_JOINER.join(path);
			chainPaths.add(pathString);

			//txIn.setScriptSig(inputScript);
		}

		for (int i = 0 ; i < numOutputs ; i++) {
			TransactionOutput out = tx.getOutput(i);
			Script script = out.getScriptPubKey();
			boolean foundPath = false;
			if (script.isPayToScriptHash()) {
				byte[] scriptHash = script.getPubKeyHash();
				RedeemData redeemData = keyBag.findRedeemDataFromScriptHash(scriptHash);
				if (redeemData != null) {
					ECKey key = redeemData.keys.get(0);
					if (key instanceof DeterministicKey) {
						List<ChildNumber> path = Lists.newArrayList(((DeterministicKey) key).getPath());
						path.remove(0);
						outputChainPaths.add(PATH_JOINER.join(path));
						foundPath = true;
					}
				}
			}

			if (!foundPath)
				outputChainPaths.add(null);
		}

		ApiTransaction apiTx;
		try {
			apiTx = new ApiTransaction(propTx.partialTx.bitcoinSerialize(),
					redeemScripts,
					chainPaths,
					inputTransactions,
					outputChainPaths,
					accountKeyStrings
					);
		} catch (AddressFormatException e) {
			throw new RuntimeException(e);
		}

		SigningRequest request = new SigningRequest("BitcoinWallet-001", apiTx, false);
		byte[] spendId = new byte[16];
		new SecureRandom().nextBytes(spendId);
		request.setSpendId(spendId);

		HttpURLConnection connection = null;
		try {
			String transactionsUrl = url + "/transactions";
			String msg = OBJECT_MAPPER.writeValueAsString(request);
			log.info("send to {}: {}", url, msg);
			connection = (HttpURLConnection) new URL(transactionsUrl).openConnection();
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);
			connection.connect();
			OutputStream os = connection.getOutputStream();
			OBJECT_MAPPER.writeValue(os, request);
			os.close();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
			{
				SigningResponse apiResponse = OBJECT_MAPPER.readValue(connection.getInputStream(), SigningResponse.class);
				log.info(OBJECT_MAPPER.writeValueAsString(apiResponse));

				if ("success".equals(apiResponse.getResult())) {
					Transaction newTx = new Transaction(tx.getParams(), apiResponse.getTransaction().getBytes());
					Preconditions.checkState(newTx.getInputs().size() == numInputs);
					for (int i = 0; i < numInputs; i++) {
						Script scriptSig = newTx.getInput(i).getScriptSig();
						log.info("scriptSig result {}", scriptSig);
						tx.getInput(i).setScriptSig(scriptSig);
					}
				} else {
					throw new SigningException(apiResponse.getResult());
				}
			} else {
				log.error("Got response " + connection.getResponseMessage());
				throw new SigningException(connection.getResponseMessage());
			}

		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				connection.disconnect();
			} catch (Exception e) {
				log.error("disconnect", e);
			}
		}

		return true;
	}
}
