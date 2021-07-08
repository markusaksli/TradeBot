package system;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.Account;

public final class BinanceAPI {
    private static BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
    private static BinanceApiRestClient defaultClient;
    private static Account account;
    private static boolean loggedIn;

    private BinanceAPI() {
        throw new IllegalStateException("Utility class");
    }

    public static void login(String apiKey, String secretKey) {
        factory = BinanceApiClientFactory.newInstance(apiKey, secretKey);
        defaultClient = factory.newRestClient();
        account = defaultClient.getAccount();
        loggedIn = true;
    }

    public static BinanceApiClientFactory getFactory() {
        return factory;
    }

    public static BinanceApiRestClient get() {
        if (defaultClient == null) {
            defaultClient = factory.newRestClient();
        }
        return defaultClient;
    }

    public static Account getAccount() {
        if (!loggedIn) return null;
        return account;
    }
}
