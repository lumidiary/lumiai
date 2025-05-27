package com.lumidiary.ai.queue;

public class QueueMessage {
    private String channel;
    private String data; // JSON 문자열로 받음

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
