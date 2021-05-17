package com.mqtt.client.paho.subscriber;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        /*
         * jar dirPicSubscriber.jar <broker_ip> <broker_port> <broker_topic>
         * <image_save_directory> <key_store_directory> <user_name> <user_password>
         */

        // Inputs for meant use.
        String brokerParameter = args[0];
        String portParameter = args[1];
        String channelParameter = args[2];
        String imageDirectory = args[3];
        String keyStoreParameter = args[4];
        String userNameParameter = args[5];
        String userPasswordParameter = args[6];

        // Test inputs.
        // String brokerParameter = "localhost";
        // String portParameter = "1883";
        // String channelParameter = "test";
        // String imageDirectory = "/home/sht/dirPic/testImageSaveDirectory/";
        // String keyStoreDirectory = "test";
        // String userName = "test";
        // String userPassword = "test";

        try {

            String brokerDetails = "tcp://" + brokerParameter + ":" + portParameter;

            MqttClient client = new MqttClient(brokerDetails, "idSubscriber" + MqttClient.generateClientId());

            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable throwable) {

                    System.out.println("Connection lost!");

                }

                @Override
                public void messageArrived(String t, MqttMessage m) throws Exception {

                    System.out.println("Message received! Assessing bytes..");

                    // tries to save image from received bytes
                    try {

                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                                .ofPattern("/yyyy/MM/dd/ dd.MM.yyyy_HH:mm:ss");

                        LocalDateTime currentTime = LocalDateTime.now();

                        String fileName = dateTimeFormatter.format(currentTime) + ".jpeg";

                        String filePath = imageDirectory + fileName;

                        byte[] data = m.getPayload();

                        ByteArrayInputStream bis = new ByteArrayInputStream(data);

                        BufferedImage bImage2 = ImageIO.read(bis);

                        File file = new File(filePath);

                        file.getParentFile().mkdirs();

                        file.createNewFile();

                        ImageIO.write(bImage2, "jpeg", file);

                        System.out.println("Image created!");

                    } catch (Exception e) {

                        System.out.println("Could not create image file from bytes!");

                    }

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                    System.out.println(new String("Delivered!"));

                }

            });

            MqttConnectOptions options = new MqttConnectOptions();

            // best to use will on a different topic to avoid intermingling with images'
            // bytes. options.setWill("test", "Disconnect".getBytes(), 2, true);

            options.setUserName(userNameParameter);

            options.setPassword(userPasswordParameter.toCharArray());

            options.setCleanSession(false);

            client.connect(options);

            client.subscribe(channelParameter);

            System.out.println("Subscribed!");

            // client.disconnect();

        } catch (MqttException e) {

            System.out.println("Not Subscribed!");

        }
    }
}
