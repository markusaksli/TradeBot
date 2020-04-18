package trading;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TradeBean {
    private final double price;
    private final Long timestamp;
    private int isClose = 0;
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

    public void close() {
        this.isClose = 1;
    }

    @Override
    public String toString() {
        return getDate() + ";" + timestamp + ";" + price + ";" + isClose;
    }
}
