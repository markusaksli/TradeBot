package experimental;

import com.webcerebrium.binance.api.BinanceApiException;
import trading.Mode;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class CollectLiveData {
    public static void main(String[] args) {

        Mode.set(Mode.SIMULATION);
        ComparisonCurrency klineCurrency = null;
        try {
            klineCurrency = new ComparisonCurrency("BTC", true);
        } catch (BinanceApiException e) {
            e.printStackTrace();
        }

        ComparisonCurrency aggCurrency = null;
        try {
            aggCurrency = new ComparisonCurrency("BTC", false);
        } catch (BinanceApiException e) {
            e.printStackTrace();
        }

        Scanner sc = new Scanner(System.in);
        System.out.println("write log to log results");
        while (true) {
            String string = sc.nextLine();
            if (string.equals("log")) {
                try (FileWriter writer = new FileWriter("kline_log.txt")) {
                    writer.write(klineCurrency.getRawLog());
                    writer.write("\n\n\n\n");
                    writer.write(klineCurrency.getLog());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try (FileWriter writer = new FileWriter("agg_log.txt")) {
                    writer.write(aggCurrency.getRawLog());
                    writer.write("\n\n\n\n");
                    writer.write(aggCurrency.getLog());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("write log to log results");
            }
        }
    }
}
