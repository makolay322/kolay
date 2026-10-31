package app.kolay.payload.tools.qr;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Thin ZXing wrapper for one luminance frame. Not thread-safe: use from a single thread. Pure Java, so it can be tried on a desktop JVM. */
final class QrDecoder {
    private final QRCodeReader reader = new QRCodeReader();
    private final Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
    private int misses;

    QrDecoder() { hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE)); }

    /**
     * @param y        luminance plane, {@code rowStride} bytes per row
     * @param width    visible width in pixels (≤ rowStride)
     * @return the decoded text, or null when the frame holds no readable code
     */
    String decode(byte[] y, int rowStride, int width, int height) {
        LuminanceSource src = new PlanarYUVLuminanceSource(y, rowStride, height, 0, 0, width, height, false);
        String r = tryDecode(src);
        // Light-on-dark codes are rare, so the inverted attempt only runs on every third miss to keep frames fast.
        if (r == null && ++misses % 3 == 0) r = tryDecode(src.invert());
        return r;
    }

    private String tryDecode(LuminanceSource src) {
        try {
            return reader.decode(new BinaryBitmap(new HybridBinarizer(src)), hints).getText();
        } catch (ReaderException e) {
            return null;  // NotFound / Checksum / Format: no code in this frame
        } finally {
            reader.reset();
        }
    }
}
