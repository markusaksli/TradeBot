public class CurrencyBot implements StrategyListener {

    @Override
    public void subscribe(Strategy s) {
        s.listen(this);
    }

    @Override
    public void onBuySignal() {
//set buy order
    }

    @Override
    public void onSellSignal() {
//close orders
    }
}
