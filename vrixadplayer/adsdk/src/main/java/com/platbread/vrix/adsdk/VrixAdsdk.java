package com.platbread.vrix.adsdk;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;



public class VrixAdsdk extends FrameLayout {
    private Context mContext;
    private String loadURI = "http://devgp.vrixon.com/vadpm/vadpm.html?pform=android";// &d_gom=1&d_ad=1"; //Default SBTV page URI
    private WebView vrixWebview;
    private VrixBridge mBridge;
    private JSONObject adOptions;
    private VrixADPLayoutListener mVrixADPLayoutListener;
    private GadidInfo gadidInfo;
    private GoogleAdIdTask googleAdIdTask;


    /**
     * Vrix Ad sdk version
     * @return
     */
    private String getVersion(){
        return com.platbread.vrix.adsdk.BuildConfig.VERSION_NAME;
    }


    /**
     * SDK interface
     */
    public interface VrixADPLayoutListener{
        public void onAdPlaying();
        public void onAdComplete();
        public void onAdClicked();
        public void onError(String msg);
    }


    /********************************************************************
     * 생성자
     * @param context
     * @param opt
     ********************************************************************/
    public VrixAdsdk(Context context, JSONObject opt){
        super(context);
        mContext = context;
        adOptions = opt;
        googleAdIdTask = new GoogleAdIdTask();

        initVrixWebview();
    }



    /********************************************************
     * 외부에 제공되는 메서드
     ********************************************************/
    /**
     * 외부 메서드 : 광고 재생 요청
     */
    public void play(){
        Log.d("VRIX",">> VrixAdsdk.play() ------------");
        if(vrixWebview != null) {
            // 실행할때 마다 GoogleAdIdTask 실행해서  AAID, AAID_STATUS 갱신후 실행
            googleAdIdTask.execute();
        }else{
            Toast.makeText(mContext,"did'nt init google ads service",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 외부 메서드 : interface 를 통해 이벤트를 수신하기 위한 Listener 등록 메서드
     * @param listener
     */
    public void addEventListener(VrixADPLayoutListener listener){
        mVrixADPLayoutListener = listener;
    }




    /********************************************************
     * 내부 메서드
     ********************************************************/
    /**
     * VrixWebview 초기화
     */
    private void initVrixWebview(){
        Log.d("VRIX",">> initVrixWebview() ------------");

        mBridge = new VrixBridge();
        LayoutParams vparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        vrixWebview = new WebView(this.getContext());
        vrixWebview.getSettings().setJavaScriptEnabled(true); // 자바스크립트 사용을 허용한다.
        vrixWebview.setWebViewClient(new WebViewClientClass());  // 새로운 창을 띄우지 않고 내부에서 웹뷰를 실행시킨다.
        vrixWebview.addJavascriptInterface(mBridge, "VrixBridge");
        vrixWebview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        vrixWebview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        String userAgent = vrixWebview.getSettings().getUserAgentString();
        String sdkVersion = getVersion();

        // Agent 값 추가
        vrixWebview.getSettings().setUserAgentString(userAgent+" VRIX_SDK_Android/"+sdkVersion);
        this.addView(vrixWebview);
    }

    /**
     * GoogleAdIdTask 가 완료된후 vrixWebview 에 실질적으로 URI 로드 요청하는 부분
     */
    private void loadWebView(){
        Log.d("VRIX",">> loadWebView() ------------");
        String reqURI = loadURI + "?AAID=" + gadidInfo.AAID + "&AAID_STATUS=" + gadidInfo.AAID_STATUS;
        vrixWebview.loadUrl(reqURI);
    }




    /********************************************************
     * CLASS 선언
     ********************************************************/

    /**
     * vrixWebview의 javascript 와 통신하기위한 Bridge (JavascriptInterface)
     */
    protected final class VrixBridge {

        public VrixBridge() {
        }

        /**
         * 참고(변경됨): web 에서 js 스크립트 로드 완료 된후 onReady 를 호출
         * @param
         */
        @JavascriptInterface
        public void onReady() {
            Log.d("VRIX","[JavascriptInterface] onReady() ------------");
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
            Log.d("VRIX","[JavascriptInterface] onStateChange() ------------ arg="+arg);
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
            Log.d("VRIX","[JavascriptInterface] onError() ------------ msg="+msg);
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


    /**
     * Google play services ads : adid, status 획득
     */
    private class GoogleAdIdTask extends AsyncTask<Void, Void, String> {
        protected String doInBackground(final Void... params) {
            String adId = null;
            Boolean aaid_status = false;
            try {
                adId = AdvertisingIdClient.getAdvertisingIdInfo(mContext).getId();
                aaid_status = AdvertisingIdClient.getAdvertisingIdInfo(mContext).isLimitAdTrackingEnabled();
                Log.d("VRIX","adid = " + adId);
            } catch (IllegalStateException ex) {
                ex.printStackTrace();
                Log.d("VRIX","IllegalStateException");
            } catch (GooglePlayServicesRepairableException ex) {
                ex.printStackTrace();
                Log.d("VRIX","GooglePlayServicesRepairableException");
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.d("VRIX","IOException");
            } catch (GooglePlayServicesNotAvailableException ex) {
                ex.printStackTrace();
                Log.d("VRIX","GooglePlayServicesNotAvailableException");
            }

            gadidInfo = new GadidInfo();
            gadidInfo.setAAID(adId);
            gadidInfo.setAAID_STATUS(aaid_status ? "N":"Y");

            return adId;
        }

        protected void onPostExecute(String adId) {
            //Ad ID를 리턴받은 이후에 이용한 작업 수행
            Log.d("VRIX","gadidInfo.AAID  = " + gadidInfo.AAID );
            Log.d("VRIX","gadidInfo.AAID_STATUS  = " + gadidInfo.AAID_STATUS );

            loadWebView();
        }
    }

    /**
     * Google ADID Info Class
     */
    private class GadidInfo{
        private String AAID;
        private String AAID_STATUS;

        public GadidInfo(){
            super();
        }

        public String getAAID() {
            return AAID;
        }

        public void setAAID(String AAID) {
            this.AAID = AAID;
        }

        public String getAAID_STATUS() {
            return AAID_STATUS;
        }

        public void setAAID_STATUS(String AAID_STATUS) {
            this.AAID_STATUS = AAID_STATUS;
        }
    }

}
