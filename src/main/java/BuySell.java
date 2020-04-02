import java.math.BigDecimal;
import java.sql.Timestamp;

public class BuySell {

    //Could also change the parameter to Trade object or smth.
    public void buy(Currency currency) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        double currentPrice = currency.getPrice();
        //Trade trade = new Trade(currentPrice, timestamp);

    }
    public void sell(Trade currency) {

    }
}
