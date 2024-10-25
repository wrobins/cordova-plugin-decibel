package com.wrobins.cordova.plugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.util.Timer;
import java.util.TimerTask;

public class DecibelPlugin extends CordovaPlugin {

    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecorder = null;
    private CallbackContext callbackContext;
    private Timer decibelPollingTimer = null;
    private short[] audioBuffer;
    private double previousDecibel = 0.0;
    private int pollingFrequency = 50;
    private boolean isListening = false;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("startListening")) {
            if (isListening) {
                callbackContext.error("Decibel recording is already in progress.");
                return true;
            }
            pollingFrequency = args.getInt(0);
            this.startListening();
            return true;
        }
        if (action.equals("stopListening")) {
            this.stopListening();
            return true;
        }
        return false;
    }

    private void startListening() {
        try {
            if(cordova.hasPermission(Manifest.permission.RECORD_AUDIO)) {
                startListeningInternal();
            } else {
                cordova.requestPermissions(this, 0, new String[]{Manifest.permission.RECORD_AUDIO});
            }
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListeningInternal();
            } else {
                callbackContext.error("RECORD_AUDIO permission was denied by the user.");
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startListeningInternal() {
        try {
            isListening = true;
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            audioBuffer = new short[bufferSize];
            audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
            audioRecorder.startRecording();

            decibelPollingTimer = new Timer();
            decibelPollingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        int bytesRead = audioRecorder.read(audioBuffer, 0, bufferSize);
                        if (bytesRead > 0) {
                            double maxAmplitude = calculateDecibels(audioBuffer, bytesRead);
                            PluginResult result = new PluginResult(PluginResult.Status.OK, Math.round(maxAmplitude));
                            result.setKeepCallback(true);
                            callbackContext.sendPluginResult(result);
                        }
                    } catch (Exception e) {
                        callbackContext.error("Error reading audio data: " + e.getMessage());
                    }
                }
            }, 0, pollingFrequency);
        } catch (IllegalArgumentException | IllegalStateException e) {
            callbackContext.error("Error starting audio recording: " + e.getMessage());
        }
    }

    private double calculateDecibels(short[] buffer, int length) {
        double maxAmplitude = 0.0;
        for (short value : buffer) {
            if (Math.abs(value) > maxAmplitude) {
                maxAmplitude = Math.abs(value);
            }
        }

        double db = ((maxAmplitude > 0 ? 20.0 * (Math.log10(maxAmplitude / 32767.0)) + 90 : 0.0) + previousDecibel) / (previousDecibel == 0.0 ? 1 : 2);
        previousDecibel = db;
        return db;
    }

    private void stopListening() {
        try {
            if (decibelPollingTimer != null) {
                decibelPollingTimer.cancel();
                decibelPollingTimer = null;
            }
            if (audioRecorder != null) {
                audioRecorder.stop();
                audioRecorder.release();
                audioRecorder = null;
            }
            isListening = false;
            callbackContext.success();
        } catch (Exception e) {
            callbackContext.error("Error stopping audio recording: " + e.getMessage());
        }
    }
}
