public class Strategy {

    public static void update(Currency currency) {
        double amount = nextAmount();
        if (amount == 0) {
            return; //If no fiat is available, we cant trade
        }
        double rsi = currency.getRsi().getTemp(currency.getPrice());
        double lastMACD = currency.getLastMACD();
        double tempMACD = currency.getMacd().getTemp(currency.getPrice());
        if (checkRSI(rsi) + checkMACD(lastMACD, tempMACD) >= 2) { //As we add more indicators we can use this to open a trade if we get a confluence of 2 or more
            String explanation = "Trade opened due to RSI of " + rsi + " and unclosed MACD histogram growing by " + Formatter.formatPercent((tempMACD - lastMACD) / lastMACD) + " in current candle";
            System.out.println("---" + currency.getCoin() + " " + explanation);
            BuySell.open(currency, amount / currency.getPrice(), explanation);
        }
    }

    //Check if RSI is below 30
    private static int checkRSI(double rsi) {
        if (rsi < 30) return 1;
        return 0;
    }

    //Check if MACD histogram is growing by at least 10%
    private static int checkMACD(double lastTick, double temp) {
        if ((temp - lastTick) / lastTick > 0.1) return 1;
        return 0;
    }

    //TODO: Better risk management algorithm
    //Calculate trade USDT amount based on portfolio size and current liquidity in USDT (simple 10% risk allocation)
    private static double nextAmount() {
        return Math.min(BuySell.getAccount().getFiat(), BuySell.getAccount().getTotalValue() * 0.10);
    }
}
