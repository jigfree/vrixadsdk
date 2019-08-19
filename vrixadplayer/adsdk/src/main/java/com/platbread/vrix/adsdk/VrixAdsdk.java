package com.platbread.vrix.adsdk;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONException;
import org.json.JSONObject;


public class VrixAdsdk extends FrameLayout {
    private Context mContext;

    private String loadURI = "http://devgp.vrixon.com/vadpm/vadpm.html?pform=android"; // Default SBTV page URI
    private WebView vrixWebview;

    private VrixBridge mBridge;
    private JSONObject adOptions;

    private VrixADPLayoutListener mVrixADPLayoutListener;

    public interface VrixADPLayoutListener{
        public void onAdPlaying();
        public void onAdComplete();
        public void onAdClicked();
        public void onError(String msg);
    }

    public VrixAdsdk(Context context, JSONObject opt){
        super(context);
        mContext = context;
        adOptions = opt;
        init();
    }

    public void init(){
        Log.d("VRIX","init::"+mContext);

        mBridge = new VrixBridge();
        LayoutParams vparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        vrixWebview = new WebView(this.getContext());
        vrixWebview.getSettings().setJavaScriptEnabled(true); // 자바스크립트 사용을 허용한다.
        vrixWebview.setWebViewClient(new WebViewClientClass());  // 새로운 창을 띄우지 않고 내부에서 웹뷰를 실행시킨다.
        vrixWebview.addJavascriptInterface(mBridge, "VrixBridge");
        vrixWebview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        vrixWebview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        this.addView(vrixWebview);
    }

    public void play(){
        vrixWebview.loadUrl(loadURI);
    }

    public void addEventListener(VrixADPLayoutListener listener){
        mVrixADPLayoutListener = listener;
    }

    protected final class VrixBridge {

        public VrixBridge() {
        }

        /**
         * 참고(변경됨): web 에서 js 스크립트 로드 완료 된후 onReady 를 호출
         * @param
         */
        @JavascriptInterface
        public void onReady() {
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("status", "ON_READY");
                        jsonObject.put("adOpt", adOptions);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    StringBuilder sendObj = new StringBuilder();
                    sendObj.append(jsonObject);
                    
                    Log.d("VRIX","sendObj::"+sendObj);

                    vrixWebview.loadUrl("javascript:onStateChange("+sendObj+")");
                }
            });
        }

        /**
         * 참고 : 이벤트 시점에 대해 명확한 다음의 항목을 제외하고는 콜백 하지 않음
         *
         * @param arg
         */
        @JavascriptInterface
        public void onStateChange(final String arg) {
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (arg) {
                        case "SDK_BIG_PLAY_BTN_CLICKED":
                            Log.d("VRIX", "SDK_BIG_PLAY_BTN_CLICKED");
                            if(mVrixADPLayoutListener != null){
                                mVrixADPLayoutListener.onAdClicked();
                            }
                            break;

                        case "SDK_PLAYING":
                            Log.d("VRIX", "SDK_PLAYING");
                            if(mVrixADPLayoutListener != null){
                                mVrixADPLayoutListener.onAdPlaying();
                            }
                            break;

                        case "SDK_ENDING":
                            Log.d("VRIX", "SDK_ENDING");
                            if(mVrixADPLayoutListener != null){
                                mVrixADPLayoutListener.onAdComplete();
                            }
/*
                            JSONObject jsonObject = new JSONObject();

                            try {
                                jsonObject.put("status", "RELOAD");


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                            StringBuilder sendObj = new StringBuilder();
                            sendObj.append(jsonObject);

                            vrixWebview.loadUrl("javascript:onStateChange("+sendObj+")");*/
                            break;
                        default:
                            break;
                    }
                }
            });
        }

        /**
         * Error event
         * @param msg
         */
        @JavascriptInterface
        public void onError(final String msg){
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mVrixADPLayoutListener != null){
                        mVrixADPLayoutListener.onError(msg);
                    }
                }
            });
        }

    }



    /**
     * WebViewClientClass 정의
     */
    private class WebViewClientClass extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // 만약에 있을 iframe 정상 구동을 위해서 url 재정의를 하지 않는다.
            //view.loadUrl(url);
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                CookieSyncManager.getInstance().sync();
            } else {
                CookieManager.getInstance().flush();
            }
            vrixWebview.invalidate();
        }
    }
}
