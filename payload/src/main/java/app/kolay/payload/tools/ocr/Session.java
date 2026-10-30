package app.kolay.payload.tools.ocr;

import android.graphics.Bitmap;
import android.net.Uri;
import java.util.*;

/** State shared by the screens of one OCR flow: the recent photos, the selection and the thumbnail cache. */
final class Session {
    final List<Uri> photos = new ArrayList<>();
    final LinkedHashSet<Uri> selected = new LinkedHashSet<>();
    final Map<Uri, Bitmap> thumbs = new HashMap<>();
}
