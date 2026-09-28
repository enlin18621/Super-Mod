package com.nimbus.weather;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

/** Encrypts an optional API key locally. Never commit keys to GitHub or put them in URLs. */
final class SafeKeyStore {
    private static final String ALIAS="nimbus_openai_key_v1";
    private static SecretKey key()throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(ALIAS))return (SecretKey)store.getKey(ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build());
        return generator.generateKey();
    }
    static void save(Context c,String plain)throws Exception{
        if(plain==null||plain.trim().isEmpty()){Prefs.get(c).edit().remove("ai_key").apply();return;}
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] bytes=cipher.doFinal(plain.trim().getBytes(StandardCharsets.UTF_8));
        String stored=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(bytes,Base64.NO_WRAP);
        Prefs.get(c).edit().putString("ai_key",stored).apply();
    }
    static String read(Context c){
        String enc=Prefs.get(c).getString("ai_key","");
        if(enc.isEmpty())return "";
        try{
            String[] parts=enc.split(":",2);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
        }catch(Exception ex){return "";}
    }
    static void remove(Context c){Prefs.get(c).edit().remove("ai_key").apply();}
}
