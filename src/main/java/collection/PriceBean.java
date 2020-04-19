package collection;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PriceBean {
    private final double price;
    private final long timestamp;
    private int isClose = 0;
    private static SimpleDateFormat dateFormat;

    public PriceBean(double price, long timestamp) {
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
        PriceBean.dateFormat = dateFormat;
    }

    @Override
    public String toString() {
        return timestamp + ";" + price + ";" + isClose;
    }
}
