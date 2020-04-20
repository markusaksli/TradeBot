package experimental;

import com.webcerebrium.binance.api.BinanceApiException;
import trading.Mode;

import java.io.FileWriter;
import java.io.IOException;

public class BacktestLiveData {
    public static void main(String[] args) {
        Mode.set(Mode.SIMULATION);
        ComparisonCurrency backtestCurrency = null;
        try {
            backtestCurrency = new ComparisonCurrency("C:\\Users\\marku\\Google Drive\\UT\\OOP\\TradeBot\\backtesting\\BTCUSDT_1587415200000_1587416105694.txt");
        } catch (BinanceApiException | IOException e) {
            e.printStackTrace();
        }

        try (FileWriter writer = new FileWriter("backtest_log.txt")) {
            writer.write(backtestCurrency.getRawLog());
            writer.write("\n\n\n\n");
            writer.write(backtestCurrency.getLog());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
