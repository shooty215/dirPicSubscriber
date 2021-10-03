package com.mqtt.client.paho.subscriber.mqtt;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.crypto.Cipher;
import javax.imageio.ImageIO;
import javax.net.ssl.SSLSocketFactory;

import com.mqtt.client.paho.subscriber.cryptography.als.Cryptography;
import com.mqtt.client.paho.subscriber.service.Properties;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttClient extends MqttAsyncClient {

    private final String NOTIFICATION_FILE_WRITE_ATTEMPT = "Writing image...";

    private final String NOTIFICATION_SUCCESS_SUBSCRIBED_IMAGE = "Message received! Assessing bytes..";
    private final String NOTIFICATION_SUCCESS_WRITING = "Image written!";
    private final String NOTIFICATION_SUCCESS_DELIVERY = "Delivered! Should Not Accrue.";
    private final String NOTIFICATION_SUCCESS_ENCRYPTION = "\nEncrypted!\n";
    private final String TIME_DATE_PATTERN = "yyyy.MM.dd_HH:mm:ss";

    private final String FILE_FORMAT_JPEG = ".jpeg";
    private final String NOTIFICATION_ERROR_ENCRYPTION = "\nEncryption Exception!\n";
    private final String NOTIFICATION_ERROR_CONNECTION = "\nConnection Issues!\n";
    private final String NOTIFICATION_ERROR_RETRY = "\nRetrying...\n";

    private final String NOTIFICATION_ERROR_IO = "\nI/O problem!\n";
    private final String NOTIFICATION_ERROR_GENERAL = "\nGeneral Exception!\n";

    private final int MQTT_QOS = 2;

    private final int INTERVAL_TIMEOUT_CONNECTION = 60;
    private final int INTERVAL_KEEP_ALIVE_CONNECTION = 60;

    private final boolean FLAG_PERSISTENCE_LAYER = false;

    private SSLSocketFactory socketFactory;
    private MqttConnectOptions options;
    private Properties props;

    public MqttClient(SSLSocketFactory socketFactory, String brokerDetails, Properties props) throws MqttException {
        super(brokerDetails, "idSubscriber" + MqttClient.generateClientId(), new MemoryPersistence());
        this.socketFactory = socketFactory;
        this.props = props;
        this.setConnectionCredentials();
    }

    private boolean setConnectionCredentials() {

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

                    String filePath = props.getStoragePath() + fileName;

                    byte[] data = m.getPayload();

                    Cryptography decryption = new Cryptography(props, Cipher.DECRYPT_MODE, data);

                    if (decryption.is_success()) {

                        System.out.println(NOTIFICATION_SUCCESS_ENCRYPTION);

                        data = decryption.getUnsecretive();

                    } else {

                        System.out.println(NOTIFICATION_ERROR_ENCRYPTION);

                        return;
                    }

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

        this.options.setUserName(props.getBrokerAuthUser());

        this.options.setPassword(props.getBrokerAuthPassword().toCharArray());

        return this.validateOptions();
    }

    private boolean validateOptions() {

        return this.options.getSocketFactory() == this.socketFactory
                && this.options.getSocketFactory() == this.socketFactory
                && this.options.getConnectionTimeout() == INTERVAL_TIMEOUT_CONNECTION
                && this.options.getKeepAliveInterval() == INTERVAL_KEEP_ALIVE_CONNECTION
                && this.options.isCleanSession() == FLAG_PERSISTENCE_LAYER
                && this.options.getUserName() == this.props.getBrokerAuthUser()
                && this.options.getPassword() == props.getBrokerAuthPassword().toCharArray();
    }

    @Override
    public IMqttToken connect() throws MqttSecurityException, MqttException {

        IMqttToken tok = super.connect(options, null, null);

        tok.waitForCompletion();

        return tok;
    }

    public void subscribe(String topic) throws MqttException, MqttPersistenceException {

        super.subscribe(topic, MQTT_QOS);

    }
}
