package com.platbread.vrix.adplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import com.platbread.vrix.adsdk.VrixAdsdk;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    VideoView videoView;
    Context mContex;
    FrameLayout vadp;
    VrixAdsdk vrixAdsdk;
    MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mContex = this;


        /**
         * start button
         */
        Button start_btn = findViewById(R.id.start_btn);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vadp.setVisibility(View.VISIBLE);
                videoView.seekTo(0);
                videoView.pause();
                vrixAdStart();
            }
        });


        init();
    }

    public void init(){
        /**
         * 샘플 본영상 플레이어
         */
        String videoFile = "http://devgp.vrixon.com/vrixplayer/comm/res_sample/sampleMV_1080p.mp4";
        videoView = (VideoView)findViewById(R.id.videoView);
        videoView.setVideoPath( videoFile );
        videoView.seekTo(0);

        mediaController = new MediaController(mContex);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {

                videoView.setMediaController(mediaController);

                mediaController.setAnchorView(videoView);
                mediaController.setPadding(30, 0, 30, 30);

                LinearLayout viewGroupLevel1 = (LinearLayout)  mediaController.getChildAt(0);
                viewGroupLevel1.setBackgroundColor(getResources().getColor(R.color.AlmostTransparent));
            }
        });



        /**
         * Layout for VRiX AD SDK Container
         */
        vadp = (FrameLayout) findViewById(R.id.vadplayer);
        vadp.setVisibility(View.GONE);

        /**
         *  Define VRiX AD SDK
         */
        JSONObject adOptions = new JSONObject();

        try {
           // vadp.setVisibility(View.VISIBLE);

            adOptions.put("adTag", "https://devads.vrixon.com/vast/vast.vrix?invenid=KHLOC");
            adOptions.put("autostart",true);

            vrixAdsdk = new VrixAdsdk(this,adOptions);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            vrixAdsdk.setLayoutParams(params);

            vadp.addView(vrixAdsdk);
            vrixAdsdk.addEventListener(new VrixAdsdk.VrixADPLayoutListener() {
                @Override
                public void onAdPlaying() {
                    Log.d("VRIX", "@@@@@@@@@@@@@@ onAdPlaying");
                }

                @Override
                public void onAdComplete() {
                    Log.d("VRIX", "@@@@@@@@@@@@@@ onAdComplete");
                    vadp.setVisibility(View.GONE);
                    videoView.start();
                    mediaController.show();
                }

                @Override
                public void onAdClicked() {
                    Log.d("VRIX", "@@@@@@@@@@@@@@ onAdClicked");
                }

                @Override
                public void onError(String msg) {
                    Log.d("VRIX", "@@@@@@@@@@@@@@ onError : "+ msg);
                    vadp.setVisibility(View.GONE);
                    videoView.start();
                    mediaController.show();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            vadp.setVisibility(View.GONE);
            videoView.start();
            mediaController.show();
        }
    }

    public void vrixAdStart(){
        mediaController.hide();
        vrixAdsdk.play();
    }
}
