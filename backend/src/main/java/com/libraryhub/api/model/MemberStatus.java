package com.libraryhub.api.model;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
public enum MemberStatus {
    ACTIVE("Active"), SUSPENDED("Suspended");
    private final String value;
    MemberStatus(String value){this.value=value;}
    @JsonValue public String getValue(){return value;}
    @JsonCreator public static MemberStatus fromValue(String value){
        for(MemberStatus status:values()) if(status.value.equalsIgnoreCase(value)||status.name().equalsIgnoreCase(value)) return status;
        throw new IllegalArgumentException("Member status must be Active or Suspended");
    }
}
