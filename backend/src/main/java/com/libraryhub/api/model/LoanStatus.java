package com.libraryhub.api.model;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
public enum LoanStatus {
    BORROWED("Borrowed"), RETURNED("Returned"), OVERDUE("Overdue");
    private final String value;
    LoanStatus(String value){this.value=value;}
    @JsonValue public String getValue(){return value;}
    @JsonCreator public static LoanStatus fromValue(String value){
        for(LoanStatus status:values()) if(status.value.equalsIgnoreCase(value)||status.name().equalsIgnoreCase(value)) return status;
        throw new IllegalArgumentException("Invalid loan status");
    }
}
