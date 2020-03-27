import com.fasterxml.jackson.annotation.JsonProperty;

public class Wallet_Data {
    @JsonProperty(value = "account")
    private int account;

    @JsonProperty(value = "currency")
    private String currency;

    public int getAccount() {
        return account;
    }

    public String getCurrency() {
        return currency;
    }

    public void setAccount(int account) {
        this.account = account;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "Wallet_Data {" +
                "account=" + account +
                ", currency='" + currency + '\'' +
                '}';
    }
}
