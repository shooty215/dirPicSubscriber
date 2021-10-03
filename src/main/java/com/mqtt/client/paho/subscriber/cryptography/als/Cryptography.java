package com.mqtt.client.paho.subscriber.cryptography.als;

import javax.crypto.Cipher;

import com.mqtt.client.paho.subscriber.service.Properties;

public class Cryptography {

    private int mode;
    private byte[] unsecretive, cipherToken;
    private String privateKey;
    private String publicKey;
    private boolean flag;
    private byte[] text;

    public Cryptography(Properties props, int mode, byte[] text) throws Exception {

        this.mode = mode;

        this.text = text;

        if (this.mode == Cipher.ENCRYPT_MODE) {

            this.publicKey = props.getRsaPublicKey();

            this.crypt(props, props.getAesKey());

        } else if (this.mode == Cipher.DECRYPT_MODE) {

            this.privateKey = props.getRsaPrivateKey();

            this.crypt(props, props.getAesKey());

        } else {

            this.flag = false;

        }
    }

    public byte[] getUnsecretive() {
        return unsecretive;
    }

    public byte[] getCipherToken() {
        return cipherToken;
    }

    public boolean is_success() {
        return flag;
    }

    private void crypt(Properties props, String bs) throws Exception {

        byte[] cipherToken;

        Symmetric sym = new Symmetric(this.mode, this.text, bs);

        if (sym.is_success()) {

            cipherToken = sym.getCipherToken();

            System.out.println(new String(cipherToken));

            Asymmetric asym = null;

            if (this.mode == Cipher.ENCRYPT_MODE) {

                asym = new Asymmetric(this.mode, cipherToken, this.publicKey);

            } else if (this.mode == Cipher.DECRYPT_MODE) {

                asym = new Asymmetric(this.mode, cipherToken, this.privateKey);

            }

            if (asym.is_success() && this.mode == Cipher.ENCRYPT_MODE) {

                this.flag = true;

                this.cipherToken = sym.getCipherToken();

            } else if (asym.is_success() && this.mode == Cipher.DECRYPT_MODE) {

                this.flag = true;

                this.unsecretive = sym.getUnsecretive();

            } else {

                this.flag = false;
            }

        } else {

            this.flag = false;
        }
    }
}
