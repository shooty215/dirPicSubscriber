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

        if (this.mode == Cipher.ENCRYPT_MODE) {

            Asymmetric asym = new Asymmetric(this.mode, this.text, this.publicKey);

            if (asym.is_success()) {

                this.flag = true;

                Symmetric sym = new Symmetric(this.mode, asym.getResult(), bs);

                if (sym.is_success()) {

                    this.flag = true;

                    this.cipherToken = sym.getCipherToken();

                } else {

                    this.flag = false;

                    this.cipherToken = new byte[0];

                }

            } else {

                this.flag = false;

                this.cipherToken = new byte[0];

            }

        } else if (this.mode == Cipher.DECRYPT_MODE) {

            Symmetric sym = new Symmetric(this.mode, this.text, bs);

            if (sym.is_success()) {

                this.flag = true;

                Asymmetric asym = new Asymmetric(this.mode, sym.getUnsecretive(), this.privateKey);

                if (asym.is_success()) {

                    this.flag = true;

                    this.unsecretive = asym.getResult();

                } else {

                    this.flag = false;

                    this.unsecretive = new byte[0];
                }

            } else {

                this.flag = false;

                this.unsecretive = new byte[0];

            }

        } else {

            this.flag = false;

            this.unsecretive = new byte[0];

        }

    }
}
