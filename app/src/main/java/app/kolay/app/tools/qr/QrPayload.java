package app.kolay.app.tools.qr;

/** Text that phones understand when they scan a QR: Wi-Fi join strings and vCards. Pure Java. */
public final class QrPayload {
    private QrPayload() {}

    public enum Security { WPA, WEP, OPEN }

    private static final String WIFI_SPECIAL = "\\;,:\"";

    /** ZXing/Android "WIFI:" format; \ ; , : " are backslash-escaped. */
    public static String wifi(String ssid, String password, Security sec, boolean hidden) {
        StringBuilder b = new StringBuilder("WIFI:T:").append(sec == Security.OPEN ? "nopass" : sec.name()).append(";S:").append(esc(ssid, WIFI_SPECIAL)).append(';');
        if (sec != Security.OPEN && !password.isEmpty()) b.append("P:").append(esc(password, WIFI_SPECIAL)).append(';');
        if (hidden) b.append("H:true;");
        return b.append(';').toString();
    }

    /** vCard 3.0 with name, phone, e-mail and organisation; empty parts are left out. */
    public static String vcard(String name, String phone, String email, String org) {
        String n = name.trim();
        int sp = n.lastIndexOf(' ');
        String first = sp < 0 ? n : n.substring(0, sp).trim(), last = sp < 0 ? "" : n.substring(sp + 1);
        String e = "\\,;";
        StringBuilder b = new StringBuilder("BEGIN:VCARD\r\nVERSION:3.0\r\n");
        b.append("N:").append(esc(last, e)).append(';').append(esc(first, e)).append(";;;\r\n");
        b.append("FN:").append(esc(n, e)).append("\r\n");
        if (!org.trim().isEmpty()) b.append("ORG:").append(esc(org.trim(), e)).append("\r\n");
        if (!phone.trim().isEmpty()) b.append("TEL;TYPE=CELL:").append(phone.trim().replaceAll("[^0-9+]", "")).append("\r\n");
        if (!email.trim().isEmpty()) b.append("EMAIL:").append(esc(email.trim(), e)).append("\r\n");
        return b.append("END:VCARD").toString();
    }

    private static String esc(String s, String special) {
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == '\n') { b.append("\\n"); continue; }
            if (ch == '\r') continue;
            if (special.indexOf(ch) >= 0) b.append('\\');
            b.append(ch);
        }
        return b.toString();
    }
}
