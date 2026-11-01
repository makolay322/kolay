package app.kolay.app.tools.qr;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Thin ZXing wrapper for one luminance frame: QR codes, or (product mode) the 1-D retail barcodes.
 * Not thread-safe: use from a single thread. Pure Java, so it can be tried on a desktop JVM.
 */
final class QrDecoder {
    private final Reader reader;
    private final boolean product;
    private final Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
    private int misses;

    QrDecoder() { this(false); }

    QrDecoder(boolean product) {
        this.product = product;
        if (product) {
            reader = new MultiFormatReader();
            hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E, BarcodeFormat.CODE_128));
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        } else {
            reader = new QRCodeReader();
            hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
        }
    }

    /**
     * @param y        luminance plane, {@code rowStride} bytes per row
     * @param width    visible width in pixels (≤ rowStride)
     * @return the decoded text, or null when the frame holds no readable code
     */
    String decode(byte[] y, int rowStride, int width, int height) {
        if (product) return decodeProduct(y, rowStride, width, height);
        LuminanceSource src = new PlanarYUVLuminanceSource(y, rowStride, height, 0, 0, width, height, false);
        String r = tryDecode(src);
        // Light-on-dark codes are rare, so the inverted attempt only runs on every third miss to keep frames fast.
        if (r == null && ++misses % 3 == 0) r = tryDecode(src.invert());
        return r;
    }

    /**
     * The phone is held upright, so a horizontal barcode runs along the sensor's short side. 1-D readers only scan rows,
     * hence the frame is read rotated by 90 degrees (direction is irrelevant: readers handle mirrored bars); every other
     * miss also tries the frame as delivered, for a phone held sideways.
     */
    private String decodeProduct(byte[] y, int stride, int w, int h) {
        byte[] rot = new byte[w * h];
        for (int r = 0; r < h; r++) {
            int base = r * stride;
            for (int x = 0; x < w; x++) rot[x * h + (h - 1 - r)] = y[base + x];
        }
        String res = tryDecode(new PlanarYUVLuminanceSource(rot, h, w, 0, 0, h, w, false));
        if (res == null && ++misses % 2 == 0) res = tryDecode(new PlanarYUVLuminanceSource(y, stride, h, 0, 0, w, h, false));
        return res;
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
