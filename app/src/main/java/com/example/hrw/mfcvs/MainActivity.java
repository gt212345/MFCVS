package com.example.hrw.mfcvs;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.MediaCodec;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.video.VideoQuality;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements SurfaceHolder.Callback, View.OnClickListener {
        private Camera mCamera;
        private SurfaceView previewSurfaceView;
        private SurfaceHolder previewSurfaceHolder;
        private VideoRecord videoRecord;
        private Button record,stream;
        private MediaPlayer mediaPlayer;
        private Context context;
        private String USERNAME,PASSWORD;
        private boolean isNotRec = true;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
//          Runtime.getRuntime().exec("su");
            context = getActivity().getApplicationContext();
            mediaPlayer = new MediaPlayer();
            mCamera = getCameraInstance();
            record = (Button)getView().findViewById(R.id.record);
            record.setOnClickListener(this);
            stream = (Button) getView().findViewById(R.id.stream);
            stream.setOnClickListener(this);
            if(mCamera == null){
                Toast.makeText(getActivity(),
                        "Fail to get Camera",
                        Toast.LENGTH_LONG).show();
            }
            previewSurfaceView = (SurfaceView)getView().findViewById(R.id.surfaceview);
            previewSurfaceHolder = previewSurfaceView.getHolder();
            previewSurfaceHolder.addCallback(this);
            videoRecord = new VideoRecord("test",mCamera);
            Map<String, String> headers = getRtspHeaders();
//            Uri source = Uri.parse(RTSP_URL);
        }

        public Camera getCameraInstance() {
            Camera c = null;
            try {
                c = openFrontFacingCamera();
            }
            catch (Exception e){
                Log.w("getfrontcamera",e.toString());
            }
            return c;
        }

        private Camera openFrontFacingCamera() {
            int cameraCount;
            Camera cam = null;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx<cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    try {
                        cam = Camera.open(camIdx);
                        Log.w("Camera","No:"+String.valueOf(camIdx)+" get");
                        Camera.Parameters param = cam.getParameters();
                        param.set( "cam_mode", 1 );
                        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        cam.setParameters( param );
                    } catch (RuntimeException e) {
                        Log.e("Your_TAG", "Camera failed to open: " + e.getLocalizedMessage());
                    }
                }
            }
            return cam;
        }

        private Map<String, String> getRtspHeaders() {
            Map<String, String> headers = new HashMap<String, String>();
            String basicAuthValue = getBasicAuthValue(USERNAME, PASSWORD);
            headers.put("Authorization", basicAuthValue);
            return headers;
        }

        private String getBasicAuthValue(String usr, String pwd) {
            String credentials = usr + ":" + pwd;
            int flags = Base64.URL_SAFE | Base64.NO_WRAP;
            byte[] bytes = credentials.getBytes();
            return "Basic " + Base64.encodeToString(bytes, flags);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mediaPlayer.setDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.setDisplayOrientation(90);
                mCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            mCamera.release();
        }


        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.record:
                    if (isNotRec) {
                        Toast.makeText(getActivity(), "Record Start", Toast.LENGTH_SHORT).show();
                        isNotRec = false;
                        try {
                            videoRecord.startEncoding();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Record Stop", Toast.LENGTH_SHORT).show();
                        isNotRec = true;
                        videoRecord.stopEncoding();
                    }
                    break;
                case R.id.stream:
                    break;
            }
        }
    }
}
