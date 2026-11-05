package com.maranata.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WhatsappWebhookPayloadDTO {

    @JsonProperty("event")
    private String event;

    @JsonProperty("data")
    private WhatsappDataDTO data;

    public WhatsappWebhookPayloadDTO() {
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public WhatsappDataDTO getData() {
        return data;
    }

    public void setData(WhatsappDataDTO data) {
        this.data = data;
    }

    public static class WhatsappDataDTO {

        @JsonProperty("key")
        private WhatsappKeyDTO key;

        @JsonProperty("message")
        private WhatsappMessageDTO message;

        public WhatsappDataDTO() {
        }

        public WhatsappKeyDTO getKey() {
            return key;
        }

        public void setKey(WhatsappKeyDTO key) {
            this.key = key;
        }

        public WhatsappMessageDTO getMessage() {
            return message;
        }

        public void setMessage(WhatsappMessageDTO message) {
            this.message = message;
        }

        public static class WhatsappKeyDTO {

            @JsonProperty("remoteJid")
            private String remoteJid;

            public WhatsappKeyDTO() {
            }

            public String getRemoteJid() {
                return remoteJid;
            }

            public void setRemoteJid(String remoteJid) {
                this.remoteJid = remoteJid;
            }
        }

        public static class WhatsappMessageDTO {

            @JsonProperty("conversation")
            private String conversation;

            public WhatsappMessageDTO() {
            }

            public String getConversation() {
                return conversation;
            }

            public void setConversation(String conversation) {
                this.conversation = conversation;
            }
        }
    }
}
