package modes;

import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import system.ConfigSetup;
import system.Formatter;
import trading.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;


public final class Live {
    private static LocalAccount localAccount;
    private static final List<Currency> currencies = new ArrayList<>();
    private static final File credentialsFile = new File("credentials.txt");

    private Live() {
        throw new IllegalStateException("Utility class");
    }

    public static LocalAccount getAccount() {
        return localAccount;
    }

    public static List<Currency> getCurrencies() {
        return currencies;
    }

    public static void close() {
        for (Currency currency : currencies) {
            try {
                currency.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void init() {
        boolean fileFailed = true;
        if (credentialsFile.exists()) {
            //TODO: This try block doesn't work
            try {
                final List<String> strings = Files.readAllLines(credentialsFile.toPath());
                if (!strings.get(0).matches("\\*+")) {
                    localAccount = new LocalAccount(strings.get(0), strings.get(1));
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
            localAccount = new LocalAccount(apiKey, apiSecret);
        }

        //This doesn't seem to do anything
        //localAccount.getRealAccount().setUpdateTime(1);
        System.out.println("Can trade: " + localAccount.getRealAccount().isCanTrade());
        System.out.println(localAccount.getMakerComission() + " Maker commission.");
        System.out.println(localAccount.getBuyerComission() + " Buyer commission");
        System.out.println(localAccount.getTakerComission() + " Taker comission");
        BuySell.setAccount(localAccount);

        //TODO: Open price for existing currencies
        String current = "";
        try {
            List<String> addedCurrencies = new ArrayList<>();
            for (AssetBalance balance : localAccount.getRealAccount().getBalances()) {
                if (balance.getFree().matches("0\\.0+")) continue;
                if (ConfigSetup.getCurrencies().contains(balance.getAsset())) {
                    current = balance.getAsset();
                    Currency balanceCurrency = new Currency(current);
                    currencies.add(balanceCurrency);
                    addedCurrencies.add(current);
                    double amount = Double.parseDouble(balance.getFree());
                    localAccount.getWallet().put(balanceCurrency, amount);
                    double price = Double.parseDouble(CurrentAPI.get().getPrice(current + ConfigSetup.getFiat()).getPrice());
                    Optional<String> lotSize = CurrentAPI.get().getExchangeInfo().getSymbolInfo(current + ConfigSetup.getFiat()).getFilters().stream().filter(f -> FilterType.LOT_SIZE == f.getFilterType()).findFirst().map(f1 -> f1.getMinQty());
                    Optional<String> minNotational = CurrentAPI.get().getExchangeInfo().getSymbolInfo(current + ConfigSetup.getFiat()).getFilters().stream().filter(f -> FilterType.MIN_NOTIONAL == f.getFilterType()).findFirst().map(SymbolFilter::getMinNotional);
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
                    localAccount.getActiveTrades().add(trade);
                    balanceCurrency.setActiveTrade(trade);
                    System.out.println("Added an active trade of " + balance.getFree() + " " + current + " at " + Formatter.formatDecimal(trade.getEntryPrice()) + " based on existing balance in account");
                }
            }
            localAccount.setStartingValue(localAccount.getTotalValue());
            for (String arg : ConfigSetup.getCurrencies()) {
                if (!addedCurrencies.contains(arg)) {
                    current = arg;
                    currencies.add(new Currency(current));
                }
            }
        } catch (Exception e) {
            System.out.println("---Could not add " + current + ConfigSetup.getFiat());
            System.out.println(e.getMessage());
        }
    }

    public static void refreshWalletAndTrades() {
        for (AssetBalance balance : localAccount.getRealAccount().getBalances()) {
            if (balance.getFree().matches("0\\.0+")) continue;
            if (balance.getAsset().equals(ConfigSetup.getFiat())) {
                final double amount = Double.parseDouble(balance.getFree());
                if (localAccount.getFiat() != amount) {
                    System.out.println("---Refreshed " + balance.getAsset() + " from " + Formatter.formatDecimal(localAccount.getFiat()) + " to " + amount);
                    System.out.println(balance.getLocked());
                    localAccount.setFiat(amount);
                }
                continue;
            }
            for (Currency currency : currencies) {
                if ((balance.getAsset() + ConfigSetup.getFiat()).equals(currency.getPair())) {
                    final double amount = Double.parseDouble(balance.getFree());
                    if (!localAccount.getWallet().containsKey(currency)) {
                        System.out.println("---Refreshed " + currency.getPair() + " from 0 to " + balance.getFree());
                        localAccount.getWallet().replace(currency, amount);
                    }
                    if (localAccount.getWallet().get(currency) != amount) {
                        System.out.println("---Refreshed " + currency.getPair() + " from " + Formatter.formatDecimal(localAccount.getWallet().get(currency)) + " to " + balance.getFree());
                        System.out.println(balance.getLocked());
                        localAccount.getWallet().replace(currency, amount);
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
    }
}
