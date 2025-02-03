package mb.enums;

import java.util.Arrays;

public enum ElectionType {
    NONE("none"),
    RING("ring"),
    BULLY("bully"),
    RAFT("raft");

    private final String stringValue;

    ElectionType(String value) {
        this.stringValue = value;
    }

    public String getStringValue() {
        return this.stringValue;
    }

    public static ElectionType fromString(String value) {
        return Arrays.stream(ElectionType.values())
                .filter(electionType -> electionType.getStringValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No election type with value " + value));
    }
}
