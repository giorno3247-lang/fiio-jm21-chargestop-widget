package jp.giorno.chargestop;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;

/** Shizuku の UserService（CommandService）を束ねて、shell 権限で charging_disable を書き換える。 */
public final class ShizukuExec {
    public static String lastError = "";

    private static ICommandService service;
    private static final Object LOCK = new Object();

    private ShizukuExec() {}

    public static boolean isBinderAlive() {
        try {
            return Shizuku.pingBinder();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isGranted() {
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean shouldShowRationale() {
        try {
            return Shizuku.shouldShowRequestPermissionRationale();
        } catch (Exception e) {
            return false;
        }
    }

    public static void requestPermission() {
        Shizuku.requestPermission(1);
    }

    /**
     * 同期実行（バックグラウンドスレッドから呼ぶこと）。
     * UserService に接続し、settings put を実行して、読み戻しで確認する。
     */
    public static boolean setSync(Context ctx, boolean on) {
        lastError = "";
        if (!isBinderAlive()) {
            lastError = "Shizukuが動いていません";
            return false;
        }
        if (!isGranted()) {
            lastError = "Shizukuの許可がありません";
            return false;
        }
        try {
            ICommandService svc = obtainService(ctx);
            if (svc == null) {
                lastError = "UserServiceに接続できませんでした";
                return false;
            }
            int rc = svc.setChargingDisable(on ? 1 : 0);
            if (rc != 0) {
                lastError = "settings put の終了コード " + rc;
                return false;
            }
        } catch (Exception e) {
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            service = null;
            return false;
        }
        // 読み戻して確認（少し待ってから）
        for (int i = 0; i < 10; i++) {
            if (ChargeStop.isOn(ctx) == on) return true;
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
        lastError = "書き込み後に値が変わりませんでした";
        return false;
    }

    private static ICommandService obtainService(Context ctx) throws InterruptedException {
        synchronized (LOCK) {
            if (service != null) {
                try {
                    service.asBinder().pingBinder();
                    return service;
                } catch (Exception e) {
                    service = null;
                }
            }
            final CountDownLatch latch = new CountDownLatch(1);
            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                    new ComponentName(ctx.getPackageName(), CommandService.class.getName()))
                    .daemon(false)
                    .tag("chargestop")
                    .processNameSuffix("service")
                    .debuggable(false)
                    .version(3);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder binder) {
                    service = ICommandService.Stub.asInterface(binder);
                    latch.countDown();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    service = null;
                }
            };
            Shizuku.bindUserService(args, conn);
            latch.await(8, TimeUnit.SECONDS);
            return service;
        }
    }
}
