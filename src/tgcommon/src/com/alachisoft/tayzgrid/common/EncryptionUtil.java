/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.alachisoft.tayzgrid.common;

import java.io.UnsupportedEncodingException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil
{

    private static String algorithm = "DESede";
    private static String transformation = "DESede/CBC/PKCS5Padding";
    private static String s_key = "A41'D3a##asd[1-a;d zs[s`";
    private static String s_iv = "KKNWLCZU";

    public static byte[] ConvertStringToByteArray(String s)
    {
        try
        {
            return s.getBytes("ASCII");
        }
        catch (UnsupportedEncodingException ex)
        {
            return null;
        }
    }

    public static byte[] Encrypt(String PlainText)
    {
        if (PlainText == null)
        {
            return null;
        }
        try
        {
            byte[] data = ConvertStringToByteArray(PlainText);

            byte[] k = ConvertStringToByteArray(s_key);
            byte[] IV = ConvertStringToByteArray(s_iv);
            SecretKeySpec securityKey = new SecretKeySpec(k, algorithm);
            Cipher encrypter = Cipher.getInstance(transformation);
            encrypter.init(Cipher.ENCRYPT_MODE, securityKey, new IvParameterSpec(IV));

            return encrypter.doFinal(data);

        }
        catch (Exception e)
        {
            return null;
        }
    }
    public static String Decrypt(byte[] CypherText)
    {
        if (CypherText == null)
        {
            return new String();
        }
        try
        {
            byte[] k = ConvertStringToByteArray(s_key);
            byte[] IV = ConvertStringToByteArray(s_iv);
            SecretKeySpec securityKey = new SecretKeySpec(k, algorithm);
            Cipher encrypter = Cipher.getInstance(transformation);
            encrypter.init(Cipher.DECRYPT_MODE, securityKey, new IvParameterSpec(IV));
            return new String(encrypter.doFinal(CypherText), "ASCII");
        }
        catch (Exception ex)
        {
            return null;
        }

    }

    /// <summary>
    /// Encrypt user provided key with the default key stored; This key is obfuscated
    /// </summary>
    /// <param name="key">Key</param>
    /// <returns>encrypted string</returns>
    public static String EncryptKey(String key)
    {
        if (key == null && key.isEmpty())
        {
            return new String();
        }
        try
        {
            byte[] data = key.getBytes("US-ASCII");
            byte[] k = ConvertStringToByteArray(s_key);
            byte[] IV = ConvertStringToByteArray(s_iv);
            SecretKeySpec securityKey = new SecretKeySpec(k, algorithm);
            Cipher encrypter = Cipher.getInstance(transformation);
            encrypter.init(Cipher.ENCRYPT_MODE, securityKey, new IvParameterSpec(IV));

            byte[] encryptedText = encrypter.doFinal(data);

            return Base64.encodeBytes(encryptedText);
        }
        catch (Exception ex)
        {
             return null;
        }

    }

    public static String DecryptKey(String encodedkey)
    {
        if (encodedkey == null && encodedkey.isEmpty())
        {
            return new String();
        }

        try
        {
            byte[] data = Base64.decode(encodedkey);

            byte[] k = ConvertStringToByteArray(s_key);
            byte[] IV = ConvertStringToByteArray(s_iv);
            SecretKeySpec securityKey = new SecretKeySpec(k, algorithm);
            Cipher encrypter = Cipher.getInstance(transformation);
            encrypter.init(Cipher.DECRYPT_MODE, securityKey, new IvParameterSpec(IV));

            byte[] encryptedText = encrypter.doFinal(data);
            return new String(encryptedText,"US-ASCII");
        }
        catch (Exception ex)
        {
           return null;
        }

    }


}
