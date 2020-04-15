package trading;

import com.webcerebrium.binance.api.BinanceApi;

public final class CurrentAPI {
    private static final BinanceApi binanceApi = new BinanceApi();

    /**
     * Class is created because we would not have to call out a new BinanceApi() every time we need it.
     * We call it out here and it can be adressed in other classes as follows: backend.CurrentAPI.get();
     * No need to create an instance backend.CurrentAPI currentapi = new backend.CurrentAPI();
     */

    public static BinanceApi get() {
        return binanceApi;
    }
}
