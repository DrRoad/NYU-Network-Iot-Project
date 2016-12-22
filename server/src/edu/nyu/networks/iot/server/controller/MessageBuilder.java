package edu.nyu.networks.iot.server.controller;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
/**
 *
 * @author Wesley Painter
 *
 */
public class MessageBuilder {

    private String messageType;
    private Boolean compress;
    private Integer frequency;
    private String command;
    private String[] interval;
    private List<String> sensors;
    private Long period;

    MessageBuilder(String messageType) {
        this.messageType = messageType;
    }

    public MessageBuilder withCompress(Boolean flag) {
        this.compress = flag;
        return this;
    }

    public MessageBuilder withFrequency(Integer frequency) {
        this.frequency = frequency;
        return this;
    }

    public MessageBuilder withCommand(String command) {
        this.command = command;
        return this;
    }

    public MessageBuilder withPeriod(Long period) {
        this.period = period;
        return this;
    }

    public MessageBuilder withSensor(String sensor) {
        if (sensors == null) {
            sensors = new ArrayList<String>();
            sensors.add(sensor);
        } else if (sensors.indexOf(sensor) == -1) {
            sensors.add(sensor);
        }

        return this;
    }

    public MessageBuilder withTimeInterval(String startTime, String endTime) {
        this.interval = new String[2];
        this.interval[0] = startTime;
        this.interval[1] = endTime;
        return this;
    }

    public String build() {
        Gson object = new Gson();
        String body = object.toJson(this);
        Integer header = body.length();
        String message = header.toString() + "\r\n" + body + "\r\n";

        return message;
    }

}
