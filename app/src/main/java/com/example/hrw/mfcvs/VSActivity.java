package com.example.hrw.mfcvs;

import android.app.Activity;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import net.majorkernelpanic.streaming.rtsp.RtspClient;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.video.VideoQuality;

import java.io.IOException;

/*
 * Copyright (C) 2011-2014 GUIGUI Simon, fyhertz@gmail.com
 *
 * This project contain libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * This is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


public class VSActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
    public static class PlaceholderFragment extends Fragment implements SurfaceHolder.Callback, View.OnClickListener, Session.Callback, RtspClient.Callback, Camera.PreviewCallback {
        private static final String TAG = "VSActivity";
        private Camera mCamera;
        private SurfaceView previewSurfaceView;
        private VideoRecord videoRecord;
        private Button record,stream;
        private Context context;
        private Session session;
        private int frameSize;
        private String destIP = "192.168.1.277:1234";
        private boolean isNotRec = true;
        private RtspClient rtspClient;
        private VideoCodec videoCodec;
        private SurfaceHolder previewSurfaceHolder;

        @Override
        public void onStop() {
            super.onStop();
            videoCodec.close();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
//          Runtime.getRuntime().exec("su");
            useCamTest();
            context = getActivity().getApplicationContext();
            record = (Button)getView().findViewById(R.id.record);
            record.setOnClickListener(this);
            stream = (Button) getView().findViewById(R.id.stream);
            stream.setOnClickListener(this);
            session = SessionBuilder.getInstance()
                    .setCallback(this)
                    .setSurfaceView(previewSurfaceView)
                    .setPreviewOrientation(90)
                    .setContext(context)
                    .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                    .setAudioQuality(new AudioQuality(16000, 32000))
                    .setVideoEncoder(SessionBuilder.VIDEO_H264)
                    .setVideoQuality(new VideoQuality(720,480,30,5000000))
                    .setDestination(destIP)
                    .build();
            previewSurfaceView.getHolder().addCallback(this);
            rtspClient = new RtspClient();
            rtspClient.setSession(session);
            rtspClient.setCallback(this);
        }

        private void getCamSize(){
            Camera.Parameters camp = mCamera.getParameters();
            int pixel = ImageFormat.getBitsPerPixel(camp.getPictureFormat())/8;
            Camera.Size cams = camp.getPreviewSize();
            frameSize = cams.width * cams.height * pixel;
            Log.w(TAG,String.valueOf(frameSize));
        }

        private void useCamTest() {
            videoCodec = new VideoCodec();
            mCamera = getCameraInstance();
            getCamSize();
            previewSurfaceView = (SurfaceView) getView().findViewById(R.id.surfaceview);
            previewSurfaceHolder = previewSurfaceView.getHolder();
            previewSurfaceHolder.addCallback(this);
//            videoRecord = new VideoRecord("test", mCamera);
        }

        private Camera getCameraInstance() {
            Camera c = null;
            try {
                c = openFrontFacingCamera();
            }
            catch (Exception e){
                Log.w("get front camera",e.toString());
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
//            session.startPreview();
            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.getParameters().setPreviewSize(320,240);
                mCamera.setParameters(mCamera.getParameters());
                mCamera.addCallbackBuffer(new byte[151552]);
                mCamera.setPreviewCallbackWithBuffer(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
//            session.stopPreview();
//            rtspClient.stopStream();
//            session.stop();
            mCamera.stopPreview();
        }


        @Override
        public void onBitrateUpdate(long bitrate) {
            Log.w(TAG,"Bitrate: "+bitrate);
        }

        @Override
        public void onSessionError(int reason, int streamType, Exception e) {
            Log.e(TAG, "An error occured:"+String.valueOf(reason) + e.toString());
        }

        @Override
        public void onPreviewStarted() {
            Log.w(TAG,"PreviewStarted");
        }

        @Override
        public void onSessionConfigured() {
            Log.w(TAG,"Session Configured");
            Log.w(TAG,session.getSessionDescription());
//            session.start();
        }

        @Override
        public void onSessionStarted() {
            Log.w(TAG,"Stream Session Started");
        }

        @Override
        public void onSessionStopped() {
            Log.w(TAG,"Stream Session Stopped");
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
                    toggleStream();
                    break;
            }
        }

        @Override
        public void onRtspUpdate(int message, Exception exception) {
            Log.w("onRtspUpdate",String.valueOf(message)+exception.toString());
        }
        public void toggleStream() {
            if (!rtspClient.isStreaming()) {
//                String ip,port,path;

                // We parse the URI written in the Editext
//                Pattern uri = Pattern.compile("rtsp://(.+):?(\\d*)/(.+)");
//                Matcher m = uri.matcher(destIP); m.find();
//                ip = m.group(1);
//                port = m.group(2);
//                path = m.group(3);

                rtspClient.setServerAddress("192.168.1.227", 1234);
                rtspClient.setStreamPath("/"+"stream");
                rtspClient.startStream();

            } else {
                // Stops the stream and disconnects from the RTSP server
                rtspClient.stopStream();
            }
        }

        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            Log.w(TAG,"Start encode");
            videoCodec.offerEncoder(bytes,0,bytes.length);
        }
    }
}
