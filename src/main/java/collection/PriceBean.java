package collection;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PriceBean {
    private final double price;
    private final long timestamp;
    private int isClose = 0;
    private static SimpleDateFormat dateFormat;

    public PriceBean(long timestamp, double price) {
        this.price = price;
        this.timestamp = timestamp;
    }

    public PriceBean(long timestamp, double price, int isClose) {
        this.price = price;
        this.timestamp = timestamp;
        this.isClose = isClose;
    }

    public static PriceBean of(String line) {
        String[] values = line.split(";");
        return new PriceBean(Long.parseLong(values[0]), Double.parseDouble(values[1]), Integer.parseInt(values[2]));
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
        this.isClose = 1;
    }

    public boolean isClose() {
        return isClose == 1;
    }

    public static void setDateFormat(SimpleDateFormat dateFormat) {
        PriceBean.dateFormat = dateFormat;
    }

    @Override
    public String toString() {
        return timestamp + ";" + price + ";" + isClose;
    }
}
