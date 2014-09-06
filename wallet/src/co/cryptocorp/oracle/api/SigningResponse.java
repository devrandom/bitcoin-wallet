package co.cryptocorp.oracle.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * @author devrandom
 */
public class SigningResponse extends ApiResponse {
    private ApiTransaction transaction;
    private Deferral deferral;
    private final Date now = new Date();
    private boolean broadcasted;
    private BigInteger pendingFee;
    private String spendId;

    public String getSpendId() {
        return spendId;
    }

    public void setSpendId(String spendId) {
        this.spendId = spendId;
    }

    public static class Deferral {
        @JsonIgnore
        private long until;
        @JsonProperty
        private String reason;
        @JsonProperty
        private List<String> verifications;

        Deferral() {
        }

        @JsonProperty
        public Date getUntil() {
            if (until > 0)
                return new Date(until);
            else
                return null;
        }

        public String getReason() {
            return reason;
        }

        @JsonProperty
        public List<String> getVerifications() {
            return verifications;
        }
    }

    public SigningResponse(@JsonProperty("result") String result) {
        super(result);
    }
	public SigningResponse() {
		super(null);
	} // FIXME

    public Deferral getDeferral() {
        return deferral;
    }

	public ApiTransaction getTransaction() {
		return transaction;
	}

	public void setTransaction(ApiTransaction transaction) {
        this.transaction = transaction;
    }

    public Date getNow() {
        return now;
    }

    public void setBroadcasted(boolean broadcasted) {
        this.broadcasted = broadcasted;
    }

    public boolean isBroadcasted() {
        return broadcasted;
    }

    public void setPendingFee(BigInteger pendingFee) {
        this.pendingFee = pendingFee;
    }

    public BigInteger getPendingFee() {
        return pendingFee;
    }
}
