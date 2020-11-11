package system;

public enum Mode {
    LIVE,
    SIMULATION,
    BACKTESTING,
    COLLECTION;

    private static Mode state;

    public static Mode get() {
        return state;
    }

    static void set(Mode state) {
        Mode.state = state;
    }
}
