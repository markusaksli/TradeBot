package collection;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PriceBean {
    private final double price;
    private final long timestamp;
    private boolean closing;
    private static SimpleDateFormat dateFormat;

    public PriceBean(long timestamp, double price) {
        this.price = price;
        this.timestamp = timestamp;
        this.closing = false;
    }

    public PriceBean(long timestamp, double price, boolean closing) {
        this.price = price;
        this.timestamp = timestamp;
        this.closing = closing;
    }

    public double getPrice() {
        return price;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getDate() {
        return dateFormat.format(new Date(timestamp));
    }

    public void close() {
        this.closing = true;
    }

    public boolean isClosing() {
        return closing;
    }

    public static void setDateFormat(SimpleDateFormat dateFormat) {
        PriceBean.dateFormat = dateFormat;
    }

    @Override
    public String toString() {
        return timestamp + ";" + price + ";" + closing;
    }
}
