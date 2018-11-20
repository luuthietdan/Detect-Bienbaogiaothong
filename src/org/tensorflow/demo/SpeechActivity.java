/*
 * Copyright 2017 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Demonstrates how to run an audio recognition model in Android.

This example loads a simple speech recognition model trained by the tutorial at
https://www.tensorflow.org/tutorials/audio_training

The model files should be downloaded automatically from the TensorFlow website,
but if you have a custom model you can update the LABEL_FILENAME and
MODEL_FILENAME constants to point to your own files.

The example application displays a list view with all of the known audio labels,
and highlights each one when it thinks it has detected one through the
microphone. The averaging of results to give a more reliable signal happens in
the RecognizeCommands helper class.
*/

package org.tensorflow.demo;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.demo.R;

/**
 * An activity that listens for audio and then uses a TensorFlow model to detect particular classes,
 * by default a small set of action words.
 */
public class SpeechActivity extends Activity {

  // Constants that control the behavior of the recognition code and model
  // settings. See the audio recognition tutorial for a detailed explanation of
  // all these, but you should customize them to match your training settings if
  // you are running your own model.
  private static final int SAMPLE_RATE = 16000;
  private static final int SAMPLE_DURATION_MS = 1000;
  private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
  private static final long AVERAGE_WINDOW_DURATION_MS = 500;
  private static final float DETECTION_THRESHOLD = 0.70f;
  private static final int SUPPRESSION_MS = 1500;
  private static final int MINIMUM_COUNT = 3;
  private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 30;
  private static final String LABEL_FILENAME = "file:///android_asset/conv_actions_labels.txt";
  private static final String MODEL_FILENAME = "file:///android_asset/conv_actions_frozen.pb";
  private static final String INPUT_DATA_NAME = "decoded_sample_data:0";
  private static final String SAMPLE_RATE_NAME = "decoded_sample_data:1";
  private static final String OUTPUT_SCORES_NAME = "labels_softmax";

  // UI elements.
  private static final int REQUEST_RECORD_AUDIO = 13;

  private ListView labelsListView;
  private static final String LOG_TAG = SpeechActivity.class.getSimpleName();

  // Working variables.
  short[] recordingBuffer = new short[RECORDING_LENGTH];
  int recordingOffset = 0;
  boolean shouldContinue = true;
  private Thread recordingThread;
  boolean shouldContinueRecognition = true;
  private Thread recognitionThread;
  private final ReentrantLock recordingBufferLock = new ReentrantLock();
  private TensorFlowInferenceInterface inferenceInterface;
  private List<String> labels = new ArrayList<String>();
  private List<String> displayedLabels = new ArrayList<>();
  private RecognizeCommands recognizeCommands = null;
  private static TextView mTrafficName;
  private static ImageView imgHistory,imgSearch;
  private DatabaseHelper databaseHelper;
  private ImageView imageView;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    deleteCache(getApplicationContext());
    // Set up the UI.
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_speech);

    mTrafficName= (TextView) findViewById(R.id.txtSignName);
    imageView= (ImageView) findViewById(R.id.imgTraffic);
    imgHistory= (ImageView) findViewById(R.id.imgHistory);
     imgSearch= (ImageView) findViewById(R.id.imgSearch);

//    imgData= (ImageView) findViewById(R.id.imgData);
//    imgSetting= (ImageView) findViewById(R.id.imgSetting);
    imgHistory.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent=new Intent(SpeechActivity.this,History.class);
        startActivity(intent);
        finish();
      }
    });
    imgSearch.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        Intent intent=new Intent(SpeechActivity.this,DetectorActivity.class);
        startActivity(intent);
        finish();
      }
    });
    Intent intent=getIntent();
    String trafficName=intent.getStringExtra("TrafficSign");
    String imgNameDemo=trafficName.replaceAll("[^u0000-u007F]+","");
    String imgName = imgNameDemo.toLowerCase();
    Log.d("ddd",imgName);
    int imageID=getRawResIdByName(imgName);
    imageView.setImageResource(imageID);
    mTrafficName.setText(trafficName);
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss");
    String currentDateandTime = sdf.format(new Date());
     databaseHelper= new DatabaseHelper(this, "database.sqlite",null, 1);

    databaseHelper.QueryData("CREATE TABLE IF NOT EXISTS History(Id INTEGER PRIMARY KEY AUTOINCREMENT, Name VARCHAR(100), Image INTEGER, Time VARCHAR(100))");

     databaseHelper.QueryData("INSERT INTO History VALUES(null, '" + trafficName +"', '" +imageID  + "', '" + currentDateandTime +"')");
//      databaseHelper.QueryData("DROP TABLE History");
    Cursor dataHistory = databaseHelper.GetData("SELECT * FROM History");
    while(dataHistory.moveToNext()){
      String ten = dataHistory.getString(1);
      int image = dataHistory.getInt(2);
      String time = dataHistory.getString(3);
      Log.d("BBB", ten + "\n" + image + "\n" + time);
    }

//    quitButton = (Button) findViewById(R.id.quit);
//    quitButton.setOnClickListener(
//        new View.OnClickListener() {
//          @Override
//          public void onClick(View view) {
//            moveTaskToBack(true);
//            android.os.Process.killProcess(android.os.Process.myPid());
//            System.exit(1);
//          }
//        });


    // Load the labels for the model, but only display those that don't start
    // with an underscore.
    String actualFilename = LABEL_FILENAME.split("file:///android_asset/")[1];
    Log.i(LOG_TAG, "Reading labels from: " + actualFilename);
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(getAssets().open(actualFilename)));
      String line;
      while ((line = br.readLine()) != null) {
        labels.add(line);
        if (line.charAt(0) != '_') {
          displayedLabels.add(line.substring(0, 1).toUpperCase() + line.substring(1));
        }
      }
      br.close();
    } catch (IOException e) {
      throw new RuntimeException("Problem reading label file!", e);
    }

    // Build a list view based on these labels.


    // Set up an object to smooth recognition results to increase accuracy.
    recognizeCommands =
        new RecognizeCommands(
            labels,
            AVERAGE_WINDOW_DURATION_MS,
            DETECTION_THRESHOLD,
            SUPPRESSION_MS,
            MINIMUM_COUNT,
            MINIMUM_TIME_BETWEEN_SAMPLES_MS);

    // Load the TensorFlow model.
    inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILENAME);

    // Start the recording and recognition threads.
    requestMicrophonePermission();
    startRecording();
    startRecognition();
  }

  private int getRawResIdByName(String resName) {
    String pkgName=getApplicationContext().getPackageName();
    return this.getResources().getIdentifier(resName,"drawable",pkgName);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu,menu);
    return true;
  }

  private void requestMicrophonePermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestPermissions(
          new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == REQUEST_RECORD_AUDIO
        && grantResults.length > 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      startRecording();
      startRecognition();
    }
  }

  public synchronized void startRecording() {
    if (recordingThread != null) {
      return;
    }
    shouldContinue = true;
    recordingThread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                record();
              }
            });
    recordingThread.start();
  }

  public synchronized void stopRecording() {
    if (recordingThread == null) {
      return;
    }
    shouldContinue = false;
    recordingThread = null;
  }

  private void record() {
    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

    // Estimate the buffer size we'll need for this device.
    int bufferSize =
        AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
      bufferSize = SAMPLE_RATE * 2;
    }
    short[] audioBuffer = new short[bufferSize / 2];

    AudioRecord record =
        new AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize);

    if (record.getState() != AudioRecord.STATE_INITIALIZED) {
      Log.e(LOG_TAG, "Audio Record can't initialize!");
      return;
    }

    record.startRecording();

    Log.v(LOG_TAG, "Start recording");

    // Loop, gathering audio data and copying it to a round-robin buffer.
    while (shouldContinue) {
      int numberRead = record.read(audioBuffer, 0, audioBuffer.length);
      int maxLength = recordingBuffer.length;
      int newRecordingOffset = recordingOffset + numberRead;
      int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
      int firstCopyLength = numberRead - secondCopyLength;
      // We store off all the data for the recognition thread to access. The ML
      // thread will copy out of this buffer into its own, while holding the
      // lock, so this should be thread safe.
      recordingBufferLock.lock();
      try {
        System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
        System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
        recordingOffset = newRecordingOffset % maxLength;
      } finally {
        recordingBufferLock.unlock();
      }
    }

    record.stop();
    record.release();
  }

  public synchronized void startRecognition() {
    if (recognitionThread != null) {
      return;
    }
    shouldContinueRecognition = true;
    recognitionThread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                recognize();
              }
            });
    recognitionThread.start();
  }

  public synchronized void stopRecognition() {
    if (recognitionThread == null) {
      return;
    }
    shouldContinueRecognition = false;
    recognitionThread = null;
  }

  private void recognize() {
    Log.v(LOG_TAG, "Start recognition");

    short[] inputBuffer = new short[RECORDING_LENGTH];
    float[] floatInputBuffer = new float[RECORDING_LENGTH];
    float[] outputScores = new float[labels.size()];
    String[] outputScoresNames = new String[] {OUTPUT_SCORES_NAME};
    int[] sampleRateList = new int[] {SAMPLE_RATE};

    // Loop, grabbing recorded data and running the recognition model on it.
    while (shouldContinueRecognition) {
      // The recording thread places data in this round-robin buffer, so lock to
      // make sure there's no writing happening and then copy it to our own
      // local version.
      recordingBufferLock.lock();
      try {
        int maxLength = recordingBuffer.length;
        int firstCopyLength = maxLength - recordingOffset;
        int secondCopyLength = recordingOffset;
        System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
        System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
      } finally {
        recordingBufferLock.unlock();
      }

      // We need to feed in float values between -1.0f and 1.0f, so divide the
      // signed 16-bit inputs.
      for (int i = 0; i < RECORDING_LENGTH; ++i) {
        floatInputBuffer[i] = inputBuffer[i] / 32767.0f;
      }

      // Run the model.
      inferenceInterface.feed(SAMPLE_RATE_NAME, sampleRateList);
      inferenceInterface.feed(INPUT_DATA_NAME, floatInputBuffer, RECORDING_LENGTH, 1);
      inferenceInterface.run(outputScoresNames);
      inferenceInterface.fetch(OUTPUT_SCORES_NAME, outputScores);

      // Use the smoother to figure out if we've had a real recognition event.
      long currentTime = System.currentTimeMillis();
      final RecognizeCommands.RecognitionResult result =
          recognizeCommands.processLatestResults(outputScores, currentTime);

      runOnUiThread(
          new Runnable() {
            @Override
            public void run() {
              // If we do have a new command, highlight the right list entry.
              if (!result.foundCommand.startsWith("_") && result.isNewCommand) {
                int labelIndex = -1;
                for (int i = 0; i < labels.size(); ++i) {
                  if (labels.get(i).equals(result.foundCommand)) {
                    labelIndex = i;
                  }
                }
//                final View labelView = labelsListView.getChildAt(labelIndex - 2);
//
//                AnimatorSet colorAnimation =
//                    (AnimatorSet)
//                        AnimatorInflater.loadAnimator(
//                            SpeechActivity.this, R.animator.color_animation);
//                colorAnimation.setTarget(labelView);
//                colorAnimation.start();
              }
            }
          });
      try {
        // We don't need to run too frequently, so snooze for a bit.
        Thread.sleep(MINIMUM_TIME_BETWEEN_SAMPLES_MS);
      } catch (InterruptedException e) {
        // Ignore
      }
    }

    Log.v(LOG_TAG, "End recognition");
  }
  public static void deleteCache(Context context){
    try{
      File dir=context.getCacheDir();
      deleteDir(dir);
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  private static boolean deleteDir(File dir) {
    if(dir!=null && dir.isDirectory()){
      String[] children=dir.list();
      for(int i=0;i<children.length;i++){
        boolean success=deleteDir(new File(dir,children[i]));
        if(!success){
          return false;
        }
      }
      return dir.delete();
    }else if(dir!=null&& dir.isFile()){
      return dir.delete();
    }else {
      return false;
    }
  }
}
