package trading;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TradeBean {
    private final double price;
    private final long timestamp;
    private int isClose = 0;
    private static SimpleDateFormat dateFormat;

    public TradeBean(double price, long timestamp) {
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

    public static void setDateFormat(SimpleDateFormat dateFormat) {
        TradeBean.dateFormat = dateFormat;
    }

    @Override
    public String toString() {
        return timestamp + ";" + price + ";" + isClose;
    }
}
