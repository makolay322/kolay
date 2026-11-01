package app.kolay.shell;

import java.io.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Pure-Java crypto helpers (no Android types) so they can be tested on a desktop JVM. */
final class Verify {
    private Verify() {}

    static String sha256(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) { return sha256(in); }
    }

    static String sha256(InputStream in) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = new byte[65536]; int n;
            while ((n = in.read(b)) > 0) md.update(b, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte x : md.digest()) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { throw new IOException(e); }
    }

    /** ECDSA/SHA-256 (DER signature, as produced by `openssl dgst -sha256 -sign`). Fails closed on any error. */
    static boolean signature(File data, byte[] sigDer, String[] pubKeysB64) {
        for (String k : pubKeysB64) if (signature(data, sigDer, k)) return true;
        return false;
    }

    static boolean signature(File data, byte[] sigDer, String pubB64) {
        try {
            PublicKey k = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pubB64)));
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(k);
            try (InputStream in = new FileInputStream(data)) {
                byte[] b = new byte[65536]; int n;
                while ((n = in.read(b)) > 0) s.update(b, 0, n);
            }
            return s.verify(sigDer);
        } catch (Exception e) { return false; }
    }
}
