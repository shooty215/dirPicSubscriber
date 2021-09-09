package com.mqtt.client.paho.subscriber.service;

import java.time.format.DateTimeFormatter;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;

public class DirPicMqttClient extends MqttClient {

    private final String NOTIFICATION_FILE_WRITE_ATTEMPT = "Writing image...";

    private final String NOTIFICATION_SUCCESS_SUBSCRIBED_IMAGE = "Message received! Assessing bytes..";
    private final String NOTIFICATION_SUCCESS_WRITING = "Image written!";
    private final String NOTIFICATION_SUCCESS_DELIVERY = "Delivered! Should Not Accrue.";
    private final String TIME_DATE_PATTERN = "yyyy.MM.dd_HH:mm:ss";

    private final String FILE_FORMAT_JPEG = ".jpeg";

    final static String NOTIFICATION_ERROR_CONNECTION = "\nConnection Issues!\n";
    final static String NOTIFICATION_ERROR_RETRY = "\nRetrying...\n";
    final static String NOTIFICATION_ERROR_PARAMETER = "\nParameter Issues!\n";
    final static String NOTIFICATION_ERROR_SUBSCRIPTION = "\nSubscription Issues!\n";
    final static String NOTIFICATION_ERROR_IO = "\nI/O problem!\n";
    final static String NOTIFICATION_ERROR_GENERAL = "\nGeneral Exception!\n";

    private final int MQTT_QOS = 2;
    private final boolean MQTT_RETAINED = false;
    // private final String MQTT_PUBLISHER_ID_PREFIX = "idPublisher";

    private final int INTERVAL_TIMEOUT_CONNECTION = 10;
    private final int INTERVAL_KEEP_ALIVE_CONNECTION = 10;

    private final boolean FLAG_PERSISTENCE_LAYER = false;

    private SSLSocketFactory socketFactory;
    private MqttConnectOptions options;
    private String userParameter;
    private String passwordParameter;
    private String imageDirectory;

    public DirPicMqttClient(SSLSocketFactory socketFactory, String imageDirectory, String brokerDetails,
            String userParameter, String passwordParameter) throws MqttException {
        super(brokerDetails, "idSubscriber" + MqttClient.generateClientId(), new MemoryPersistence());
        this.imageDirectory = imageDirectory;
        this.socketFactory = socketFactory;
        this.userParameter = userParameter;
        this.passwordParameter = passwordParameter;
        this.setConnectionCredentials(this.imageDirectory);
    }

    private boolean setConnectionCredentials(String imageDirectory) {

        this.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable throwable) {

                System.out.println(NOTIFICATION_ERROR_CONNECTION + throwable.getMessage());

            };

            @Override
            public void messageArrived(String t, MqttMessage m) throws Exception {

                System.out.println(NOTIFICATION_SUCCESS_SUBSCRIBED_IMAGE);

                // tries to save image from received bytes
                try {

                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(TIME_DATE_PATTERN);

                    LocalDateTime currentTime = LocalDateTime.now();

                    String fileName = dateTimeFormatter.format(currentTime) + FILE_FORMAT_JPEG;

                    String filePath = imageDirectory + fileName;

                    byte[] data = m.getPayload();

                    ByteArrayInputStream bis = new ByteArrayInputStream(data);

                    BufferedImage bImage2 = ImageIO.read(bis);

                    bImage2.flush();

                    File file = new File(filePath);

                    file.getParentFile().mkdirs();

                    file.createNewFile();

                    System.out.println(NOTIFICATION_FILE_WRITE_ATTEMPT);

                    ImageIO.write(bImage2, FILE_FORMAT_JPEG, file);

                    if (file.isFile() && file.exists()) {

                        System.out.println(NOTIFICATION_SUCCESS_WRITING);

                        System.out.println(NOTIFICATION_ERROR_RETRY);

                    } else {

                        System.out.println(NOTIFICATION_ERROR_IO);

                        System.out.println(NOTIFICATION_ERROR_RETRY);

                    }

                } catch (Exception e) {

                    System.out.println(NOTIFICATION_ERROR_GENERAL);

                    System.out.println(NOTIFICATION_ERROR_RETRY);

                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

                System.out.println(NOTIFICATION_SUCCESS_DELIVERY);

            }

        });

        this.options = new MqttConnectOptions();

        this.options.setSocketFactory(this.socketFactory);

        this.options.setConnectionTimeout(INTERVAL_TIMEOUT_CONNECTION);

        this.options.setKeepAliveInterval(INTERVAL_KEEP_ALIVE_CONNECTION);

        this.options.setCleanSession(FLAG_PERSISTENCE_LAYER); // Persistent Session

        this.options.setUserName(this.userParameter);

        this.options.setPassword(this.passwordParameter.toCharArray());

        return this.validateOptions();
    }

    private boolean validateOptions() {

        return this.options.getSocketFactory() == this.socketFactory
                && this.options.getSocketFactory() == this.socketFactory
                && this.options.getConnectionTimeout() == INTERVAL_TIMEOUT_CONNECTION
                && this.options.getKeepAliveInterval() == INTERVAL_KEEP_ALIVE_CONNECTION
                && this.options.isCleanSession() == FLAG_PERSISTENCE_LAYER
                && this.options.getUserName() == this.userParameter
                && this.options.getPassword() == this.passwordParameter.toCharArray();

    }

    @Override
    public void connect() throws MqttSecurityException, MqttException {

        super.connect(this.options);

    }

    public void publish(String topic, byte[] payload) throws MqttException, MqttPersistenceException {

        super.publish(topic, payload, MQTT_QOS, MQTT_RETAINED);

    }
}
