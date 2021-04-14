package trading;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;

public final class BinanceAPI {
    private static final BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
    private static BinanceApiRestClient defaultClient;

    private BinanceAPI() {
        throw new IllegalStateException("Utility class");
    }

    public static BinanceApiClientFactory login(String apiKey, String secretKey) {
        return BinanceApiClientFactory.newInstance(apiKey, secretKey);
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
}
