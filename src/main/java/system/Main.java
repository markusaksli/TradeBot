package system;

import trading.*;
import trading.Currency;

import java.util.*;


public class Main {
    private static List<Currency> currencies;

    public static void main(String[] args) {
        //Program config.
        try {
            ConfigSetup.readConfig();
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
        System.out.println("Welcome to TradeBot (v0.10.0)\n" +
                "(made by Markus Aksli, Marten Türk, and Mark Robin Kalder)\n" +
                "\n" +
                "This is a cryptocurrency trading bot that uses the Binance API,\n" +
                "and a strategy based on a couple of 5 minute chart indicators\n" +
                "(RSI, MACD, Bollinger Bands)\n" +
                "\n" +
                "The bot has the following modes of operation:\n" +
                "---LIVE\n" +
                "-This mode trades with real money on the Binance platform\n" +
                "-API key and Secret key required\n" +
                "---SIMULATION\n" +
                "-Real-time trading simulation based on actual market data\n" +
                "-Trades are only simulated based on market prices \n" +
                "-No actual orders are made\n" +
                "---BACKTESTING\n" +
                "-Simulation based on historical data.\n" +
                "-Allows for quick testing of the behavior and profitability of the bot\n" +
                "-Data needs to be loaded from a .dat file created with the COLLECTION mode\n" +
                "---COLLECTION\n" +
                "-Collects raw market price data from a specified time period\n" +
                "-Collected data is saved in a file in the /backtesting directory\n" +
                "-Never run more than one TradeBot with this mode at the same time\n" +
                "\n" +
                "Simulation and backtesting do not always reflect live performance\n" +
                "Make sure you are ready to commit to a strategy before starting LIVE\n");
    }
}