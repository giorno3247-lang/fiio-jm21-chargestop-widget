package jp.giorno.chargestop;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private TextView statusView;
    private Button toggleButton;
    private Button shizukuButton;
    private String errorNote = "";

    private final Shizuku.OnBinderReceivedListener binderListener =
            new Shizuku.OnBinderReceivedListener() {
                @Override
                public void onBinderReceived() {
                    runOnUiThread(new Runnable() {
                        @Override public void run() { refresh(); }
                    });
                }
            };

    private final Shizuku.OnRequestPermissionResultListener permListener =
            new Shizuku.OnRequestPermissionResultListener() {
                @Override
                public void onRequestPermissionResult(int requestCode, int grantResult) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() { refresh(); }
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("充電停止トグル");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        statusView = new TextView(this);
        statusView.setTextSize(17);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, pad, 0, pad);
        root.addView(statusView);

        toggleButton = new Button(this);
        toggleButton.setTextSize(18);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ShizukuExec.isBinderAlive()) {
                    Toast.makeText(MainActivity.this,
                            "Shizukuが動いていません。Shizukuを起動してください", Toast.LENGTH_LONG).show();
                    return;
                }
                if (!ShizukuExec.isGranted()) {
                    Toast.makeText(MainActivity.this,
                            "先にShizukuの許可が必要です", Toast.LENGTH_LONG).show();
                    return;
                }
                final boolean next = !ChargeStop.isOn(MainActivity.this);
                toggleButton.setEnabled(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final boolean ok = ShizukuExec.setSync(MainActivity.this, next);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                errorNote = ok ? "" : ShizukuExec.lastError;
                                toggleButton.setEnabled(true);
                                Toast.makeText(MainActivity.this,
                                        ok ? ("充電停止を " + (next ? "ON" : "OFF") + " にしました")
                                           : "切り替えに失敗しました",
                                        Toast.LENGTH_SHORT).show();
                                refresh();
                            }
                        });
                    }
                }).start();
            }
        });
        root.addView(toggleButton);

        shizukuButton = new Button(this);
        shizukuButton.setText("Shizukuの許可をリクエスト");
        shizukuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ShizukuExec.shouldShowRationale()) {
                    Toast.makeText(MainActivity.this,
                            "許可が拒否されています。Shizukuアプリ側から許可してください", Toast.LENGTH_LONG).show();
                    return;
                }
                ShizukuExec.requestPermission();
            }
        });
        root.addView(shizukuButton);

        TextView note = new TextView(this);
        note.setText("\nFiiO JM21 の充電停止（charging_disable）を、Shizuku経由（shell権限）で切り替えます。\n"
                + "Shizukuアプリのインストールと起動が必要です。JM21を再起動した後は、Shizukuの再起動（adbコマンド）が必要です。\n"
                + "ホーム画面にウィジェットを置くと、タップで切り替えできます。");
        note.setGravity(Gravity.CENTER);
        root.addView(note);

        setContentView(root);

        try {
            Shizuku.addBinderReceivedListenerSticky(binderListener);
            Shizuku.addRequestPermissionResultListener(permListener);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Shizuku.removeBinderReceivedListener(binderListener);
            Shizuku.removeRequestPermissionResultListener(permListener);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean on = ChargeStop.isOn(this);
        boolean alive = ShizukuExec.isBinderAlive();
        boolean granted = alive && ShizukuExec.isGranted();
        String shizukuState = !alive ? "停止中（未起動 or 未インストール）"
                : (granted ? "実行中・許可済み" : "実行中・未許可");
        statusView.setText("現在の状態：充電停止 " + (on ? "ON" : "OFF")
                + "\nShizuku：" + shizukuState
                + (errorNote.isEmpty() ? "" : "\nエラー詳細：" + errorNote));
        toggleButton.setText(on ? "充電停止を OFF にする" : "充電停止を ON にする");
        shizukuButton.setVisibility(alive && !granted ? View.VISIBLE : View.GONE);
        ToggleWidgetProvider.refreshAll(this);
    }
}
