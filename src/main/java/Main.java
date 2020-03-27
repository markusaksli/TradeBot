import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.bitmex.BitmexExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.marketdata.MarketDataService;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    public static void main(String[] args) {
/*
        try {
            BinanceApi api = new BinanceApi();

            System.out.println("ETH-BTC PRICE=" + api.pricesMap().get("ETHBTC"));
        } catch (
                BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
*/

        Exchange bitstamp = ExchangeFactory.INSTANCE.createExchange(BitmexExchange.class.getName());

        MarketDataService marketDataService = bitstamp.getMarketDataService();
        
        Ticker ticker = null;
        while (true) {
            try {
                ticker = marketDataService.getTicker(CurrencyPair.ETH_USD);
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (ticker != null) {
                System.out.println(ticker.toString());
            }
        }
    }
}
