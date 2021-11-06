package com.mqtt.client.paho.subscriber.cryptography.tls;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.mqtt.client.paho.subscriber.service.Properties;

import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

public class TlsContext {

    private final String TYPE_REQUESTED_CERTIFICATE = "X.509";
    private final String VERSION_TLS = "TLSv1.3";

    private final String TLS_SERVER_CRT_PEM = "tls_server_crt.pem";
    private final String TLS_CLIENT_CRT_PEM = "tls_client_crt.pem";
    private final String TLS_CLIENT_PRIVATE_KEY_PEM = "tls_client_private_key.pem";

    private final String NOTIFICATION_ENCRYPTED_PEM_FILE = "Encrypted key - we will use provided password";
    private final String NOTIFICATION_UN_ENCRYPTED_PEM_FILE = "Un-encrypted key - no password needed";

    private final String ALIAS_CA_CERTIFICATE = "ca-certificate";
    private final String ALIAS_CERTIFICATE = "certificate";
    private final String ALIAS_PRIVATE_KEY = "private-key";

    private SSLSocketFactory factory;
    private X509Certificate caCert;
    private X509Certificate cert;
    private KeyPair key;
    private TrustManagerFactory tmf;
    private String password;

    public TlsContext(Properties props) {

        this.password = "";

        Security.addProvider(new BouncyCastlePQCProvider());

        try {

            this.loadServerCert(props.getKeyStorePath() + TLS_SERVER_CRT_PEM);

            this.loadClientCert(props.getKeyStorePath() + TLS_CLIENT_CRT_PEM);

            this.loadClientPrivateKey(props.getKeyStorePath() + TLS_CLIENT_PRIVATE_KEY_PEM);

            this.authenticateServersCACertificate();

            this.authenticateClientCACertificateWithServer();

        } catch (CertificateException | IOException | UnrecoverableKeyException | KeyManagementException
                | KeyStoreException | NoSuchAlgorithmException e) {

            e.printStackTrace();

        }
    }

    public SSLSocketFactory getFactory() {

        return factory;

    }

    private void loadServerCert(String caCrtFile) throws CertificateException, IOException {

        X509Certificate caCert = null;

        FileInputStream fis = new FileInputStream(caCrtFile);

        BufferedInputStream bis = new BufferedInputStream(fis);

        CertificateFactory cf = CertificateFactory.getInstance(TYPE_REQUESTED_CERTIFICATE);

        while (bis.available() > 0) {

            caCert = (X509Certificate) cf.generateCertificate(bis);

        }

        this.caCert = caCert;
    }

    private void loadClientCert(String crtFile) throws CertificateException, IOException {

        X509Certificate cert = null;

        CertificateFactory cf = CertificateFactory.getInstance(TYPE_REQUESTED_CERTIFICATE);

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(crtFile));

        while (bis.available() > 0) {

            cert = (X509Certificate) cf.generateCertificate(bis);

            // System.out.println(caCert.toString());

        }

        this.cert = cert;
    }

    private void loadClientPrivateKey(String keyFile) throws IOException {

        // load client private key
        PEMParser pemParser = new PEMParser(new FileReader(keyFile));

        Object object = pemParser.readObject();

        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(this.password.toCharArray());

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

        this.key = key;
    }

    private void authenticateServersCACertificate()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());

        caKs.load(null, null);

        caKs.setCertificateEntry(ALIAS_CA_CERTIFICATE, this.caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TYPE_REQUESTED_CERTIFICATE);

        this.tmf = tmf;

        this.tmf.init(caKs);

    }

    private void authenticateClientCACertificateWithServer() throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        ks.load(null, null);

        ks.setCertificateEntry(ALIAS_CERTIFICATE, this.cert);

        ks.setKeyEntry(ALIAS_PRIVATE_KEY, this.key.getPrivate(), this.password.toCharArray(),
                new java.security.cert.Certificate[] { this.cert });

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        kmf.init(ks, this.password.toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance(VERSION_TLS);

        context.init(kmf.getKeyManagers(), this.tmf.getTrustManagers(), null);

        this.factory = context.getSocketFactory();
    }

}
