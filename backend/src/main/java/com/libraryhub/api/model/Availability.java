package com.libraryhub.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Availability {
    AVAILABLE("Available"),
    ON_LOAN("On Loan");

    private final String value;
    Availability(String value) { this.value = value; }
    @JsonValue public String getValue() { return value; }

    @JsonCreator
    public static Availability fromValue(String value) {
        for (Availability item : values()) {
            if (item.value.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value.replace(" ", "_"))) return item;
        }
        throw new IllegalArgumentException("Availability must be 'Available' or 'On Loan'");
    }
}
