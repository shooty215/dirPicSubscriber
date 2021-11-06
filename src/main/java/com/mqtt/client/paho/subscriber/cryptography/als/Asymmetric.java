package com.mqtt.client.paho.subscriber.cryptography.als;

import java.nio.charset.StandardCharsets;
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

public class Asymmetric {

    private final String RSA = "RSA";
    private final int keySize = 8194;
    private final int dataSetSize = 10;
    private final int workableResultSize = (keySize / 8);
    private final int workableSize = workableResultSize - 11;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private Cipher cipher;
    private byte[] text, result;
    private boolean flag;
    private int mode;

    Asymmetric(int mode, byte[] text, String key) throws Exception {

        this.mode = mode;

        this.text = text;

        if (this.mode == Cipher.ENCRYPT_MODE) {

            this.publicKey = this.turnStringToPublicKey(key);

            this.result = this.sizeUpBytesAndEncrypt();

        } else if (this.mode == Cipher.DECRYPT_MODE) {

            this.privateKey = this.turnStringToPrivateKey(key);

            this.result = this.sizeUpBytesAndDecrypt();

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

    private byte[] encrypt(byte[] bytes) throws Exception {

        this.cipher = Cipher.getInstance(RSA);

        this.cipher.init(Cipher.ENCRYPT_MODE, this.publicKey);

        byte[] result = this.cipher.doFinal(bytes);

        if (result.length > 0) {

            this.flag = true;

            return result;

        } else {

            this.flag = false;

            return new byte[0];
        }

    }

    private byte[] decrypt(byte[] bytes) throws Exception {

        Security.addProvider(new BouncyCastlePQCProvider());

        this.cipher = Cipher.getInstance("RSA/ECB/NoPadding");

        this.cipher.init(Cipher.DECRYPT_MODE, this.privateKey);

        byte[] result = this.cipher.doFinal(bytes);

        if (result.length > 0) {

            this.flag = true;

            return result;

        } else {

            this.flag = false;

            return new byte[0];
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

    byte[] getBytesFromArray(int start, int stop, byte[] token) {

        if (stop - start > 0) {

            byte[] iv = new byte[stop - start + 1];

            for (int i = 0; i < iv.length; i++) {

                iv[i] = token[start + i];

            }

            return iv;

        } else {

            System.out.println("error byte copy");

            return new byte[0];

        }

    }

    private byte[] determineOverlapsBytes(int overlaps) {

        String overlapsString = Integer.toBinaryString(overlaps);

        int offset = this.dataSetSize - overlapsString.length();

        String fillString = "";

        for (int i = 0; i < offset; i++) {

            fillString = fillString + "0";

        }

        overlapsString = fillString + overlapsString;

        byte[] overlapsBytes = overlapsString.getBytes(StandardCharsets.UTF_8);

        return overlapsBytes;

    }

    private byte[] sizeUpBytesAndEncrypt() {

        int numberOfContainers = (int) Math.floor((this.text.length / (float) this.workableResultSize));

        int overlaps = this.text.length % this.workableSize;

        byte[] overlapsByte = this.determineOverlapsBytes(overlaps);

        byte[] encrypteds = new byte[(this.workableResultSize * (numberOfContainers + 1)) + this.dataSetSize];

        for (int i = 0; i < numberOfContainers; i++) {

            byte[] tempArray = getBytesFromArray(i * this.workableSize, (i * this.workableSize) + this.workableSize - 1,
                    this.text);

            byte[] tempArrayEncryptionResult = new byte[this.workableResultSize];

            try {

                tempArrayEncryptionResult = this.encrypt(tempArray);

            } catch (Exception e) {

                e.printStackTrace();

            }

            for (int j = i * this.workableResultSize; j < (i * this.workableResultSize)
                    + this.workableResultSize; j++) {

                encrypteds[j] = tempArrayEncryptionResult[j - (i * this.workableResultSize)];

            }
        }

        byte[] tempArray = this.getBytesFromArray(numberOfContainers * this.workableSize,
                (numberOfContainers * this.workableSize) + overlaps - 1, this.text);

        byte[] tempArrayEncryptionResult = new byte[this.workableResultSize];

        try {

            tempArrayEncryptionResult = encrypt(tempArray);

            for (int j = 0; j < tempArrayEncryptionResult.length; j++) {

                encrypteds[(this.workableResultSize * numberOfContainers) + j] = tempArrayEncryptionResult[j];

            }

            for (int i = 0; i < this.dataSetSize; i++) {

                encrypteds[encrypteds.length - this.dataSetSize + i] = overlapsByte[i];

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

        return encrypteds;

    }

    private byte[] sizeUpBytesAndDecrypt() {

        int numberOfContainers = ((int) Math
                .floor(((this.text.length - this.dataSetSize) / (float) this.workableResultSize))) - 1;

        if (numberOfContainers < 0) {

            numberOfContainers = 0;

        }

        byte[] overlapsBytes = new byte[this.dataSetSize];

        for (int i = 0; i < this.dataSetSize; i++) {

            overlapsBytes[i] = this.text[this.text.length - this.dataSetSize + i];

        }

        byte[] bytesArray = new byte[this.text.length - this.dataSetSize];

        for (int i = 0; i < bytesArray.length; i++) {

            bytesArray[i] = this.text[i];

        }

        String overlapsString = new String(overlapsBytes);

        int overlaps = 0;

        for (int i = 0; i < this.dataSetSize; i++) {

            int charInt = overlapsString.charAt(this.dataSetSize - i - 1) - '0';

            if (charInt > 0) {

                int sum = ((int) Math.pow(2 * charInt, i));

                overlaps += sum;

            } else {

                int sum = 0;

                overlaps += sum;

            }

        }

        byte[] decrypteds = new byte[(numberOfContainers * this.workableResultSize) + this.workableResultSize];

        for (int i = 0; i < numberOfContainers; i++) {

            byte[] tempArray0 = new byte[this.workableResultSize];

            tempArray0 = this.getBytesFromArray(i * this.workableResultSize,
                    (i * this.workableResultSize) + this.workableResultSize - 1, bytesArray);

            byte[] tempArrayDecryptionResult0 = new byte[this.workableResultSize];

            try {

                tempArrayDecryptionResult0 = this.decrypt(tempArray0);

                for (int j = 0; j < tempArrayDecryptionResult0.length; j++) {

                    decrypteds[j + (this.workableResultSize * i)] = tempArrayDecryptionResult0[j];

                }

            } catch (Exception e) {

                e.printStackTrace();

            }

        }

        byte[] tempArray1 = new byte[this.workableResultSize];

        tempArray1 = getBytesFromArray(numberOfContainers * this.workableResultSize,
                (numberOfContainers * this.workableResultSize) + this.workableResultSize - 1, bytesArray);

        byte[] tempArrayDecryptionResult = new byte[this.workableResultSize];

        try {

            tempArrayDecryptionResult = this.decrypt(tempArray1);

            for (int j = 0; j < tempArrayDecryptionResult.length; j++) {

                decrypteds[j + (this.workableResultSize * numberOfContainers)] = tempArrayDecryptionResult[j];

            }

        } catch (Exception e) {

            return new byte[0];

        }

        return this.padding(decrypteds, overlaps);
    }

    private byte[] padding(byte[] text, int overlaps) {

        int IIth = 11;

        int blockSize = 1024;

        int filler = blockSize - overlaps;

        int newLength = text.length - filler;

        byte[] resultArray = new byte[newLength];

        for (int n = 0; n < text.length; n = n + blockSize) {

            if (n < (text.length - blockSize)) {

                for (int i = 0; i < blockSize - IIth; i++) {

                    resultArray[n + i] = text[n + IIth + i];

                }

            } else {

                for (int j = 0; j < overlaps; j++) {

                    resultArray[n + j] = text[n + filler + j];

                }

            }

        }

        return resultArray;

    }

}
