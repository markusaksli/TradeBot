import com.webcerebrium.binance.api.BinanceApi;

public final class CurrentAPI {
    private static final BinanceApi binanceApi = new BinanceApi();

    public static BinanceApi getBinanceApi() {
        return binanceApi;
    }
}
