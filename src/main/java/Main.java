import Indicators.EMA;
import Indicators.MACD;
import Indicators.RSI;
import Indicators.SMA;
import com.google.gson.JsonObject;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceCandlestick;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws BinanceApiException {
        try {
            Currency currency = new Currency("BTC");
            long startTime = System.nanoTime();
            List<BinanceCandlestick> history = currency.getCandles(250);
            RSI rsi = new RSI(history, 14);
            SMA sma = new SMA(history, 9);
            EMA ema = new EMA(history, 9, false);
            MACD macd = new MACD(history, 12, 26, 9);
            long endTime = System.nanoTime();
            double time = (endTime - startTime) / 1.e6;
            System.out.println("Setup took: " + time + " ms");

            /*long startTime = System.nanoTime();
            RSI rsi = new RSI(currency.getCandles(250), 14);
            SMA sma = new SMA(currency.getCandles(15), 9);
            EMA ema = new EMA(currency.getCandles(250), 9, false);
            MACD macd = new MACD(currency.getCandles(250), 12, 26, 9);
            long endTime = System.nanoTime();
            double time = (endTime - startTime) / 1.e6;
            System.out.println("Setup took: " + time + " ms");*/

            while (true) {
                double newPrice = currency.getPrice();
                System.out.println("RSI: " + rsi.getTemp(newPrice));
                System.out.println("SMA: " + sma.getTemp(newPrice));
                System.out.println("EMA: " + ema.getTemp(newPrice));
                System.out.println("MACD: " + macd.getTemp(newPrice));
            }
        } catch (
                BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        //Account toomas = new Account("Investor Toomas", 1000);

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter your API Key: ");
        CurrentAPI.get().setApiKey(sc.nextLine());
        System.out.println("Enter your Secret Key: ");
        CurrentAPI.get().setSecretKey(sc.nextLine());
        JsonObject account = CurrentAPI.get().account();
        //Connection with Binance API and sout-ing some info.
        System.out.println("Maker Commission: " + account.get("makerCommission").getAsBigDecimal());
        System.out.println("Taker Commission: " + account.get("takerCommission").getAsBigDecimal());
        System.out.println("Buyer Commission: " + account.get("buyerCommission").getAsBigDecimal());
        System.out.println("Seller Commission: " + account.get("sellerCommission").getAsBigDecimal());
        System.out.println("Can Trade: " + account.get("canTrade").getAsBoolean());
        System.out.println("Can Withdraw: " + account.get("canWithdraw").getAsBoolean());
        System.out.println("Can Deposit: " + account.get("canDeposit").getAsBoolean());
    }
}
