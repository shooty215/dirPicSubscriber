package com.mqtt.client.paho.subscriber;

import com.mqtt.client.paho.subscriber.service.DirPicMqttClient;
import com.mqtt.client.paho.subscriber.service.Factory;

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

            // Inputs for meant use.
            final String brokerParameter = args[0];
            final String portParameter = args[1];
            final String channelParameter = args[2];
            final String imageDirectory = args[3];
            final String keyStoreParameter = args[4];
            final String userNameParameter = args[5];
            final String userPasswordParameter = args[6];
            final String caPassword = args[7];

            final String serverCa = keyStoreParameter + ALIAS_CA_CRT_PEM;
            final String clientCrt = keyStoreParameter + ALIAS_CRT_PEM;
            final String clientKey = keyStoreParameter + ALIAS_KEY_PEM;
            final String keyPwd = caPassword;

            subscribeToChannel(brokerParameter, portParameter, channelParameter, imageDirectory, keyStoreParameter,
                    userNameParameter, userPasswordParameter, caPassword, serverCa, clientCrt, clientKey, keyPwd);

        } catch (IndexOutOfBoundsException e) {

            // e.printStackTrace();

            System.out.println(NOTIFICATION_ERROR_INPUT_BOUNDARIES);

            System.out.println(NOTIFICATION_ERROR_RETRY);

            return;

        } catch (IllegalArgumentException e) {

            // e.printStackTrace();

            System.out.println(NOTIFICATION_ERROR_INPUT_ILLEGAL);

            System.out.println(NOTIFICATION_ERROR_RETRY);

            return;

        }
    }

    private static void subscribeToChannel(String brokerParameter, String portParameter, String channelParameter,
            String imageDirectory, String keyStoreParameter, String userParameter, String passwordParameter,
            String caPassword, String serverCa, String clientCrt, String clientKey, String keyPwd) {

        try {

            System.out.println(NOTIFICATION_CONNECTION_PREPARE);

            String brokerDetails = MQTT_PROTOCOL_CONNECTION + brokerParameter + ":" + portParameter;

            SSLSocketFactory socketFactory = Factory.getSocketFactory(serverCa, clientCrt, clientKey, keyPwd);

            DirPicMqttClient client = new DirPicMqttClient(socketFactory, imageDirectory, brokerDetails, userParameter,
                    passwordParameter);

            System.out.println(NOTIFICATION_CONNECTION_ATTEMPT);

            client.connect();

            System.out.println(NOTIFICATION_CONNECTION_ESTABLISHED);

            System.out.println(NOTIFICATION_SUBSCRIBE_ATTEMPT);

            client.subscribe(channelParameter);

            System.out.println(NOTIFICATION_SUCCESS_SUBSCRIPTION);

        } catch (MqttException e) {

            // e.printStackTrace();

            System.out.println(NOTIFICATION_ERROR_SUBSCRIPTION + NOTIFICATION_ERROR_CONNECTION);

            System.out.println(NOTIFICATION_ERROR_RETRY);

            subscribeToChannel(brokerParameter, portParameter, channelParameter, imageDirectory, keyStoreParameter,
                    userParameter, passwordParameter, caPassword, serverCa, clientCrt, clientKey, keyPwd);

        } catch (Exception e) {

            // e.printStackTrace();

            System.out.println(NOTIFICATION_ERROR_GENERAL);

            System.out.println(NOTIFICATION_ERROR_RETRY);

            subscribeToChannel(brokerParameter, portParameter, channelParameter, imageDirectory, keyStoreParameter,
                    userParameter, passwordParameter, caPassword, serverCa, clientCrt, clientKey, keyPwd);
        }
    }
}
