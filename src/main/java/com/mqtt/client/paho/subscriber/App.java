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
import javax.imageio.ImageIO;

public final class App {
    private App() {
    }

    /**
     * Says hello to the world.
     *
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        /*
         * String brokerParameter = args[0]; String channelParameter = args[1];
         */
        String brokerParameter = "tcp://localhost:1883";
        String channelParameter = "test";

        try {

            MqttClient client = new MqttClient(brokerParameter, MqttClient.generateClientId());

            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable throwable) {
                }

                @Override
                public void messageArrived(String t, MqttMessage m) throws Exception {

                    System.out.println(new String(m.getPayload()));

                    byte[] data = m.getPayload();
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    BufferedImage bImage2 = ImageIO.read(bis);
                    File file = new File("/home/sht/output.jpg");
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                    ImageIO.write(bImage2, "jpg", file);
                    System.out.println("image created");

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                    System.out.println(new String("Delivered!"));

                }

            });
            MqttConnectOptions options = new MqttConnectOptions();

            // options.setWill("test", "Disconnect".getBytes(), 2, true);

            options.setUserName("userName");

            options.setPassword("password".toCharArray());

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
