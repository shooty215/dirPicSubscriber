package com.mqtt.client.paho.subscriber;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        /*
         * jar dirPicSubscriber.jar <broker_ip> <broker_port> <broker_topic>
         * <image_save_directory> <key_store_directory> <user_name> <user_password>
         * <ca_password>
         */

        // Inputs for meant use.
        String brokerParameter = args[0];
        String portParameter = args[1];
        String channelParameter = args[2];
        String imageDirectory = args[3];
        String keyStoreParameter = args[4];
        String userNameParameter = args[5];
        String userPasswordParameter = args[6];
        String caPassword = args[7];

        final String serverCa = keyStoreParameter + "ca_crt.pem";
        final String clientCrt = keyStoreParameter + "client_crt.pem";
        final String clientKey = keyStoreParameter + "client_key.pem";
        final String keyPwd = caPassword;

        try {

            String brokerDetails = "ssl://" + brokerParameter + ":" + portParameter;

            MqttClient client = new MqttClient(brokerDetails, "idSubscriber" + MqttClient.generateClientId());

            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable throwable) {

                    System.out.println("Connection lost!");

                };

                @Override
                public void messageArrived(String t, MqttMessage m) throws Exception {

                    System.out.println("Message received! Assessing bytes..");

                    // tries to save image from received bytes
                    try {

                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH:mm:ss");

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

            SSLSocketFactory socketFactory = getSocketFactory(serverCa, clientCrt, clientKey, keyPwd);

            options.setSocketFactory(socketFactory);

            options.setUserName(userNameParameter);

            options.setPassword(userPasswordParameter.toCharArray());

            options.setCleanSession(false);

            System.out.println("Connecting...");

            client.connect(options);

            System.out.println("Connected!");

            System.out.println("Subscribing...");

            client.subscribe(channelParameter);

            System.out.println("Subscribed!");

            // client.disconnect();

        } catch (MqttException e) {

            System.out.println("Not Subscribed! - Server Error.");

            e.printStackTrace();

        } catch (Exception e) {

            System.out.println("Not Subscribed! - Something Went Wrong.");

            e.printStackTrace();
        }
    }

    private static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile,
            final String password) throws Exception {
        Security.addProvider(new BouncyCastlePQCProvider());

        // load CA certificate
        X509Certificate caCert = null;

        FileInputStream fis = new FileInputStream(caCrtFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        while (bis.available() > 0) {
            caCert = (X509Certificate) cf.generateCertificate(bis);
            // System.out.println(caCert.toString());
        }

        // load client certificate
        bis = new BufferedInputStream(new FileInputStream(crtFile));
        X509Certificate cert = null;
        while (bis.available() > 0) {
            cert = (X509Certificate) cf.generateCertificate(bis);
            // System.out.println(caCert.toString());
        }

        // load client private key
        PEMParser pemParser = new PEMParser(new FileReader(keyFile));
        Object object = pemParser.readObject();
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        KeyPair key;
        if (object instanceof PEMEncryptedKeyPair) {
            System.out.println("Encrypted key - we will use provided password");
            key = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
        } else {
            System.out.println("Unencrypted key - no password needed");
            key = converter.getKeyPair((PEMKeyPair) object);
        }
        pemParser.close();

        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate
        // us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", cert);
        ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(),
                new java.security.cert.Certificate[] { cert });
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }

}
