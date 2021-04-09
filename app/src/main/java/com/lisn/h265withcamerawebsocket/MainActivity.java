package com.lisn.h265withcamerawebsocket;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CheckBox;

public class MainActivity extends AppCompatActivity {

    private LiveManager liveManager;
    private CheckBox checkbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SurfaceView localSurfaceView = findViewById(R.id.localSurfaceView);
        SurfaceView remoteSurfaceView = findViewById(R.id.remoteSurfaceView);
        checkbox = findViewById(R.id.checkbox);

        // localSurfaceView放置在顶层，即始终位于最上层
        localSurfaceView.setZOrderOnTop(true);
        requestPermission();

        liveManager = new LiveManager(localSurfaceView.getHolder(), remoteSurfaceView.getHolder());

        // 因为是H265,你可以采用4K的分辨率
        liveManager.init(540, 960);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    public void start(View view) {
        liveManager.start(checkbox.isChecked());
    }

    public void stop(View view) {
        liveManager.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        liveManager.stop();
    }
}