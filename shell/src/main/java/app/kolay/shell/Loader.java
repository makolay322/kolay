package app.kolay.shell;

import android.content.Context;
import app.kolay.sdk.Module;
import dalvik.system.DexClassLoader;
import java.nio.file.Files;

/** Validates and loads one payload. Throws on anything suspicious; the caller falls back to the next candidate. */
final class Loader {
    private Loader() {}

    static Module load(Context ctx, Registry reg, Registry.Cand c) throws Exception {
        if (!c.apk.canExecute() && c.apk.canWrite()) c.apk.setReadOnly();
        Registry.Meta m = reg.readMeta(c.apk);
        if (m == null) throw new IllegalStateException("payload unreadable");
        if (!Config.PAYLOAD_PKG.equals(m.pkg)) throw new SecurityException("wrong payload package: " + m.pkg);
        if (m.vc != c.vc) throw new IllegalStateException("payload version mismatch");
        if (m.api < Config.MIN_PAYLOAD_API || m.api > Config.SHELL_API)
            throw new IllegalStateException("payload needs API " + m.api + ", shell offers " + Config.SHELL_API);
        if (!c.bundled) {  // downloaded code: re-check authenticity every time (cheap, guards against corruption)
            if (!c.sig.isFile() || !Verify.signature(c.apk, Files.readAllBytes(c.sig.toPath()), Config.UPDATE_PUBLIC_KEYS))
                throw new SecurityException("payload signature invalid");
        }
        DexClassLoader cl = new DexClassLoader(c.apk.getAbsolutePath(), null, null, Loader.class.getClassLoader());
        Object o = Class.forName(Config.ENTRY_CLASS, true, cl).getDeclaredConstructor().newInstance();
        return (Module) o;
    }
}
