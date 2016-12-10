package edu.nyu.networks.iot.server.controller;

import java.util.List;
import java.lang.reflect.Type;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * Message reader class takes message as string and returns a JSON object.
 *
 * The class also provides several static convenience methods for interpreting the incoming messages.
 * @author Wesley Painter
 *
 */
public class MessageReader {

    static Type listType = new TypeToken<List<String>>() {}.getType();

    static JsonObject readMessage(String message) {
        JsonObject messageObject = new Gson().fromJson(message, JsonObject.class);
        return messageObject;
    }

    static Boolean isKeepAlive(JsonObject messageObject) {
        if (messageObject.get("i_m_e_i") != null) {
            return true;
        }
        return false;
    }

    static List<String> extractSensorList(JsonObject messageObject) {
        List<String> sensors = new Gson().fromJson(messageObject.get("avail_sensors"), listType);
        return sensors;
    }
}
