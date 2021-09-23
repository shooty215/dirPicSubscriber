package com.mqtt.client.paho.subscriber.service;

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

import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

public class DirPicSslContext {

    private final String TYPE_REQUESTED_CERTIFICATE = "X.509";
    private final String VERSION_TLS = "TLSv1.2";

    private final String ALIAS_CA_CERTIFICATE = "ca-certificate";
    private final String ALIAS_CERTIFICATE = "certificate";
    private final String ALIAS_PRIVATE_KEY = "private-key";

    private final String CA_CERTIFICATE = "ca_crt.pem";
    private final String CLIENT_CERTIFICATE = "client_crt.pem";
    private final String CLIENT_PRIVATE_KEY = "client_key.pem";

    private final String NOTIFICATION_ENCRYPTED_PEM_FILE = "Encrypted key - we will use provided password";
    private final String NOTIFICATION_UN_ENCRYPTED_PEM_FILE = "Un-encrypted key - no password needed";

    private Properties props;

    private X509Certificate caCertificate, clientCertificate;

    private FileInputStream fiInSt;
    private BufferedInputStream buInSt;

    private CertificateFactory certFactory;

    private PEMParser pemParser;
    private Object obj;
    private PEMDecryptorProvider pemDecryptProvider;
    private JcaPEMKeyConverter pemKeyConverter;

    private KeyPair keyPair;

    private KeyStore caKeyStore, clientKeyStore;
    private TrustManagerFactory trustFactory;

    private KeyManagerFactory keyFactory;

    private SSLContext sslContext;

    public DirPicSslContext(Properties props) {

        this.props = props;

        this.prepareSslContextForFactoryCreation();

    }

    private void loadCAsCertificate() throws CertificateException, IOException {

        X509Certificate caCert = null;

        this.fiInSt = new FileInputStream(this.props.getKeyStorePath() + CA_CERTIFICATE);

        this.buInSt = new BufferedInputStream(this.fiInSt);

        this.certFactory = CertificateFactory.getInstance(TYPE_REQUESTED_CERTIFICATE);

        while (this.buInSt.available() > 0) {

            caCert = (X509Certificate) this.certFactory.generateCertificate(this.buInSt);

        }

        this.caCertificate = caCert;

        caCert = null;

    }

    private void loadClientsCertificate() throws CertificateException, IOException {

        X509Certificate cert = null;
        ;

        this.buInSt = new BufferedInputStream(new FileInputStream(this.props.getKeyStorePath() + CLIENT_CERTIFICATE));

        this.certFactory = CertificateFactory.getInstance(TYPE_REQUESTED_CERTIFICATE);

        while (this.buInSt.available() > 0) {

            cert = (X509Certificate) this.certFactory.generateCertificate(this.buInSt);

        }

        this.clientCertificate = cert;

        cert = null;
    }

    private void loadClientsPrivateKey() throws IOException {

        this.pemParser = new PEMParser(new FileReader(this.props.getKeyStorePath() + CLIENT_PRIVATE_KEY));

        this.obj = pemParser.readObject();

        this.pemDecryptProvider = new JcePEMDecryptorProviderBuilder()

                .build(props.getBrokerCertPassword().toCharArray());

        this.pemKeyConverter = new JcaPEMKeyConverter()
                .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        if (this.obj instanceof PEMEncryptedKeyPair) {

            System.out.println(NOTIFICATION_ENCRYPTED_PEM_FILE);

            this.keyPair = this.pemKeyConverter
                    .getKeyPair(((PEMEncryptedKeyPair) this.obj).decryptKeyPair(this.pemDecryptProvider));

        } else {

            System.out.println(NOTIFICATION_UN_ENCRYPTED_PEM_FILE);

            this.keyPair = this.pemKeyConverter.getKeyPair((PEMKeyPair) this.obj);

        }

        this.pemParser.close();

    }

    private void authenticateServerViaCaCertificate()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        this.caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        this.caKeyStore.load(null, null);

        this.caKeyStore.setCertificateEntry(ALIAS_CA_CERTIFICATE, this.caCertificate);

        this.trustFactory = TrustManagerFactory.getInstance(TYPE_REQUESTED_CERTIFICATE);

        this.trustFactory.init(this.caKeyStore);

    }

    private void sendClientsKeyAndCertificateToServer() throws KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException, CertificateException, IOException {

        this.clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        this.clientKeyStore.load(null, null);

        this.clientKeyStore.setCertificateEntry(ALIAS_CERTIFICATE, this.clientCertificate);

        this.clientKeyStore.setKeyEntry(ALIAS_PRIVATE_KEY, this.keyPair.getPrivate(),
                this.props.getBrokerCertPassword().toCharArray(),
                new java.security.cert.Certificate[] { this.clientCertificate });

        this.keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        keyFactory.init(this.clientKeyStore, this.props.getBrokerCertPassword().toCharArray());

    }

    private void createSslSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {

        this.sslContext = SSLContext.getInstance(VERSION_TLS);

        this.sslContext.init(this.keyFactory.getKeyManagers(), this.trustFactory.getTrustManagers(), null);

    }

    private void prepareSslContextForFactoryCreation() {

        Security.addProvider(new BouncyCastlePQCProvider());

        try {

            this.loadCAsCertificate();

            this.loadClientsCertificate();

            this.loadClientsPrivateKey();

            this.authenticateServerViaCaCertificate();

            this.sendClientsKeyAndCertificateToServer();

            this.createSslSocketFactory();

        } catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException
                | CertificateException | IOException e) {

            e.printStackTrace();

        }

    }

    public SSLSocketFactory getSslSocketFactory() {

        SSLSocketFactory result = this.sslContext.getSocketFactory();

        this.flush();

        return result;

    }

    private void flush() {

        this.props = null;
        this.fiInSt = null;
        this.buInSt = null;
        this.certFactory = null;
        this.clientCertificate = null;
        this.pemParser = null;
        this.obj = null;
        this.pemDecryptProvider = null;
        this.pemKeyConverter = null;
        this.keyPair = null;
        this.caKeyStore = null;
        this.trustFactory = null;
        this.clientKeyStore = null;
        this.keyFactory = null;
        this.sslContext = null;

    }
}
