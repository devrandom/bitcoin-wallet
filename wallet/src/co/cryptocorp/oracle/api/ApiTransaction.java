package co.cryptocorp.oracle.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Lists;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

/**
 * Created by devrandom.
 */
public class ApiTransaction {
	private final byte[] bytes;
	private final List<byte[]> inputScripts;
	private final List<String> chainPaths;
	private final List<byte[]> inputTransactions;
	private final List<String> outputChainPaths;
	@JsonIgnore
	private List<DeterministicKey> masterKeys;

	public ApiTransaction(@JsonProperty("bytes") @JsonDeserialize(using = HexDeserializer.class)
	                      byte[] bytes,
	                      @JsonProperty("inputScripts") @JsonDeserialize(contentUsing = HexDeserializer.class)
	                      List<byte[]> inputScripts,
	                      @JsonProperty("chainPaths")
	                      List<String> chainPaths,
	                      @JsonProperty("inputTransactions") @JsonDeserialize(contentUsing = HexDeserializer.class)
	                      List<byte[]> inputTransactions,
	                      @JsonProperty("outputChainPaths")
	                      List<String> outputChainPaths,
	                      @JsonProperty("masterKeys")
	                      List<String> masterKeyStrings
	) throws AddressFormatException {
		this.bytes = bytes;
		this.inputScripts = inputScripts;
		this.chainPaths = chainPaths;
		this.inputTransactions = inputTransactions;
		this.outputChainPaths = outputChainPaths;

		if (masterKeyStrings != null) {
			masterKeys = Lists.newArrayList();
			for (String key: masterKeyStrings) {
				masterKeys.add(HDUtil.parseDeterministicKey(key));
			}
		}
	}

	@JsonSerialize(using=HexSerializer.class)
	public byte[] getBytes() {
		return bytes;
	}

	@JsonSerialize(contentUsing=HexSerializer.class)
	public List<byte[]> getInputScripts() { return inputScripts; }

	public List<String> getChainPaths() {
		return chainPaths;
	}

	@JsonSerialize(contentUsing=HexSerializer.class)
	public List<byte[]> getInputTransactions() { return inputTransactions; }

	public List<String> getOutputChainPaths() {
		return outputChainPaths;
	}

	@JsonSerialize(contentUsing=DeterministicKeySerializer.class)
	public List<DeterministicKey> getMasterKeys() {
		return masterKeys;
	}
}
