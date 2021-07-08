package trading;

import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.api.client.exception.BinanceApiException;
import data.config.ConfigData;
import data.price.PriceReader;
import system.BinanceAPI;
import data.config.Config;
import system.Formatter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class Instance implements Closeable {
    private List<Currency> currencies;
    private String fiat;
    private Config config;
    private final LocalAccount account;
    private final Mode mode;
    private final String ID;

    public String getID() {
        return ID;
    }

    public LocalAccount getAccount() {
        return account;
    }

    public Mode getMode() {
        return mode;
    }


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

    /**
     * Creates a simulation instance
     *
     * @param coins        List of tradeable currencies against the fiat
     * @param fiat         FIAT currency
     * @param config       Loaded Config
     * @param startingFIAT Predefined starting FIAT value
     */
    public Instance(List<String> coins, String fiat, Config config, double startingFIAT) {
        this.mode = Mode.SIMULATION;
        this.fiat = fiat;
        this.config = config;
        this.ID = "Simulation" + "_" + config.name() + "_" + System.currentTimeMillis();
        this.account = new LocalAccount(this, startingFIAT);
        List<Currency> currencies = new ArrayList<>();
        for (String arg : coins) {
            //The currency class contains all of the method calls that drive the activity of our bot
            try {
                currencies.add(new Currency(arg, account));
            } catch (BinanceApiException e) {
                System.out.println("---Could not add " + arg + fiat);
                System.out.println(e.getMessage());
            }
        }
    }

    //BACKTESTING
    public Instance(PriceReader reader, Config config, double startingFIAT) {
        this.mode = Mode.BACKTESTING;
        this.fiat = reader.getFiat();
        this.config = config;
        this.ID = "Backtesting" + "_" + config.name() + "_" + System.currentTimeMillis();
        this.account = new LocalAccount(this, startingFIAT);

        System.out.println("\n---Backtesting...");
        Currency currency = new Currency(reader, account);
        currencies.add(currency);
        currency.runBacktest(reader).thenApply(unused -> {
            for (Trade trade : account.getActiveTrades()) {
                trade.setExplanation(trade.getExplanation() + "Manually closed");
                account.close(trade);
            }
            int i = 1;
            String path = "log/" + reader.getPath().getFileName();
            String resultPath = path.replace(".dat", "_run_" + i + ".txt");
            while (new File(resultPath).exists()) {
                i++;
                resultPath = path.replace(".dat", "_run_" + i + ".txt");
            }
            new File("log").mkdir();
            currency.log(resultPath);

            return null;
        });
    }

    //LIVE
    public Instance(List<String> coins, String fiat, Config config) {
        this.mode = Mode.LIVE;
        this.fiat = fiat;
        this.config = config;
        this.ID = "Backtesting" + "_" + config.name() + "_" + System.currentTimeMillis();
        this.account = new LocalAccount(this, 0);

        //TODO: Open price for existing currencies
        String current = "";
        try {
            List<String> addedCurrencies = new ArrayList<>();
            for (AssetBalance balance : BinanceAPI.getAccount().getBalances()) {
                if (balance.getFree().matches("0\\.0+")) continue;
                if (coins.contains(balance.getAsset())) {
                    current = balance.getAsset();
                    Currency balanceCurrency = new Currency(current, account);
                    currencies.add(balanceCurrency);
                    addedCurrencies.add(current);
                    double amount = Double.parseDouble(balance.getFree());
                    account.getWallet().put(balanceCurrency, amount);
                    double price = Double.parseDouble(BinanceAPI.get().getPrice(current + fiat).getPrice());
                    Optional<String> lotSize = BinanceAPI.get().getExchangeInfo().getSymbolInfo(current + fiat).getFilters().stream().filter(f -> FilterType.LOT_SIZE == f.getFilterType()).findFirst().map(f1 -> f1.getMinQty());
                    Optional<String> minNotational = BinanceAPI.get().getExchangeInfo().getSymbolInfo(current + fiat).getFilters().stream().filter(f -> FilterType.MIN_NOTIONAL == f.getFilterType()).findFirst().map(SymbolFilter::getMinNotional);
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
            System.out.println("---Could not add " + current + fiat);
            System.out.println(e.getMessage());
        }
    }

    public void refreshWalletAndTrades() {
        if (mode != Mode.LIVE) return;
        for (AssetBalance balance : BinanceAPI.getAccount().getBalances()) {
            if (balance.getFree().matches("0\\.0+")) continue;
            if (balance.getAsset().equals(fiat)) {
                final double amount = Double.parseDouble(balance.getFree());
                if (account.getFiat() != amount) {
                    System.out.println("---Refreshed " + balance.getAsset() + " from " + Formatter.formatDecimal(account.getFiat()) + " to " + amount);
                    System.out.println(balance.getLocked());
                    account.setFiat(amount);
                }
                continue;
            }
            for (Currency currency : currencies) {
                if ((balance.getAsset() + fiat).equals(currency.getPair())) {
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
                    System.out.println("\nTotal wallet value: " + Formatter.formatDecimal(instance.account.getTotalValue()) + " " + instance.getFiat());
                    System.out.println(Formatter.formatDecimal(instance.account.getFiat()) + " " + instance.getFiat());
                    for (Map.Entry<Currency, Double> entry : instance.account.getWallet().entrySet()) {
                        if (entry.getValue() != 0) {
                            System.out.println(Formatter.formatDecimal(entry.getValue()) + " " + entry.getKey().getPair().replace(instance.getFiat(), "")
                                    + " (" + Formatter.formatDecimal(entry.getKey().getPrice() * entry.getValue()) + " " + instance.getFiat() + ")");
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

    public ConfigData getConfig() {
        return config.getData();
    }

    public String getFiat() {
        return fiat;
    }

    public enum Mode {
        LIVE,
        SIMULATION,
        BACKTESTING;
    }
}
