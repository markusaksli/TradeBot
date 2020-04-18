public enum Mode {
    LIVE,
    SIMULATED,
    BACKTESTING,
    COLLECTION;

    private static Mode state;

    public static Mode get() {
        return state;
    }

    public static void set(Mode state) {
        Mode.state = state;
    }
}
