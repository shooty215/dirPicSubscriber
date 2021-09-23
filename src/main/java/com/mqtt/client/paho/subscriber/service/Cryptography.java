package com.mqtt.client.paho.subscriber.service;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Cryptography {

    private final String SEC_KEY_SPEC_ALGORITHM = "AES";

    private final String GCM_CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    private final String NOTIFICATION_ERROR_FAILURE = "Cryptography module failed: ";

    private final int GCM_IV_LENGTH = 12;
    private static final int GCM_BLOCK_SIZE = 16;
    private static final int GCM_TAG_LENGTH_BYTES = 16;
    private final int GCM_TAG_LENGTH_BITS = 16 * 8;

    private SecretKeySpec keySpec;
    private GCMParameterSpec gcmParameterSpec;
    private Cipher cipher;
    private String key;
    private byte[] initializationVector, unsecretive, cipherToken;
    private boolean flag = false;
    private int mode;

    public Cryptography(int mode, byte[] unsecretive, byte[] cipherToken, String key) {

        this.mode = mode;

        this.key = key;

        if (this.mode == Cipher.ENCRYPT_MODE) {

            this.unsecretive = unsecretive;

            this.flag = this.encryptData();

        } else if (this.mode == Cipher.DECRYPT_MODE) {

            this.cipherToken = cipherToken;

            this.flag = this.decryptData();

        } else {

            this.flag = false;

        }

    }

    public byte[] getUnsecretive() {

        return this.unsecretive;

    }

    public byte[] getCipherToken() {

        return this.cipherToken;

    }

    public boolean is_success() {

        return this.flag;

    }

    private boolean encryptData() {

        try {

            this.initializationVector = this.createIV();

            this.keySpec = this.createKeySpec();

            this.gcmParameterSpec = this.createGcmSpec();

            this.cipher = this.initiateCipher();

            System.out.println(this.cipher.getParameters());

            byte[] cipherToken = this.cipher.doFinal(this.unsecretive);

            cipherToken = this.addIvToCipherToken(cipherToken, this.cipher.getIV());

            if (cipherToken.length >= GCM_TAG_LENGTH_BYTES + GCM_TAG_LENGTH_BYTES) {

                this.cipherToken = cipherToken;

                return true;

            } else {

                return false;

            }

        } catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException | NoSuchPaddingException | InvalidKeyException e) {

            System.out.println(NOTIFICATION_ERROR_FAILURE + e.toString());

            this.cipher = null;

            return false;
        }
    }

    private boolean decryptData() {

        try {

            this.initializationVector = this.getBytesFromCipherToken(this.cipherToken,
                    this.cipherToken.length - GCM_IV_LENGTH, this.cipherToken.length);

            this.cipherToken = this.getBytesFromCipherToken(this.cipherToken,
                    this.cipherToken.length - this.cipherToken.length,
                    this.cipherToken.length - this.initializationVector.length);

            this.keySpec = this.createKeySpec();

            this.gcmParameterSpec = this.createGcmSpec();

            this.cipher = this.initiateCipher();

            byte[] unsecretive = this.cipher.doFinal(this.cipherToken);

            if (unsecretive.length >= 0) {

                this.unsecretive = unsecretive;

                return true;

            } else {

                return false;

            }

        } catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException | NoSuchPaddingException | InvalidKeyException e) {

            System.out.println(NOTIFICATION_ERROR_FAILURE + e.toString());

            this.cipher = null;

            return false;
        }
    }

    private byte[] createIV() {

        int[] intVector = this.createVector(GCM_IV_LENGTH);

        byte[] cipherVector = this.castIntArrayToByteArray(intVector);

        if (cipherVector.length > 0) {

            return cipherVector;

        } else {

            System.out.println(NOTIFICATION_ERROR_FAILURE + "Vector Creation");

            return null;

        }

    }

    private SecretKeySpec createKeySpec() {

        SecretKeySpec keySpec = new SecretKeySpec(this.castCipherVectorToBytes(this.key), SEC_KEY_SPEC_ALGORITHM);

        if (!keySpec.isDestroyed()) {

            return keySpec;

        } else {

            System.out.println(NOTIFICATION_ERROR_FAILURE + "key spec creation");

            return null;

        }
    }

    private GCMParameterSpec createGcmSpec() {

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, this.initializationVector);

        if (gcmParameterSpec.getIV().length == GCM_IV_LENGTH && gcmParameterSpec.getTLen() == GCM_TAG_LENGTH_BITS) {

            return gcmParameterSpec;

        } else {

            System.out.println(NOTIFICATION_ERROR_FAILURE + "gcm spec creation");

            return null;

        }
    }

    private Cipher initiateCipher() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, InvalidKeyException {

        Cipher cipher = Cipher.getInstance(GCM_CIPHER_TRANSFORMATION);

        cipher.init(this.mode, keySpec, gcmParameterSpec);

        if (cipher.getBlockSize() == GCM_BLOCK_SIZE && cipher.getAlgorithm() == GCM_CIPHER_TRANSFORMATION) {

            return cipher;

        }

        System.out.println(NOTIFICATION_ERROR_FAILURE + "Cipher Initiation " + cipher.getAlgorithm());

        return null;
    }

    private byte[] addIvToCipherToken(byte[] token, byte[] iv) {

        byte[] cipherToken = new byte[token.length + iv.length];

        for (int i = 0; i < cipherToken.length; i++) {

            if (i < token.length) {

                cipherToken[i] = token[i];

            } else if (i >= token.length) {

                cipherToken[i] = iv[i - token.length];
            }
        }

        return cipherToken;

    }

    private byte[] getBytesFromCipherToken(byte[] token, int start, int stop) {

        if (stop - start > 0) {

            byte[] iv = new byte[stop - start];

            for (int i = 0; i < iv.length; i++) {

                iv[i] = token[start + i];

            }

            return iv;

        } else {

            System.out.println(NOTIFICATION_ERROR_FAILURE + "byte copy");

            return null;

        }

    }

    private int[] createVector(int length) {

        int[] intVector = new int[length];

        for (int i = 0; i < intVector.length; i++) {

            intVector[i] = (int) (Math.random() * 128);

        }

        return intVector;

    }

    private byte[] castCipherVectorToBytes(String vector) {

        byte[] byteVector = new byte[vector.length() / 2];

        for (int i = 0; i < byteVector.length; i++) {

            int index = i * 2;

            String bits = "";

            if (vector.charAt(index) != '0') {

                bits = bits + vector.charAt(index) + vector.charAt(index + 1);

            } else {

                bits = bits + vector.charAt(index + 1);

            }

            byteVector[i] = (byte) Integer.parseInt(bits, 16);

        }

        return byteVector;

    }

    private byte[] castIntArrayToByteArray(int[] in) {

        byte[] result = new byte[in.length];

        for (int i = 0; i < in.length; i++) {

            result[i] = (byte) in[i];

        }

        return result;

    }

}
