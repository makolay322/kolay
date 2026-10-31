package app.kolay.payload.tools.voice;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Microphone -> 16 kHz mono PCM, held in memory only (nothing is written to disk). Stops by itself at {@link #MAX_SECONDS}.
 * Needs RECORD_AUDIO, which the screen requests before {@link #start()}.
 */
final class Recorder {
    static final int RATE = 16000, MAX_SECONDS = 300;
    /** Below this peak (of 32767) the clip is treated as silence and not sent anywhere. */
    private static final int SILENCE_PEAK = 400;

    private final ByteArrayOutputStream pcm = new ByteArrayOutputStream(1 << 20);
    private AudioRecord rec;
    private Thread thread;
    private volatile boolean running, limitHit;
    private volatile float level;     // 0..1, last chunk
    private volatile int peak;

    @SuppressWarnings("MissingPermission")
    void start() throws IOException {
        int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) throw new IOException("Mikrofon bu cihazda kullanılamıyor");
        AudioRecord r = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, RATE));
        if (r.getState() != AudioRecord.STATE_INITIALIZED) { r.release(); throw new IOException("Mikrofon açılamadı (başka uygulama kullanıyor olabilir)"); }
        rec = r;
        running = true;
        r.startRecording();
        thread = new Thread(this::loop, "voice-rec");
        thread.start();
    }

    private void loop() {
        byte[] buf = new byte[RATE / 5 * 2];  // 200 ms
        final int cap = RATE * 2 * MAX_SECONDS;
        while (running) {
            int n = rec.read(buf, 0, buf.length);
            if (n < 0) break;
            if (n == 0) continue;
            long sum = 0; int pk = 0;
            for (int i = 0; i + 1 < n; i += 2) {
                int s = (short) ((buf[i] & 0xFF) | (buf[i + 1] << 8));
                int a = Math.abs(s);
                if (a > pk) pk = a;
                sum += (long) s * s;
            }
            double rms = Math.sqrt(sum / Math.max(1.0, n / 2.0));
            level = (float) Math.min(1.0, rms / 5000.0);
            if (pk > peak) peak = pk;
            synchronized (pcm) { pcm.write(buf, 0, n); if (pcm.size() >= cap) { limitHit = true; running = false; } }
        }
    }

    boolean running() { return running; }
    boolean limitHit() { return limitHit; }
    float level() { return level; }
    boolean silent() { return peak < SILENCE_PEAK; }
    long millis() { synchronized (pcm) { return pcm.size() / (RATE * 2L / 1000L); } }

    /** Stops the microphone and returns the clip as WAV bytes. Safe to call twice (second call returns the same clip). */
    byte[] stop() {
        running = false;
        try { if (thread != null) thread.join(1500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        if (rec != null) {
            try { rec.stop(); } catch (Exception ignored) { }
            rec.release();
            rec = null;
        }
        byte[] data;
        synchronized (pcm) { data = pcm.toByteArray(); }
        return wav(data);
    }

    /** Throws the clip away (screen closed): stops the microphone, keeps nothing. */
    void discard() { stop(); synchronized (pcm) { pcm.reset(); } }

    private static byte[] wav(byte[] pcm16) {
        int n = pcm16.length;
        byte[] out = new byte[44 + n];
        put(out, 0, "RIFF"); le32(out, 4, 36 + n); put(out, 8, "WAVE"); put(out, 12, "fmt ");
        le32(out, 16, 16); out[20] = 1; out[22] = 1;                // PCM, mono
        le32(out, 24, RATE); le32(out, 28, RATE * 2);              // sample rate, byte rate
        out[32] = 2; out[34] = 16;                                  // block align, bits
        put(out, 36, "data"); le32(out, 40, n);
        System.arraycopy(pcm16, 0, out, 44, n);
        return out;
    }

    private static void put(byte[] b, int at, String s) { for (int i = 0; i < s.length(); i++) b[at + i] = (byte) s.charAt(i); }
    private static void le32(byte[] b, int at, int v) { b[at] = (byte) v; b[at + 1] = (byte) (v >> 8); b[at + 2] = (byte) (v >> 16); b[at + 3] = (byte) (v >> 24); }
}
