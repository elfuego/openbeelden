/*

This file is part of the Open Images Platform, a webapplication to manage and publish open media.
    Copyright (C) 2011 Netherlands Institute for Sound and Vision

The Open Images Platform is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Open Images Platform is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with The Open Images Platform.  If not, see <http://www.gnu.org/licenses/>.

*/

package eu.openimages.api;

import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.mmbase.util.transformers.Hex;
import org.mmbase.util.transformers.Base64;
import org.mmbase.util.logging.*;

/**
 * Generates (encrypts) and decodes API tokens. 
 * An API key consists of a username and (encoded) password, seperated by a token and 
 * encrypted with a secret key. The key is used to encrypt en decrypt the API token,
 * which enables the system to retreive a username and password from it when for example it 
 * receives a post using the API token. It is stored as a property on the mmbaseusers builder.
 * This class uses Blowfish as its default algorithm, use {@link #setAlgorithm} to change it.
 * Default the API key is hex encoded, you can change that to base64 if you like with {@link #setFormat}.
 *
 * @author Andr&eacute; van Toly
 * @version $Id$
 */
public final class ApiToken {
    
    private Logger log = Logging.getLoggerInstance(ApiToken.class);

    private static String SEPERATOR = "=:=";
    private String key = "pindakaas";
    private String algorithm = "Blowfish";
    private String format = "hex";

    /* Default is 'hex', only that and 'base64' are possible. */
    public void setFormat(String f) {
        if ("base64".equals(f) || "hex".equals(f)) {
            format = f;
        }
    }
    /* You may differ your algorithm from Blowfish. */
    public void setAlgorithm(String a) {
        algorithm = a;
    }

    /**
     * Makes cq. encrypts apikey and puts username and password in it.
     *
     * @param user  Username that enables a user to login 
     * @param pw    The encoded version of password
     * @param key   Secret key which encrypts it all
     * @return      An encrypted string that can be used as an API token
     */
    public String encrypt(String user, String pw, String key) {
        StringBuilder sb = new StringBuilder();
        /* if (log.isDebugEnabled()) {
            log.debug("user: " + user + "/" + pw + ", key: " + key);
        } */
        sb.append(user).append(SEPERATOR).append(pw);
        
        try {
            byte input[] = sb.toString().getBytes(); 
            
            byte[] secretKey = key.getBytes();
            SecretKeySpec skeySpec = new SecretKeySpec(secretKey, algorithm);
            
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte encrypted[] = cipher.doFinal(input);
            
            String encoded = encode(encrypted, format);
            /* if (log.isDebugEnabled()) {
               log.debug("API token for user " + user + " is " + encoded + " (format: " + format + ")");
            } */
            return encoded;
            
        } catch (java.security.GeneralSecurityException gse) {
            log.error("GeneralSecurityException " + gse.getMessage());
        }

        return null;
    }
    public String encrypt(String user, String pw) {
        return encrypt(user, pw, key);
    }

    /**
     * Decrypts apikey and returns Map with username and password from it.
     *
     * @param apitoken  The apitoken to decrypt using the provided key
     * @param key       Secret key that encrypts it
     * @return          A map with username and password extracted from the API token
     */
    public Map<String,String> decrypt(String apitoken, String key) throws IllegalArgumentException, java.security.GeneralSecurityException {
        Map<String,String> map = new HashMap<String,String>();
        
        byte[] input = decode(apitoken, format);
        byte[] secretKey = key.getBytes();
        SecretKeySpec skeySpec = new SecretKeySpec(secretKey, algorithm);
        
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte decrypted[] = cipher.doFinal(input);
        String output = new String(decrypted);
        
        java.util.StringTokenizer toz = new java.util.StringTokenizer(output, SEPERATOR);
        if (toz.countTokens() == 2) {
            String us = toz.nextToken();
            String pw = toz.nextToken();
            if (log.isDebugEnabled()) {
                log.debug("returning: " + us + "/" + pw);
            }
            map.put("username", us);
            map.put("password", pw);
        }
        return map;
    }
    public Map<String,String> decrypt(String apitoken) throws IllegalArgumentException, java.security.GeneralSecurityException {
        return decrypt(apitoken, key);
    }


    /**
     * Encode a given array of bytes to a string, using the given format.
     * This can be 'hex' or 'base64'.
     */
    private static String encode(byte input[], String format) throws java.security.NoSuchAlgorithmException {
        if ("hex".equalsIgnoreCase(format)) {
            Hex h = new Hex();
            String output = h.transform(input);
            return output;
        } else if ("base64".equalsIgnoreCase(format)) {
            Base64 b = new Base64();
            String output = b.transform(input);
            return output;
        }
        return "";
    }

    /**
     * Decode a given string to an array of bytes, using a given format.
     * This can throw an 'IllegalArgumentException' when the given input
     * string isn't correct according to the format.
     */
    private static byte[] decode(String input, String format) throws IllegalArgumentException {
        if ("hex".equalsIgnoreCase(format)) {
            Hex h = new Hex();
            byte[] output = h.transformBack(input);
            return output;
        } else if ("base64".equalsIgnoreCase(format)) {
            Base64 b = new Base64();
            byte[] output = b.transformBack(input);
            return output;
        }
        return new byte[0];
    }
    
}
