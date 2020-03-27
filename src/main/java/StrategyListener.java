public interface StrategyListener {

    void subscribe(Strategy s);

    void onBuySignal();

    void onSellSignal();
}
