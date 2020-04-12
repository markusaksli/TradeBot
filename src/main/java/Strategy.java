import Indicators.MACD;
import Indicators.RSI;

public class Strategy {

    public static void update(Currency currency) {
        double rsi = currency.getRsi().getTemp(currency.getPrice());
        double lastMACD = currency.getLastMACD();
        double tempMACD = currency.getMacd().getTemp(currency.getPrice());
        if (checkRSI(rsi) + checkMACD(lastMACD, tempMACD) == 2) {
            System.out.println(currency.getSymbol() + " trade opened due to RSI of " + rsi + " and MACD growing from " + lastMACD + " to " + tempMACD);
            BuySell.open(currency, nextAmount() / currency.getPrice());
        }
    }

    private static int checkRSI(double rsi) {
        if (rsi < 30) return 1;
        return 0;
    }

    private static int checkMACD(double lastTick, double temp) {
        if (temp > lastTick) return 1;
        return 0;
    }

    private static double nextAmount() {
        return BuySell.getAccount().getFiatValue() * 0.05;
    }
}
