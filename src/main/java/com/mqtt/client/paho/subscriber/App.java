package com.mqtt.client.paho.subscriber;

import com.mqtt.client.paho.subscriber.service.DirPicMqttClient;
import com.mqtt.client.paho.subscriber.service.DirPicSslContext;
import com.mqtt.client.paho.subscriber.service.Properties;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.MqttException;

public final class App {

    final static String ALIAS_CA_CRT_PEM = "ca_crt.pem";
    final static String ALIAS_CRT_PEM = "client_crt.pem";
    final static String ALIAS_KEY_PEM = "client_key.pem";

    private final static String MQTT_PROTOCOL_CONNECTION = "ssl://";

    final static String NOTIFICATION_CONNECTION_PREPARE = "\nPreparing connection the server...\n";
    final static String NOTIFICATION_CONNECTION_ATTEMPT = "\nConnecting...\n";
    final static String NOTIFICATION_CONNECTION_ESTABLISHED = "\nConnected!\n";

    final static String NOTIFICATION_SUBSCRIBE_ATTEMPT = "\nSubscribing...\n";

    final static String NOTIFICATION_SUCCESS_SUBSCRIPTION = "\nWaiting for publications!\n";

    final static String NOTIFICATION_ERROR_CONNECTION = "\nConnection Issues!\n";
    final static String NOTIFICATION_ERROR_RETRY = "\nRetrying...\n";
    final static String NOTIFICATION_ERROR_PARAMETER = "\nParameter Issues!\n";
    final static String NOTIFICATION_ERROR_SUBSCRIPTION = "\nSubscription Issues!\n";
    final static String NOTIFICATION_ERROR_IO = "\nI/O problem!\n";
    final static String NOTIFICATION_ERROR_GENERAL = "\nGeneral Exception!\n";

    final static String NOTIFICATION_ERROR_INPUT_BOUNDARIES = "\nInputs are out of bound!\n";
    final static String NOTIFICATION_ERROR_INPUT_ILLEGAL = "\nIllegal inputs!\n";

    public static void main(String[] args) {
        /*
         * jar dirPicSubscriber.jar <broker_ip> <broker_port> <broker_topic>
         * <image_save_directory> <key_store_directory> <user_name> <user_password>
         * <ca_password>
         */

        try {

            Properties props = new Properties(args[0]);

            subscribeToChannel(props);

        } catch (IndexOutOfBoundsException e) {

            System.out.println(NOTIFICATION_ERROR_INPUT_BOUNDARIES + e.getStackTrace());

            System.out.println(NOTIFICATION_ERROR_RETRY);

            return;

        } catch (IllegalArgumentException e) {

            System.out.println(NOTIFICATION_ERROR_INPUT_ILLEGAL + e.getStackTrace());

            System.out.println(NOTIFICATION_ERROR_RETRY);

            return;

        }
    }

    private static void subscribeToChannel(Properties props) {

        try {

            System.out.println(NOTIFICATION_CONNECTION_PREPARE);

            String brokerDetails = MQTT_PROTOCOL_CONNECTION + props.getBrokerIp() + ":" + props.getBrokerPort();

            DirPicSslContext sslContext = new DirPicSslContext(props);

            SSLSocketFactory socketFactory = sslContext.getSslSocketFactory();

            DirPicMqttClient client = new DirPicMqttClient(props, socketFactory, brokerDetails);

            System.out.println(NOTIFICATION_CONNECTION_ATTEMPT);

            client.connect();

            System.out.println(NOTIFICATION_CONNECTION_ESTABLISHED);

            System.out.println(NOTIFICATION_SUBSCRIBE_ATTEMPT);

            client.subscribe(props.getChannelName());

            System.out.println(NOTIFICATION_SUCCESS_SUBSCRIPTION);

        } catch (MqttException e) {

            // e.printStackTrace();

            System.out.println(NOTIFICATION_ERROR_SUBSCRIPTION + NOTIFICATION_ERROR_CONNECTION + e.getStackTrace());

            System.out.println(NOTIFICATION_ERROR_RETRY);

            subscribeToChannel(props);

        } catch (Exception e) {

            // e.printStackTrace();

            System.out.println(NOTIFICATION_ERROR_GENERAL + e.getStackTrace());

            System.out.println(NOTIFICATION_ERROR_RETRY);

            subscribeToChannel(props);
        }
    }
}
