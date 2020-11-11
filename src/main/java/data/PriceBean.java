package data;

import system.Formatter;

public class PriceBean {
    private final double price;
    private final long timestamp;
    private boolean closing;


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
        return Formatter.formatDate((timestamp));
    }

    public void close() {
        this.closing = true;
    }

    public boolean isClosing() {
        return closing;
    }

    @Override
    public String toString() {
        return Formatter.formatDate(timestamp) + " " + price + (closing ? " is closing" : "");
    }

    public String toCsvString(){
        return String.format("%d,%s,%d", timestamp, price, closing ? 1 : 0);
    }
}
