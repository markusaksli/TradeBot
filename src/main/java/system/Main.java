package system;

import java.util.*;


public class Main {
    public static void main(String[] args) {
        Formatter.getSimpleFormatter().setTimeZone(TimeZone.getDefault());
        //Program config.
        try {
            BinanceAPI.get().getPrice("BTCUSDT");
        } catch (ExceptionInInitializerError cause) {
            if (cause.getCause() != null) {
                if (cause.getCause().getMessage() != null && cause.getCause().getMessage().toLowerCase().contains("banned")) {
                    long bannedTime = Long.parseLong(cause.getCause().getMessage().split("until ")[1].split("\\.")[0]);
                    System.out.println("\nIP Banned by Binance API until " + Formatter.formatDate(bannedTime) + " (" + Formatter.formatDuration(bannedTime - System.currentTimeMillis()) + ")");
                } else {
                    cause.printStackTrace();
                }
            }
            new Scanner(System.in).next();
            System.exit(3);
        }
        System.out.println("Welcome to TradeBot (v0.11.0)");

        //TODO: Implement CLI interface to create and monitor instances
    }
}