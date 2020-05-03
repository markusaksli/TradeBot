package collection;

import java.io.*;
import java.util.*;

public class ConfigSetup {
    private double moneyPerTrade;
    private long minutesForCollection;
    private double startingValue;
    private String[] currencies;
    private double MACDChange;
    private int RSIPosMax;
    private int RSIPosMin;
    private int RSINegMax;
    private int RSINegMin;

    public ConfigSetup() {
        readFile();
    }

    public void readFile() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource("config.txt")).getFile());

        try (FileReader reader = new FileReader(file);
             BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] linepieces = line.strip().split(":");
                switch (linepieces[0]) {
                    case "MACD change indicator":
                        MACDChange = Double.parseDouble(linepieces[1]);
                        break;
                    case "RSI positive side minimum":
                        RSIPosMin = Integer.parseInt(linepieces[1]);
                        break;
                    case "RSI positivse side maximum":
                        RSIPosMax = Integer.parseInt(linepieces[1]);
                        break;
                    case "RSI negative side minimum":
                        RSINegMin = Integer.parseInt(linepieces[1]);
                        break;
                    case "RSI negative side maximum":
                        RSINegMax = Integer.parseInt(linepieces[1]);
                        break;
                    case "Collection mode chunk size(minutes)":
                        minutesForCollection = Long.parseLong(linepieces[1]);
                        break;
                    case "Simulation mode starting value":
                        startingValue = Integer.parseInt(linepieces[1]);
                        break;
                    case "Simulation mode currencies":
                        currencies = linepieces[1].split(", ");
                        break;
                    case "Percentage of money per trade":
                        moneyPerTrade = Double.parseDouble(linepieces[1]);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getMoneyPerTrade() {
        return moneyPerTrade;
    }

    public long getMinutesForCollection() {
        return minutesForCollection;
    }

    public double getStartingValue() {
        return startingValue;
    }

    public String[] getCurrencies() {
        return currencies;
    }

    public double getMACDChange() {
        return MACDChange;
    }

    public int getRSIPosMax() {
        return RSIPosMax;
    }

    public int getRSIPosMin() {
        return RSIPosMin;
    }

    public int getRSINegMax() {
        return RSINegMax;
    }

    public int getRSINegMin() {
        return RSINegMin;
    }
}
