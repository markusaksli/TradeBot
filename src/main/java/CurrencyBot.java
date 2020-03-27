import org.knowm.xchange.bitmex.BitmexExchange;
import org.knowm.xchange.currency.CurrencyPair;

public class CurrencyBot implements StrategyListener {
    CurrencyPair pair;
    BitmexExchange exchange;


    @Override
    public void subscribe(Strategy s) {
        s.listen(this);
    }

    @Override
    public void onBuySignal() {
//set buy order
    }

    @Override
    public void onSellSignal() {
//close orders
    }
}
