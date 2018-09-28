package com.github.voice.broadcast;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private SoundPlayUtils mSoundPlayUtils = SoundPlayUtils.newInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i= 1; i< 10; i++) {
                    mSoundPlayUtils.playNumber("0.0" + i);
                }
            }
        });
    }
}
