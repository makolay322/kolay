package app.kolay.shell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarm, boot and clock/zone changes: all they do is hand over to {@link Prayer}. */
public class PrayerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        String a = i == null ? null : i.getAction();
        if (Prayer.ACTION_REMIND.equals(a)) Prayer.remind(c, i.getIntExtra("i", -1), i.getLongExtra("end", 0));
        else Prayer.refresh(c);   // tick, boot, time/zone change, app replaced
    }
}
