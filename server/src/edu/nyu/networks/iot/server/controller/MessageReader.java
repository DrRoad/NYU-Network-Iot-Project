package edu.nyu.networks.iot.server.controller;

import java.util.List;
import java.lang.reflect.Type;
import java.util.zip.GZIPInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * Message reader class takes message as string and returns a JSON object.
 *
 * The class also provides several static convenience methods for interpreting the incoming messages.
 * @author Wesley Painter, Wenliang Zhao
 *
 */
public class MessageReader {

    static Type listType = new TypeToken<List<String>>() {}.getType();

    public static String decompress(String str) throws Exception {
        if (str == null || str.length() == 0) {
            return str;
        }
       //
        //
        // System.out.println("Input String length : " + str.length());
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(str.getBytes("UTF-8")));
        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
        String outStr = "";
        String line;
        while ((line=bf.readLine())!=null) {
            outStr += line;
        }
        //System.out.println("Output String lenght : " + outStr.length());
        return outStr;
    }

    static JsonObject readMessage(String message) {
    //	System.out.println("Read message (MESSAGE READER):"+message);
        JsonObject messageObject = new Gson().fromJson(message, JsonObject.class);
       	String imei = messageObject.get("i_m_e_i").getAsString();
        if (!isData(messageObject)) {
            Double lat = messageObject.get("lat").getAsDouble();
            Double lon = messageObject.get("lon").getAsDouble();
            Double noise = messageObject.get("noise").getAsDouble();
            Location l = new Location(lat, lon);
            Value v = new Value(l, noise);
            Controller.sendToDB(imei, v);
        }
       // System.out.println("Message Reader returning:"+messageObject.toString());
        return messageObject;
    }

    static Boolean isData(JsonObject messageObject) {
        if (messageObject.get("is_data").getAsString() == "True") {
            return true;
        }
        return false;
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
