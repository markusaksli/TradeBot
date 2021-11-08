package system;

import modes.*;
import modes.Collection;
import trading.*;
import trading.Currency;

import java.util.*;


public class Main {
    private static final String VERSION = "v0.10.2";
    private static List<Currency> currencies;

    public static void main(String[] args) {
        System.out.println("---Startup...");
        //Program config.
        try {
            ConfigSetup.readConfig();
        } catch (ExceptionInInitializerError cause) {
            if (cause.getCause() != null) {
                if (cause.getCause().getMessage().toLowerCase().contains("banned")) {
                    long bannedTime = Long.parseLong(cause.getCause().getMessage().split("until ")[1].split("\\.")[0]);
                    System.out.println("\nIP Banned by Binance API until " + Formatter.formatDate(bannedTime) + " (" + Formatter.formatDuration(bannedTime - System.currentTimeMillis()) + ")");
                }
            } else {
                System.out.println("---Error during startup: ");
                cause.printStackTrace();
            }
            new Scanner(System.in).next();
            System.exit(3);
        }
        System.out.println("\nWelcome to TradeBot " + VERSION + "\n" +
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
        boolean returnToModes = false;
        while (true) {
            Scanner sc = new Scanner(System.in);
            while (true) {
                try {
                    //TODO: Change mode selection to single character
                    System.out.println("Enter bot mode (live, simulation, backtesting, collection)");
                    Mode.set(Mode.valueOf(sc.nextLine().toUpperCase()));
                    break;
                } catch (Exception e) {
                    System.out.println("Invalid mode, try again.");
                }
            }
            System.out.println("\n---Entering " + Mode.get().name().toLowerCase() + " mode");


            if (Mode.get() == Mode.COLLECTION) {
                Collection.startCollection(); //Init collection mode.

            } else {
                LocalAccount localAccount = null;
                long startTime = System.nanoTime();
                switch (Mode.get()) {
                    case LIVE:
                        Live.init(); //Init live mode.
                        localAccount = Live.getAccount();
                        currencies = Live.getCurrencies();
                        break;
                    case SIMULATION:
                        Simulation.init(); //Init simulation mode.
                        currencies = Simulation.getCurrencies();
                        localAccount = Simulation.getAccount();
                        break;
                    case BACKTESTING:
                        Backtesting.startBacktesting(); //Init Backtesting mode.
                        currencies = Backtesting.getCurrencies();
                        localAccount = Backtesting.getAccount();
                        break;
                }
                long endTime = System.nanoTime();
                double time = (endTime - startTime) / 1.e9;

                System.out.println("---" + (Mode.get().equals(Mode.BACKTESTING) ? "Backtesting" : "Setup") + " finished (" + Formatter.formatDecimal(time) + " s)\n");
                while (Mode.get().equals(Mode.BACKTESTING)) {
                    System.out.println("Type \"quit\" to quit");
                    System.out.println("Type \"modes\" to got back to mode selection.");
                    String s = sc.nextLine();
                    if (s.equalsIgnoreCase("quit")) {
                        System.exit(0);
                        break;
                    } else if (s.equalsIgnoreCase("modes")) {
                        returnToModes = true;
                        break;
                    }
                }

                assert localAccount != null;
                //From this point we only use the main thread to check how the bot is doing
                Timer timer = new Timer();
                boolean printing = false;
                while (!returnToModes) {
                    System.out.println("\nCommands: profit, active, history, wallet, currencies, open, close, close all, quit, modes");
                    String in = sc.nextLine();
                    switch (in) {
                        case "profit":
                            System.out.println("\nAccount profit: " + Formatter.formatPercent(localAccount.getProfit()) + "\n");
                            break;
                        case "active":
                            System.out.println("\nActive trades:");
                            for (Trade trade : localAccount.getActiveTrades()) {
                                System.out.println(trade);
                            }
                            System.out.println(" ");
                            break;
                        case "secret":
                            if (!printing) {
                                timer.scheduleAtFixedRate(new TimerTask() {
                                    @Override
                                    public void run() {
                                        System.out.println(currencies.get(0));
                                    }
                                }, 0, 100);
                                printing = true;
                            } else {
                                timer.cancel();
                                timer.purge();
                                printing = false;
                            }
                            break;
                        case "history":
                            System.out.println("\nClosed trades:");
                            for (Trade trade : localAccount.getTradeHistory()) {
                                System.out.println(trade);
                            }
                            break;
                        case "wallet":
                            System.out.println("\nTotal wallet value: " + Formatter.formatDecimal(localAccount.getTotalValue()) + " " + ConfigSetup.getFiat());
                            System.out.println(Formatter.formatDecimal(localAccount.getFiat()) + " " + ConfigSetup.getFiat());
                            for (Map.Entry<Currency, Double> entry : localAccount.getWallet().entrySet()) {
                                if (entry.getValue() != 0) {
                                    System.out.println(Formatter.formatDecimal(entry.getValue()) + " " + entry.getKey().getPair().replace(ConfigSetup.getFiat(), "")
                                            + " (" + Formatter.formatDecimal(entry.getKey().getPrice() * entry.getValue()) + " " + ConfigSetup.getFiat() + ")");
                                }
                            }
                            break;
                        case "currencies":
                            for (Currency currency : currencies) {
                                System.out.println((currencies.indexOf(currency) + 1) + "   " + currency);
                            }
                            System.out.println(" ");
                            break;
                        case "open":
                            System.out.println("Enter ID of currency");
                            String openId = sc.nextLine();
                            if (!openId.matches("\\d+")) {
                                System.out.println("\nNot an integer!");
                                continue;
                            }
                            int openIndex = Integer.parseInt(openId);
                            if (openIndex < 1 || openIndex > currencies.size()) {
                                System.out.println("\nID out of range, use \"currencies\" to see valid IDs!");
                                continue;
                            }
                            BuySell.open(currencies.get(openIndex - 1), "Trade opened due to: Manually opened\t");
                            break;
                        case "close":
                            System.out.println("Enter ID of active trade");
                            String closeId = sc.nextLine();
                            if (!closeId.matches("\\d+")) {
                                System.out.println("\nNot an integer!");
                                continue;
                            }
                            int closeIndex = Integer.parseInt(closeId);
                            if (closeIndex < 1 || closeIndex > currencies.size()) {
                                System.out.println("\nID out of range, use \"active\" to see valid IDs!");
                                continue;
                            }
                            BuySell.close(localAccount.getActiveTrades().get(closeIndex - 1));
                            break;
                        case "close all":
                            localAccount.getActiveTrades().forEach(BuySell::close);
                            break;
                        case "refresh":
                            if (Mode.get().equals(Mode.LIVE)) {
                                Live.refreshWalletAndTrades();
                                System.out.println("---Refreshed wallet and trades");
                            } else {
                                System.out.println("---Can only refresh wallet and trades in live mode!");
                            }
                            break;
                        case "quit":
                            System.exit(0);
                            break;
                        case "modes":
                            returnToModes = true;
                            break;
                        default:
                            break;
                    }
                }
                timer.cancel();
                Mode.reset();
                returnToModes = false;
            }
        }
    }
}