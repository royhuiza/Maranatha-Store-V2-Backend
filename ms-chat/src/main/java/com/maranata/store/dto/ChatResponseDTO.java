package com.maranata.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatResponseDTO {

    @JsonProperty("reply")
    private String reply;

    public ChatResponseDTO() {
    }

    public ChatResponseDTO(String reply) {
        this.reply = reply;
    }

    @JsonProperty("reply")
    public String getReply() {
        return reply;
    }

    @JsonProperty("reply")
    public void setReply(String reply) {
        this.reply = reply;
    }
}
