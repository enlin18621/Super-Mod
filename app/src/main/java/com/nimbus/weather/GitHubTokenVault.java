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

/**
 * Only used for the user's own dedicated private GitHub repository, NOT the public app-source repo.
 * PAT stays on Android and is never sent to OpenAI, ChatGPT, or logs.
 */
final class GitHubTokenVault {
    private static final String ALIAS="nimbus_private_github_sync_v1";
    private static SecretKey key()throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(ALIAS))return (SecretKey)ks.getKey(ALIAS,null);
        KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build());
        return g.generateKey();
    }
    static void save(Context c,String plain)throws Exception{
        if(plain==null||plain.trim().isEmpty())return;
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] encrypted=cipher.doFinal(plain.trim().getBytes(StandardCharsets.UTF_8));
        Prefs.get(c).edit().putString("gh_token_cipher",
            Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+
            Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply();
    }
    static String read(Context c){
        String stored=Prefs.get(c).getString("gh_token_cipher","");
        if(stored.isEmpty())return "";
        try{
            String[] parts=stored.split(":",2);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
        }catch(Exception e){return "";}
    }
    static void remove(Context c){Prefs.get(c).edit().remove("gh_token_cipher").apply();}
}
