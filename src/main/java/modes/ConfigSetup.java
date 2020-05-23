package modes;

import indicators.MACD;
import indicators.RSI;
import trading.BuySell;
import trading.Trade;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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
    private double trailingSL;
    private double takeP;

    private static String setup;

    public ConfigSetup() {
        readFile();
    }

    public static String getSetup() {
        return setup;
    }

    public void readFile() {
        int items = 0;
        File file = new File("config.txt");
        try (FileReader reader = new FileReader(file);
             BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] linepieces = line.strip().split(":");
                items++;
                switch (linepieces[0]) {
                    case "MACD change indicator":
                        MACDChange = Double.parseDouble(linepieces[1]);
                        break;
                    case "RSI positive side minimum":
                        RSIPosMin = Integer.parseInt(linepieces[1]);
                        break;
                    case "RSI positive side maximum":
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
                    case "Trailing SL":
                        trailingSL = Double.parseDouble(linepieces[1]);
                        break;
                    case "Take profit":
                        takeP = Double.parseDouble(linepieces[1]);
                        break;
                }
            }
            if (items < 11) { //11 is the number of configuration elements in the file.
                throw new ConfigException("Config file has some missing elements.");
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

        //LIVE
        Live.setCurrencyArr(getCurrencies());

        //SIMULATION
        Simulation.setCurrencyArr(getCurrencies());
        Simulation.setStartingValue(getStartingValue()); //How much money does the simulated acc start with.
        //The currencies that the simulation MODE will trade with.

        //TRADING
        BuySell.setMoneyPerTrade(getMoneyPerTrade()); //How many percentages of the money you have currently
        Trade.setTakeProfit(takeP);
        Trade.setTrailingSl(trailingSL);
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

        setup = toString();
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

    @Override
    public String toString() {
        return "MACD change indicator:" + MACDChange + "\n" +
                "RSI positive side minimum:" + RSIPosMin + "\n" +
                "RSI positive side maximum:" + RSIPosMax + "\n" +
                "RSI negative side minimum:" + RSINegMin + "\n" +
                "RSI negative side maximum:" + RSINegMax + "\n" +
                "Collection mode chunk size(minutes):" + minutesForCollection + "\n" +
                "Simulation mode starting value:" + startingValue + "\n" +
                "Percentage of money per trade:" + moneyPerTrade + "\n" +
                "Trailing SL:" + trailingSL + "\n" +
                "Take profit:" + takeP + "\n\n" +
                "Simulation mode currencies:" + Arrays.stream(currencies).map(currency -> currency + " ").collect(Collectors.joining()) + "\n";
    }
}
