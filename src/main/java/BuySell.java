/**
 * Created an account on Testnet.Bitmex.com for us to use.
 * Username: fepster99@gmail.com
 * Password: Lihtrahva123
 * ID = ZoKlEc3zTsR0L_KLbFQCthKc
 * Secret: JCC_KV8_U4T8ZCyEZIceRwzTKTosMj02jcsQqYPGXUB9BkgU
 */

import java.io.IOException;
import java.math.BigDecimal;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bitmex.BitmexExchange;
import org.knowm.xchange.bitmex.BitmexPrompt;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;

public class BuySell {
    public static void main(String[] args) throws IOException {
        createExchange();
    }
    public static Exchange createExchange() throws IOException {
        Exchange bitmex = ExchangeFactory.INSTANCE.createExchange(BitmexExchange.class.getName());

        ExchangeSpecification bitmexSpec = bitmex.getExchangeSpecification();
        bitmexSpec.setHost("testnet.bitmex.com");
        bitmexSpec.setSslUri("https://testnet.bitmex.com");
        bitmexSpec.setApiKey("ZoKlEc3zTsR0L_KLbFQCthKc");
        bitmexSpec.setSecretKey("JCC_KV8_U4T8ZCyEZIceRwzTKTosMj02jcsQqYPGXUB9BkgU");

        bitmex.applySpecification(bitmexSpec);
        System.out.println(bitmexSpec.getHost());
        bitmex.getTradeService().placeMarketOrder(new MarketOrder(Order.OrderType.ASK,
                new BigDecimal("1"),
                CurrencyPair.XBT_USD));


        return bitmex;
    }
    public static void buy(CurrencyPair currency) throws IOException {
        Exchange exchange = createExchange();
        TradeService tradeService = exchange.getTradeService();
        MarketDataService marketDataService = exchange.getMarketDataService();
        Trades trades = marketDataService.getTrades(CurrencyPair.BTC_USD, BitmexPrompt.QUARTERLY);
        OrderBook book = marketDataService.getOrderBook(CurrencyPair.BTC_USD, BitmexPrompt.QUARTERLY);




        System.out.println(trades);
        System.out.println(book);
    }
}
