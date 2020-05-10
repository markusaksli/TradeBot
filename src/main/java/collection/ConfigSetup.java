package collection;

import modes.Backtesting;
import modes.Collection;
import modes.Simulation;
import indicators.MACD;
import indicators.RSI;
import trading.BuySell;

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
        int items = 0;
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
                        items++;
                        break;
                    case "RSI positive side minimum":
                        RSIPosMin = Integer.parseInt(linepieces[1]);
                        items++;
                        break;
                    case "RSI positive side maximum":
                        RSIPosMax = Integer.parseInt(linepieces[1]);
                        items++;
                        break;
                    case "RSI negative side minimum":
                        RSINegMin = Integer.parseInt(linepieces[1]);
                        items++;
                        break;
                    case "RSI negative side maximum":
                        RSINegMax = Integer.parseInt(linepieces[1]);
                        items++;
                        break;
                    case "Collection mode chunk size(minutes)":
                        minutesForCollection = Long.parseLong(linepieces[1]);
                        items++;
                        break;
                    case "Simulation mode starting value":
                        startingValue = Integer.parseInt(linepieces[1]);
                        items++;
                        break;
                    case "Simulation mode currencies":
                        currencies = linepieces[1].split(", ");
                        items++;
                        break;
                    case "Percentage of money per trade":
                        moneyPerTrade = Double.parseDouble(linepieces[1]);
                        items++;
                        break;
                }
            }
            if (items < 9) { //9 is the number of configuration elements in the file.
                throw new ConfigException("Config file has some missing elements.");
            } else if (items > 9) {
                throw new ConfigException("Config file has too many elements.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfigException e) {
            e.printStackTrace();
            System.exit(0);
        }


        //COLLECTION MODE
        //When entering collection mode, how big chuncks do you
        //want to create
        Collection.setMinutesForCollection(getMinutesForCollection());

        //SIMULATION
        Simulation.setCurrencyArr(getCurrencies());
        Simulation.setStartingValue(getStartingValue()); //How much money does the simulated acc start with.
        //The currencies that the simulation MODE will trade with.

        //TRADING
        BuySell.setMoneyPerTrade(getMoneyPerTrade()); //How many percentages of the money you have currently
        //will the program put into one trade.

        //BACKTESTING
        Backtesting.setStartingValue(getStartingValue());

        //INDICATORS

        //MACD
        MACD.setChange(getMACDChange()); //How much change does the program need in order to give a positive signal from MACD

        //RSI
        RSI.setPositiveMin(getRSIPosMin()); //When RSI reaches this value, it returns 2 as a signal.
        RSI.setPositivseMax(getRSIPosMax()); //When RSI reaches this value, it returns 1 as a signal.
        RSI.setNegativeMin(getRSINegMin()); //When RSI reaches this value, it returns -1 as a signal.
        RSI.setNegativeMax(getRSINegMax()); //When RSI reaches this value it returns -2 as a signal.
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
