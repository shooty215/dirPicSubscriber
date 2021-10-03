package com.mqtt.client.paho.subscriber.cryptography.als;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

class Asymmetric {

    private final String RSA = "RSA";

    private byte[] text, result;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private boolean flag;
    private int mode;

    Asymmetric(int mode, byte[] text, String key) throws Exception {

        this.mode = mode;

        this.text = text;

        if (this.mode == Cipher.ENCRYPT_MODE) {

            this.publicKey = this.turnStringToPublicKey(key);

            this.encrypt();

        } else if (this.mode == Cipher.DECRYPT_MODE) {

            this.privateKey = this.turnStringToPrivateKey(key);

            this.decrypt();

        } else {

            this.flag = false;

        }

    }

    byte[] getResult() {
        return result;
    }

    boolean is_success() {
        return flag;
    }

    void encrypt() throws Exception {

        Cipher cipher = Cipher.getInstance(RSA);

        cipher.init(Cipher.ENCRYPT_MODE, this.publicKey);

        byte[] result = cipher.doFinal(this.text);

        if (result.length > 0) {

            this.flag = true;

            this.result = result;

        } else {

            this.flag = false;
        }

    }

    void decrypt() throws Exception {
        Security.addProvider(new BouncyCastlePQCProvider());

        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");

        cipher.init(Cipher.DECRYPT_MODE, this.privateKey);

        byte[] result = cipher.doFinal(this.text);

        if (result.length > 0) {

            this.flag = true;

            this.result = result;

        } else {

            this.flag = false;

        }

    }

    private PublicKey turnStringToPublicKey(String key) {

        byte[] publicBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(keySpec);
            return pubKey;
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }

    private PrivateKey turnStringToPrivateKey(String key) {
        try {
            byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(key);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);

            KeyFactory keyFactory;

            keyFactory = KeyFactory.getInstance(RSA);

            PrivateKey privKey = keyFactory.generatePrivate(keySpec);

            return privKey;

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }

    }

}
