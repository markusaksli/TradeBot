package trading;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TradeBean {
    private double price;
    private Long timestamp;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    public TradeBean(double price, Long timestamp) {
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getDate() {
        return dateFormat.format(new Date(timestamp));
    }
}
