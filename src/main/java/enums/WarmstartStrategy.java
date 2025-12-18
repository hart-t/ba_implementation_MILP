package enums;

public enum WarmstartStrategy {
    STD("STD"),
    VS("VS"),
    VH("VH");

    private final String name;

    WarmstartStrategy(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static WarmstartStrategy fromString(String str) {
        if (str == null) {
            return null;
        }
        for (WarmstartStrategy strategy : WarmstartStrategy.values()) {
            if (strategy.name.equalsIgnoreCase(str)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown strategy: " + str);
    }
}
