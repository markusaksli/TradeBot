import java.util.List;

public class Strategy {

    private List<StrategyListener> listeners;
    private boolean shouldRun;

    public void listen(StrategyListener sl) {
        listeners.add(sl);
    }

    private void buySignal() {
        listeners.forEach(StrategyListener::onBuySignal);
    }

    private void sellSignal() {
        listeners.forEach(StrategyListener::onSellSignal);
    }

    private void work() {
        while (shouldRun) {
            if (true) {
                buySignal();
            }

            if (false) {
                sellSignal();
            }
        }
    }

    public void init() {
        shouldRun = true;
    }

    public void stop() {
        shouldRun = false;
    }
}
