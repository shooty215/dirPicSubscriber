package com.mqtt.client.paho.subscriber.service;

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Properties {

    private String filePath, brokerIp, brokerPort, channelName, cameraPath, storagePath, keyStorePath, brokerAuthUser,
            brokerAuthPassword, rsaPublicKey, rsaPrivateKey, aesKey;

    public Properties(String filePath) {
        this.filePath = filePath;
        this.readFile();
    }

    public String getFilePath() {
        return filePath;
    }

    public String getBrokerIp() {
        return brokerIp;
    }

    public String getBrokerPort() {
        return brokerPort;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getCameraPath() {
        return cameraPath;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getBrokerAuthUser() {
        return brokerAuthUser;
    }

    public String getBrokerAuthPassword() {
        return brokerAuthPassword;
    }

    public String getRsaPublicKey() {
        return rsaPublicKey;
    }

    public String getRsaPrivateKey() {
        return rsaPrivateKey;
    }

    public String getAesKey() {
        return aesKey;
    }

    private void readFile() {
        JSONParser parser = new JSONParser();

        try {
            Object object = parser.parse(new FileReader(this.filePath));

            // convert Object to JSONObject
            JSONObject jsonObject = (JSONObject) object;

            // Reading the String
            this.brokerIp = (String) jsonObject.get("brokerIp");
            this.brokerPort = (String) jsonObject.get("brokerPort");
            this.channelName = (String) jsonObject.get("channelName");
            this.cameraPath = (String) jsonObject.get("cameraPath");
            this.storagePath = (String) jsonObject.get("storagePath");
            this.keyStorePath = (String) jsonObject.get("keyStorePath");
            this.brokerAuthUser = (String) jsonObject.get("brokerAuthUser");
            this.brokerAuthPassword = (String) jsonObject.get("brokerAuthPassword");
            this.rsaPublicKey = (String) jsonObject.get("rsaPublicKey");
            this.rsaPrivateKey = (String) jsonObject.get("rsaPrivateKey");
            this.aesKey = (String) jsonObject.get("aesKey");

            jsonObject = null;

        } catch (FileNotFoundException fe) {
            fe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
