package co.cryptocorp.oracle.api;

/**
 * @author devrandom
 */
public class ApiResponse {
    private String result;

    public ApiResponse(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

	public void setResult(String result) {
		this.result = result;
	}
}
