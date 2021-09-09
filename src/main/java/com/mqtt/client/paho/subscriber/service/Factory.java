package com.mqtt.client.paho.subscriber.service;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

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

public class Factory {

    final static String TYPE_REQUESTED_CERTIFICATE = "X.509";
    final static String VERSION_TLS = "TLSv1.2";

    final static String NOTIFICATION_ENCRYPTED_PEM_FILE = "Encrypted key - we will use provided password";
    final static String NOTIFICATION_UN_ENCRYPTED_PEM_FILE = "Un-encrypted key - no password needed";

    final static String ALIAS_CA_CERTIFICATE = "ca-certificate";
    final static String ALIAS_CERTIFICATE = "certificate";
    final static String ALIAS_PRIVATE_KEY = "private-key";

    public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile,
            final String password) throws Exception {
        Security.addProvider(new BouncyCastlePQCProvider());

        // load CA certificate
        X509Certificate caCert = null;

        FileInputStream fis = new FileInputStream(caCrtFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        CertificateFactory cf = CertificateFactory.getInstance(TYPE_REQUESTED_CERTIFICATE);

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
            System.out.println(NOTIFICATION_ENCRYPTED_PEM_FILE);
            key = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
        } else {
            System.out.println(NOTIFICATION_UN_ENCRYPTED_PEM_FILE);
            key = converter.getKeyPair((PEMKeyPair) object);
        }
        pemParser.close();

        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry(ALIAS_CA_CERTIFICATE, caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TYPE_REQUESTED_CERTIFICATE);
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate
        // us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry(ALIAS_CERTIFICATE, cert);
        ks.setKeyEntry(ALIAS_PRIVATE_KEY, key.getPrivate(), password.toCharArray(),
                new java.security.cert.Certificate[] { cert });
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance(VERSION_TLS);
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }
}
