package com.maranata.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SendTextRequestDTO {

    @JsonProperty("number")
    private String number;

    @JsonProperty("textMessage")
    private TextMessageDTO textMessage;

    public SendTextRequestDTO() {
    }

    public SendTextRequestDTO(String number, String text) {
        this.number = number;
        this.textMessage = new TextMessageDTO(text);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public TextMessageDTO getTextMessage() {
        return textMessage;
    }

    public void setTextMessage(TextMessageDTO textMessage) {
        this.textMessage = textMessage;
    }

    public static class TextMessageDTO {

        @JsonProperty("text")
        private String text;

        public TextMessageDTO() {
        }

        public TextMessageDTO(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
