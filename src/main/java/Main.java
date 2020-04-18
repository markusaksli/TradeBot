import com.google.gson.JsonObject;
import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceAggregatedTrades;
import com.webcerebrium.binance.datatype.BinanceCandlestick;
import com.webcerebrium.binance.datatype.BinanceInterval;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import trading.*;
import trading.Currency;
import trading.Formatter;

import java.sql.SQLOutput;
import java.util.*;

public class Main {
    static Set<Currency> currencies; //There should never be two of the same Currency

    public static void main(String[] args) throws BinanceApiException {
        Account toomas = new Account("Investor Toomas", 1000);
        BuySell.setAccount(toomas);


        //Optional for simulation, increases API request limits
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter 1 or 2. 1: Backtesting; 2: Live ------");
        String mode = sc.nextLine();
        if (mode.equals("1")) {
            System.out.println("Entering BACKTESTING MODE");

        } else if (mode.equals("2")) {
            System.out.println("Entering LIVE mode");

        /*while (true) {
            System.out.println("Enter your API Key: ");
            String apiKey = sc.nextLine();
            if (apiKey.length() == 64) {
                CurrentAPI.get().setApiKey(apiKey);
                System.out.println("Enter your Secret Key: ");
                String apiSecret = sc.nextLine();
                if (apiSecret.length() == 64) {
                    CurrentAPI.get().setSecretKey(apiSecret);
                    break;
                } else System.out.println("Secret API is incorrect, enter again.");
            } else System.out.println("Incorrect API, enter again.");

        }*/

        /*JsonObject account = CurrentAPI.get().account();
        //Connection with Binance API and sout-ing some info.
        System.out.println("Maker Commission: " + account.get("makerCommission").getAsBigDecimal());
        System.out.println("Taker Commission: " + account.get("takerCommission").getAsBigDecimal());
        System.out.println("Buyer Commission: " + account.get("buyerCommission").getAsBigDecimal());
        System.out.println("Seller Commission: " + account.get("sellerCommission").getAsBigDecimal());
        System.out.println("Can Trade: " + account.get("canTrade").getAsBoolean());
        System.out.println("Can Withdraw: " + account.get("canWithdraw").getAsBoolean());
        System.out.println("Can Deposit: " + account.get("canDeposit").getAsBoolean());*/

            currencies = new HashSet<>(); //BTC ETH LINK BNB BCH XRP LTC EOS XTZ DASH ETC TRX XLM ADA ZEC
            long startTime = System.nanoTime();
            for (String arg : args) {
                //The currency class contains all of the method calls that drive the activity of our bot
                currencies.add(new Currency(arg, 250, true, false));
            }
            long endTime = System.nanoTime();
            double time = (endTime - startTime) / 1.e9;

            System.out.println("---SETUP DONE (" + Formatter.formatDecimal(time) + " s)");

        }

        //From this point we only use the main thread to check how the bot is doing
        while (true) {
            System.out.println("Commands: profit, active, history, wallet, currencies");
            String in = sc.nextLine();
            switch (in) {
                case "profit":
                    System.out.println("Account profit: " + Formatter.formatPercent(toomas.getProfit()) + "\n");
                    break;
                case "active":
                    System.out.println("Active trades:");
                    for (Trade trade : toomas.getActiveTrades()) {
                        System.out.println(trade);
                    }
                    System.out.println(" ");
                    break;
                case "history":
                    System.out.println("Closed trades:");
                    for (Trade trade : toomas.getTradeHistory()) {
                        System.out.println(trade);
                    }
                    System.out.println(" ");
                    break;
                case "wallet":
                    System.out.println("Total wallet value: " + Formatter.formatDecimal(toomas.getTotalValue()) + " USDT");
                    System.out.println(toomas.getFiat() + " USDT");
                    for (Map.Entry<Currency, Double> entry : toomas.getWallet().entrySet()) {
                        if (entry.getValue() != 0) {
                            System.out.println(entry.getValue() + " " + entry.getKey().getCoin() + " (" + entry.getKey().getPrice() * entry.getValue() + " USDT)");
                        }
                    }
                    System.out.println(" ");
                    break;
                case "currencies":
                    for (Currency currency : currencies) {
                        System.out.println(currency);
                    }
                    System.out.println(" ");
                    break;
                default:
                    System.out.println("Wrong input. Try again \n");
                    break;
            }
        }
    }
}
