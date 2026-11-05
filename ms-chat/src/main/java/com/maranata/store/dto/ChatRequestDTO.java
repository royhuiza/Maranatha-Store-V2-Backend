package com.maranata.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatRequestDTO {

    @JsonProperty("message")
    private String message;

    public ChatRequestDTO() {
    }

    public ChatRequestDTO(String message) {
        this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }
}
