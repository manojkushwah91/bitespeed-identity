package com.bitespeed.identity;

import lombok.Data;
import java.util.List;

@Data
public class IdentifyResponse {

    private ContactPayload contact;

    @Data
    public static class ContactPayload {
        private Integer primaryContactId;
        private List<String> emails;
        private List<String> phoneNumbers;
        private List<Integer> secondaryContactIds;
    }
}
