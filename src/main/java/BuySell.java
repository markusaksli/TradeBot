/**
 * Created an acc on Bitrex.com for us to use.
 * Username: fepster99@gmail.com
 * Password: Lihtrahva123
 * ID = ZoKlEc3zTsR0L_KLbFQCthKc
 * Secret: JCC_KV8_U4T8ZCyEZIceRwzTKTosMj02jcsQqYPGXUB9BkgU
 */

import java.io.IOException;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bitmex.BitmexExchange;
import org.knowm.xchange.bitmex.BitmexPrompt;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.service.marketdata.MarketDataService;

public class BuySell {
    public Exchange createExchange() {
        Exchange bitmex = ExchangeFactory.INSTANCE.createExchange(BitmexExchange.class.getName());

        ExchangeSpecification bitmexSpec = bitmex.getDefaultExchangeSpecification();

        bitmexSpec.setApiKey("ZoKlEc3zTsR0L_KLbFQCthKc");
        bitmexSpec.setSecretKey("JCC_KV8_U4T8ZCyEZIceRwzTKTosMj02jcsQqYPGXUB9BkgU");

        bitmex.applySpecification(bitmexSpec);

        return bitmex;
    }
}
