package trading;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;

public final class CurrentAPI {
    private static BinanceApiClientFactory factory;

    public static void login(String apiKey, String secretKey) {
        factory = BinanceApiClientFactory.newInstance(apiKey, secretKey);
    }

    public static BinanceApiClientFactory getFactory() {
        if (factory == null) {
            factory = BinanceApiClientFactory.newInstance();
        }
        return factory;
    }

    public static BinanceApiRestClient get() {
        return getFactory().newRestClient();
    }
}
