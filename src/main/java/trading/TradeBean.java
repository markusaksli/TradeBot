package trading;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TradeBean {
    private final double price;
    private final Long timestamp;
    //private final int isClose;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    public TradeBean(double price, Long timestamp) {
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getDate() {
        return dateFormat.format(new Date(timestamp));
    }

    public double getPrice() {
        return price;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return getDate() + ";" + price + ";" + timestamp;
    }
}
