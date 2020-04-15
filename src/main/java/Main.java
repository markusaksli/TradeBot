import com.google.gson.JsonObject;
import com.webcerebrium.binance.api.BinanceApiException;
import trading.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Main {
    static Set<Currency> currencies; //There should never be two of the same Currency

    public static void main(String[] args) throws BinanceApiException {
        Account toomas = new Account("Investor Toomas", 1000);
        BuySell.setAccount(toomas);

        //Optional for simulation, increases API request limits
        Scanner sc = new Scanner(System.in);
        while (true) {
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

        }

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
            currencies.add(new Currency(arg, 250, true));
        }
        long endTime = System.nanoTime();
        double time = (endTime - startTime) / 1.e9;

        System.out.println("---SETUP DONE (" + Formatter.formatDecimal(time) + " s)");

        System.out.println("Commands: profit, active, history, wallet, currencies");

        //From this point we only use the main thread to check how the bot is doing
        while (true) {
            String in = sc.nextLine();
            switch (in) {
                case "profit":
                    System.out.println("Account profit: " + Formatter.formatPercent(toomas.getProfit()));
                    break;
                case "active":
                    System.out.println("Active trades:");
                    for (Trade trade : toomas.getActiveTrades()) {
                        System.out.println(trade);
                    }
                    break;
                case "history":
                    System.out.println("Closed trades:");
                    for (Trade trade : toomas.getTradeHistory()) {
                        System.out.println(trade);
                    }
                    break;
                case "wallet":
                    System.out.println("Total wallet value: " + Formatter.formatDecimal(toomas.getTotalValue()) + " USDT");
                    System.out.println(toomas.getFiat() + " USDT");
                    for (Map.Entry<Currency, Double> entry : toomas.getWallet().entrySet()) {
                        if (entry.getValue() != 0) {
                            System.out.println(entry.getValue() + " " + entry.getKey().getCoin() + " (" + entry.getKey().getPrice() * entry.getValue() + " USDT)");
                        }
                    }
                    break;
                case "currencies":
                    for (Currency currency : currencies) {
                        System.out.println(currency);
                    }
                    break;
                default:
                    System.out.println("Commands: profit, active, history, wallet, currencies");
            }
        }
    }
}
