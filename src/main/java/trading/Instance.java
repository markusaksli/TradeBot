package trading;

import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.api.client.exception.BinanceApiException;
import system.BinanceAPI;
import system.ConfigSetup;
import system.Formatter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Instance implements Closeable {
    public static double STARTING_VALUE;
    private static final File credentialsFile = new File("credentials.txt");

    private final String ID;
    private final LocalAccount account;
    private final Mode mode;

    public String getID() {
        return ID;
    }

    public LocalAccount getAccount() {
        return account;
    }

    public Mode getMode() {
        return mode;
    }

    List<Currency> currencies = new ArrayList<>();

    @Override
    public void close() {
        for (Currency currency : currencies) {
            try {
                currency.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Instance(List<String> coins, String ID) {
        this.ID = ID;
        this.mode = Mode.SIMULATION;
        this.account = new LocalAccount(this, STARTING_VALUE);

        for (String arg : coins) {
            //The currency class contains all of the method calls that drive the activity of our bot
            try {
                currencies.add(new Currency(arg, account));
            } catch (BinanceApiException e) {
                System.out.println("---Could not add " + arg + ConfigSetup.getFiat());
                System.out.println(e.getMessage());
            }
        }
    }

    //LIVE
    public Instance(List<String> coins, String ID, String apiKey, String secretKey) {
        this.ID = ID;
        this.mode = Mode.LIVE;

        /*boolean fileFailed = true;
        if (credentialsFile.exists()) {
            try {
                final List<String> strings = Files.readAllLines(credentialsFile.toPath());
                if (!strings.get(0).matches("\\*+")) {
                    account = new LocalAccount(strings.get(0), strings.get(1));
                    fileFailed = false;
                } else {
                    System.out.println("---credentials.txt has not been set up");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("---Failed to use credentials in credentials.txt");
            }
        } else {
            System.out.println("---credentials.txt file not detected!");
        }

        if (fileFailed) {
            Scanner sc = new Scanner(System.in);
            String apiKey;
            String apiSecret;
            while (true) {
                System.out.println("Enter your API Key: ");
                apiKey = sc.nextLine();
                if (apiKey.length() == 64) {
                    System.out.println("Enter your Secret Key: ");
                    apiSecret = sc.nextLine();
                    if (apiSecret.length() == 64) {
                        break;
                    } else System.out.println("Secret API is incorrect, enter again.");
                } else System.out.println("Incorrect API, enter again.");
            }
        }*/
        account = new LocalAccount(this, apiKey, secretKey);
        System.out.println("Can trade: " + account.getRealAccount().isCanTrade());
        System.out.println(account.getMakerComission() + " Maker commission.");
        System.out.println(account.getBuyerComission() + " Buyer commission");
        System.out.println(account.getTakerComission() + " Taker comission");

        //TODO: Open price for existing currencies
        String current = "";
        try {
            List<String> addedCurrencies = new ArrayList<>();
            for (AssetBalance balance : account.getRealAccount().getBalances()) {
                if (balance.getFree().matches("0\\.0+")) continue;
                if (coins.contains(balance.getAsset())) {
                    current = balance.getAsset();
                    Currency balanceCurrency = new Currency(current, account);
                    currencies.add(balanceCurrency);
                    addedCurrencies.add(current);
                    double amount = Double.parseDouble(balance.getFree());
                    account.getWallet().put(balanceCurrency, amount);
                    double price = Double.parseDouble(BinanceAPI.get().getPrice(current + ConfigSetup.getFiat()).getPrice());
                    Optional<String> lotSize = BinanceAPI.get().getExchangeInfo().getSymbolInfo(current + ConfigSetup.getFiat()).getFilters().stream().filter(f -> FilterType.LOT_SIZE == f.getFilterType()).findFirst().map(f1 -> f1.getMinQty());
                    Optional<String> minNotational = BinanceAPI.get().getExchangeInfo().getSymbolInfo(current + ConfigSetup.getFiat()).getFilters().stream().filter(f -> FilterType.MIN_NOTIONAL == f.getFilterType()).findFirst().map(SymbolFilter::getMinNotional);
                    if (lotSize.isPresent()) {
                        if (amount < Double.parseDouble(lotSize.get())) {
                            System.out.println(balance.getFree() + " " + current + " is less than LOT_SIZE " + lotSize.get());
                            continue;
                        }
                    }
                    if (minNotational.isPresent()) {
                        if (amount * price < Double.parseDouble(minNotational.get())) {
                            System.out.println(current + " notational value of "
                                    + Formatter.formatDecimal(amount * price) + " is less than min notational "
                                    + minNotational.get());
                            continue;
                        }
                    }
                    final Trade trade = new Trade(balanceCurrency, balanceCurrency.getPrice(), amount, "Trade opened due to: Added based on live account\t");
                    account.getActiveTrades().add(trade);
                    balanceCurrency.setActiveTrade(trade);
                    System.out.println("Added an active trade of " + balance.getFree() + " " + current + " at " + Formatter.formatDecimal(trade.getEntryPrice()) + " based on existing balance in account");
                }
            }
            account.setStartingValue(account.getTotalValue());
            for (String arg : coins) {
                if (!addedCurrencies.contains(arg)) {
                    current = arg;
                    currencies.add(new Currency(current, account));
                }
            }
        } catch (Exception e) {
            System.out.println("---Could not add " + current + ConfigSetup.getFiat());
            System.out.println(e.getMessage());
        }
    }

    //BACKTESTING
    public Instance(String path) {
        this.ID = path;
        this.account = new LocalAccount(this, STARTING_VALUE);
        this.mode = Mode.BACKTESTING;

        /*
        final String[] backtestingFiles = Collection.getDataFiles();
        if (backtestingFiles.length == 0) {
            System.out.println("No backtesting files detected!");
            System.exit(0);
        }
        Scanner sc = new Scanner(System.in);
        System.out.println("\nBacktesting data files:\n");
        for (int i = 0; i < backtestingFiles.length; i++) {
            System.out.println("[" + (i + 1) + "] " + backtestingFiles[i]);
        }
        System.out.println("\nEnter a number to select the backtesting data file");
        String input = sc.nextLine();
        if (!input.matches("\\d+")) continue;
        int index = Integer.parseInt(input);
        if (index > backtestingFiles.length) {

        }
        String path = "backtesting/" + backtestingFiles[index - 1];
        */

        System.out.println("\n---Backtesting...");
        Currency currency = new Currency(new File(path).getName().split("_")[0], path, account);
        currencies.add(currency);

        for (Trade trade : account.getActiveTrades()) {
            trade.setExplanation(trade.getExplanation() + "Manually closed");
            account.close(trade);
        }


        int i = 1;
        path = path.replace("backtesting", "log");
        String resultPath = path.replace(".dat", "_run_" + i + ".txt");
        while (new File(resultPath).exists()) {
            i++;
            resultPath = path.replace(".dat", "_run_" + i + ".txt");
        }
        new File("log").mkdir();

        currency.log(resultPath);
    }

    public void refreshWalletAndTrades() {
        if (mode != Mode.LIVE) return;
        for (AssetBalance balance : account.getRealAccount().getBalances()) {
            if (balance.getFree().matches("0\\.0+")) continue;
            if (balance.getAsset().equals(ConfigSetup.getFiat())) {
                final double amount = Double.parseDouble(balance.getFree());
                if (account.getFiat() != amount) {
                    System.out.println("---Refreshed " + balance.getAsset() + " from " + Formatter.formatDecimal(account.getFiat()) + " to " + amount);
                    System.out.println(balance.getLocked());
                    account.setFiat(amount);
                }
                continue;
            }
            for (Currency currency : currencies) {
                if ((balance.getAsset() + ConfigSetup.getFiat()).equals(currency.getPair())) {
                    final double amount = Double.parseDouble(balance.getFree());
                    if (!account.getWallet().containsKey(currency)) {
                        System.out.println("---Refreshed " + currency.getPair() + " from 0 to " + balance.getFree());
                        account.getWallet().replace(currency, amount);
                    }
                    if (account.getWallet().get(currency) != amount) {
                        System.out.println("---Refreshed " + currency.getPair() + " from " + Formatter.formatDecimal(account.getWallet().get(currency)) + " to " + balance.getFree());
                        System.out.println(balance.getLocked());
                        account.getWallet().replace(currency, amount);
                    }
                    if (currency.hasActiveTrade()) {
                        if (currency.getActiveTrade().getAmount() > amount) {
                            System.out.println("---Refreshed " + currency.getPair() + " trade from " + Formatter.formatDecimal(currency.getActiveTrade().getAmount()) + " to " + balance.getFree());
                            currency.getActiveTrade().setAmount(amount);
                        }
                    }
                    break;
                }
            }
        }
        System.out.println("---Refreshed wallet and trades");
    }

    static void Interface(Instance instance) {
        Scanner sc = new Scanner(System.in);
        assert instance.account != null;
        //From this point we only use the main thread to check how the bot is doing
        boolean printing = false;
        boolean exitInterface = false;
        Timer timer = null;
        while (!exitInterface) {
            System.out.println("\nCommands: profit, active, history, wallet, currencies, open, close, close all, stop, modes");
            String in = sc.nextLine();
            switch (in) {
                case "profit":
                    System.out.println("\nAccount profit: " + Formatter.formatPercent(instance.account.getProfit()) + "\n");
                    break;
                case "active":
                    System.out.println("\nActive trades:");
                    for (Trade trade : instance.account.getActiveTrades()) {
                        System.out.println(trade);
                    }
                    System.out.println(" ");
                    break;
                case "secret":
                    if (!printing) {
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                System.out.println(instance.currencies.get(0));
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
                    for (Trade trade : instance.account.getTradeHistory()) {
                        System.out.println(trade);
                    }
                    break;
                case "wallet":
                    System.out.println("\nTotal wallet value: " + Formatter.formatDecimal(instance.account.getTotalValue()) + " " + ConfigSetup.getFiat());
                    System.out.println(Formatter.formatDecimal(instance.account.getFiat()) + " " + ConfigSetup.getFiat());
                    for (Map.Entry<Currency, Double> entry : instance.account.getWallet().entrySet()) {
                        if (entry.getValue() != 0) {
                            System.out.println(Formatter.formatDecimal(entry.getValue()) + " " + entry.getKey().getPair().replace(ConfigSetup.getFiat(), "")
                                    + " (" + Formatter.formatDecimal(entry.getKey().getPrice() * entry.getValue()) + " " + ConfigSetup.getFiat() + ")");
                        }
                    }
                    break;
                case "currencies":
                    for (Currency currency : instance.currencies) {
                        System.out.println((instance.currencies.indexOf(currency) + 1) + "   " + currency);
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
                    if (openIndex < 1 || openIndex > instance.currencies.size()) {
                        System.out.println("\nID out of range, use \"currencies\" to see valid IDs!");
                        continue;
                    }
                    instance.account.open(instance.currencies.get(openIndex - 1), "Trade opened due to: Manually opened\t");
                    break;
                case "close":
                    System.out.println("Enter ID of active trade");
                    String closeId = sc.nextLine();
                    if (!closeId.matches("\\d+")) {
                        System.out.println("\nNot an integer!");
                        continue;
                    }
                    int closeIndex = Integer.parseInt(closeId);
                    if (closeIndex < 1 || closeIndex > instance.currencies.size()) {
                        System.out.println("\nID out of range, use \"active\" to see valid IDs!");
                        continue;
                    }
                    instance.account.close(instance.account.getActiveTrades().get(closeIndex - 1));
                    break;
                case "close all":
                    instance.account.getActiveTrades().forEach(instance.account::close);
                    break;
                case "refresh":
                    instance.refreshWalletAndTrades();
                    break;
                case "stop":
                    System.out.println("Close all open trades? (y/n)");
                    String answer = sc.nextLine();
                    answer = answer.trim();
                    if (answer.equalsIgnoreCase("y")) {
                        instance.account.getActiveTrades().forEach(instance.account::close);
                    } else if (!answer.equalsIgnoreCase("n")) {
                        return;
                    }
                    instance.close();
                    exitInterface = true;
                    break;
                case "return":
                    exitInterface = true;
                    break;
                default:
                    break;
            }
        }
        if (timer != null) {
            timer.cancel();
        }
    }

    public enum Mode {
        LIVE,
        SIMULATION,
        BACKTESTING;
    }
}
