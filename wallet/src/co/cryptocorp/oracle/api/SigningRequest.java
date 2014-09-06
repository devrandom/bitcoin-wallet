package co.cryptocorp.oracle.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Map;

/**
 * @author devrandom
 */
public class SigningRequest {
    private final ApiTransaction transaction;

    private String walletAgent;

    private Map<String, String> verifications;

    private boolean broadcast;

    private byte[] spendId;

    private String callback;

    public SigningRequest(
		    @JsonProperty("walletAgent") String walletAgent,
		    @JsonProperty("transaction") ApiTransaction transaction,
		    @JsonProperty("broadcast") boolean broadcast) {
        this.walletAgent = walletAgent;
        this.transaction = transaction;
        this.broadcast = broadcast;
    }

    public SigningRequest() {
        this.transaction = null;
    }

	public String getWalletAgent() {
		return walletAgent;
	}

	public ApiTransaction getTransaction() {
        return transaction;
    }

    public void setVerifications(Map<String, String> verifications) {
        this.verifications = verifications;
    }

    public Map<String, String> getVerifications() {
        return verifications;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

	@JsonSerialize(using = HexSerializer.class)
    public byte[] getSpendId() {
        return spendId;
    }

    public void setSpendId(byte[] spendId) {
        this.spendId = spendId;
    }

    /** Webhook callback URL */
    public void setCallback(String callback) {
        this.callback = callback;
    }

    /** Webhook callback URL */
    public String getCallback() {
        return callback;
    }
}
