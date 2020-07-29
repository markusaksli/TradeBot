import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.general.RateLimit;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import modes.*;
import trading.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class Main {
    private static List<Currency> currencies;

    public static void main(String[] args) {
        //Program config.
        new ConfigSetup();

        System.out.println("Welcome to TradeBot\n" +
                "(made by Markus Aksli, Marten Türk, and Mark Robin Kalder)\n" +
                "\n" +
                "This is a cryptocurrency trading bot that uses the Binance API,\n" +
                "and a strategy based on a couple of 5 minute chart indicators\n" +
                "(RSI, MACD, Bollinger Bands)\n" +
                "(The bot only trades USDT fiat pairs)\n" +
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
        System.out.println("---Entering " + Mode.get().name().toLowerCase() + " mode");


        if (Mode.get() == Mode.COLLECTION) {
            new Collection(); //Init collection mode.

        } else {
            LocalAccount localAccount = null;
            long startTime = System.nanoTime();
            switch (Mode.get()) {
                case LIVE:
                    new Live(); //Init live mode.
                    localAccount = Live.getAccount();
                    currencies = Live.getCurrencies();
                    break;
                case SIMULATION:
                    new Simulation(); //Init simulation mode.
                    currencies = Simulation.getCurrencies();
                    localAccount = Simulation.getAccount();
                    break;
                case BACKTESTING:
                    new Backtesting(); //Init Backtesting mode.
                    currencies = Backtesting.getCurrencies();
                    localAccount = Backtesting.getAccount();
                    break;
            }
            long endTime = System.nanoTime();
            double time = (endTime - startTime) / 1.e9;

            System.out.println("---" + (Mode.get().equals(Mode.BACKTESTING) ? "Backtesting" : "Setup") + " finished (" + Formatter.formatDecimal(time) + " s)");
            while (Mode.get().equals(Mode.BACKTESTING)) {
                System.out.println("Type quit to quit");
                String s = sc.nextLine();
                if (s.toLowerCase().equals("quit")) {
                    System.exit(0);
                    break;
                } else {
                    System.out.println("Type quit to quit");
                }
            }

            assert localAccount != null;
            //From this point we only use the main thread to check how the bot is doing
            System.out.println("Commands: profit, active, history, wallet, currencies, open, close, close all, quit");
            while (true) {
                String in = sc.nextLine();
                switch (in) {
                    case "profit":
                        System.out.println("Account profit: " + Formatter.formatPercent(localAccount.getProfit()) + "\n");
                        break;
                    case "active":
                        System.out.println("Active trades:");
                        for (Trade trade : localAccount.getActiveTrades()) {
                            System.out.println(trade);
                        }
                        System.out.println(" ");
                        break;
                    case "secret":
                        for (int i = 0; i < 10000; i++) {
                            System.out.println(currencies.get(0));
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case "history":
                        System.out.println("Closed trades:");
                        for (Trade trade : localAccount.getTradeHistory()) {
                            System.out.println(trade);
                        }
                        break;
                    case "wallet":
                        System.out.println("Total wallet value: " + Formatter.formatDecimal(localAccount.getTotalValue()) + " USDT");
                        System.out.println(localAccount.getFiat() + " USDT");
                        for (Map.Entry<Currency, Double> entry : localAccount.getWallet().entrySet()) {
                            if (entry.getValue() != 0) {
                                System.out.println(entry.getValue() + " " + entry.getKey().getPair() + " (" + entry.getKey().getPrice() * entry.getValue() + " USDT)");
                            }
                        }
                        break;
                    case "currencies":
                        for (Currency currency : Simulation.getCurrencies()) {
                            System.out.println((Simulation.getCurrencies().indexOf(currency) + 1) + "   " + currency);
                        }
                        System.out.println(" ");
                        break;
                    case "open":
                        System.out.println("Enter ID of currency");
                        BuySell.open(currencies.get(Integer.parseInt(sc.nextLine()) - 1), "Manually opened", Instant.now().getEpochSecond() * 1000);
                        break;
                    case "close":
                        System.out.println("Enter ID of active trade");
                        String tradeId = sc.nextLine();
                        BuySell.close(localAccount.getActiveTrades().get(Integer.parseInt(tradeId) - 1));
                        break;
                    case "close all":
                        localAccount.getActiveTrades().forEach(BuySell::close);
                        break;
                    case "quit":
                        System.exit(0);
                    default:
                        System.out.println("Commands: profit, active, history, wallet, currencies, open, close, close all, log, quit");
                        break;
                }
            }
        }
    }
}