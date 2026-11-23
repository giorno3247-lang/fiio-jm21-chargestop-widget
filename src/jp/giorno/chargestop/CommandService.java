package jp.giorno.chargestop;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Shizuku の UserService として shell 権限で動くサービス。
 * adb shell で成功した `settings put system charging_disable X` をそのまま実行する。
 */
public class CommandService extends ICommandService.Stub {

    public CommandService() {}

    @Override
    public int setChargingDisable(int value) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                    "settings", "put", "system", "charging_disable", String.valueOf(value)});
            // 出力を読み捨ててブロックを防ぐ
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (r.readLine() != null) { /* discard */ }
            return p.waitFor();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}
