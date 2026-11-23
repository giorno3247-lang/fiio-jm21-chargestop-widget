package jp.giorno.chargestop;

import android.content.Context;
import android.provider.Settings;

/** FiiO JM21 の充電停止（Settings.System: charging_disable）の読み取り。書き込みは ShizukuExec 経由。 */
public final class ChargeStop {
    public static final String KEY = "charging_disable";

    private ChargeStop() {}

    public static boolean isOn(Context ctx) {
        try {
            return Settings.System.getInt(ctx.getContentResolver(), KEY, 0) == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
