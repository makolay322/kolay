package app.kolay.shell;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Device-specific secret (API key), encrypted with a non-exportable Android Keystore key. Never part of the repo or the APK. */
final class SecretStore {
    private static final String ALIAS = "kolay_secret_v1";
    private final SharedPreferences p;

    SecretStore(Context c) { p = c.getApplicationContext().getSharedPreferences("secrets", Context.MODE_PRIVATE); }

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (ks.containsAlias(ALIAS)) return (SecretKey) ks.getKey(ALIAS, null);
        KeyGenerator g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        g.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return g.generateKey();
    }

    void set(String name, String secret) {
        try {
            if (secret == null || secret.trim().isEmpty()) { p.edit().remove("s_" + name).apply(); return; }
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key());
            byte[] ct = c.doFinal(secret.trim().getBytes("UTF-8"));
            byte[] all = new byte[1 + c.getIV().length + ct.length];
            all[0] = (byte) c.getIV().length;
            System.arraycopy(c.getIV(), 0, all, 1, c.getIV().length);
            System.arraycopy(ct, 0, all, 1 + c.getIV().length, ct.length);
            p.edit().putString("s_" + name, Base64.encodeToString(all, Base64.NO_WRAP)).apply();
        } catch (Exception e) { throw new IllegalStateException("anahtar kaydedilemedi: " + e.getMessage()); }
    }

    String get(String name) {
        try {
            String s = p.getString("s_" + name, null);
            if (s == null) return "";
            byte[] all = Base64.decode(s, Base64.NO_WRAP);
            int ivLen = all[0];
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, all, 1, ivLen));
            return new String(c.doFinal(all, 1 + ivLen, all.length - 1 - ivLen), "UTF-8");
        } catch (Exception e) { return ""; }  // unreadable (e.g. keystore reset): behave as "not set"
    }
}
