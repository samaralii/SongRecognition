package edu.cmu.pocketsphinx.demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class TestActivity extends Activity implements RecognitionListener {

    private SpeechRecognizer recognizer;
    private MediaPlayer mediaPlayer;

    private Button btnStart, btnStop;
    private ProgressBar progressBar;
    private TextView status;
    private EditText etWord;
    private RadioButton rd25, rs50;
    private RadioGroup rg;
    private String resultText = "";
    private String threshold = "45";
    float value = 1e-45f;


    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    AudioManager audioManager;

    private TextView tvResult;

    int testFlag = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        btnStop = findViewById(R.id.test_btnStop);
        progressBar = findViewById(R.id.test_progress);
        status = findViewById(R.id.test_tvStatus);
        etWord = findViewById(R.id.test_etWord);
        rg = findViewById(R.id.test_rg);

        rd25 = findViewById(R.id.test_vol25);
        rs50 = findViewById(R.id.test_vol50);

        tvResult = findViewById(R.id.test_tvResults);


        tvResult.setMovementMethod(new ScrollingMovementMethod());

        setStatus(1);

        audioManager  = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
        final int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.d("VOICE_TAG", max + "");

        int vol = max / 2;
        Log.d("VOICE_TAG", vol + "");
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol,  AudioManager.FLAG_SHOW_UI);


        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }



        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStop();
            }
        });


        findViewById(R.id.test_btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStart();
                tvResult.setText("");
            }
        });


        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {


                if (checkedId == rd25.getId()) {

                    int vol = Math.abs(max / 3);
                    Log.d("VOICE_TAG", vol + "");
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);

                } else if (checkedId == rs50.getId()) {

                    int vol = max / 2;
                    Log.d("VOICE_TAG", vol + "");
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol,  AudioManager.FLAG_SHOW_UI);

                }

            }
        });



        ((RadioGroup) findViewById(R.id.test_rgVoice)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {


                if (checkedId == R.id.test_rbSong1) {
                    testFlag = 1;
                } else if (checkedId == R.id.test_rbSong2) {
                    testFlag = 2;
                } else if (checkedId == R.id.test_rbVoice) {
                    testFlag = 0;
                }

            }
        });


        (findViewById(R.id.test_btnClear)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultText = "";
                setResultText("");
            }
        });


        ((RadioButton) findViewById(R.id.test_rbVoice)).setChecked(true);
        ((RadioButton) findViewById(R.id.test_vol50)).setChecked(true);



        tvResult.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


                if (count > 100) {
                    stopProcess();
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void setResultText(String s) {
        tvResult.setText(s);
    }


    private void btnStart() {

        stopProcess();

        if (checkThresholdVal()) {
            progressBar.setVisibility(View.VISIBLE);
            new SetupTask(this).execute();
        }

    }


    private void btnStop() {
        setStatus(1);
        stopProcess();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new TestActivity.SetupTask(this).execute();

            } else {
                finish();
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }


        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<TestActivity> activityReference;
        SetupTask(TestActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result) {

            activityReference.get().findViewById(R.id.test_progress)
                    .setVisibility(View.INVISIBLE);

            if (result != null) {
                ((TextView) activityReference.get().findViewById(R.id.test_tvStatus))
                        .setText("Failed to init recognizer " + result);
            } else {
                activityReference.get().switchSearch();
            }
        }
    }

    private void switchSearch() {


        if (testFlag == 1) {

            mediaPlayer = MediaPlayer.create(TestActivity.this, R.raw.hello);
            mediaPlayer.seekTo(13000);
            mediaPlayer.start();

        } else if (testFlag == 2) {

            mediaPlayer = MediaPlayer.create(TestActivity.this, R.raw.song2);
            mediaPlayer.seekTo(25000);
            mediaPlayer.start();

        } else {

            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer = null;
            }

        }


        setStatus(2);


        recognizer.startListening(getWord());

    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("VOICE_TAG", "onBeginningOfSpeech");

    }

    @Override
    public void onEndOfSpeech() {
        Log.d("VOICE_TAG", "onEndOfSpeech");

    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {

        if (hypothesis == null)
            return;


        String text = hypothesis.getHypstr();

        Log.d("VOICE_TAG", "onPartialResult " + text);


        resultText += resultText + text + " ";
        setResultText(resultText);


        if (text.equals(getWord())) {
            setStatus(3);
            stopProcess();
        }


    }


    private void stopProcess() {


        if (recognizer != null) {
            recognizer.stop();
            recognizer.shutdown();
            recognizer = null;
        }


        if (mediaPlayer!= null) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }


        resultText = "";
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        Log.d("VOICE_TAG", "onResult");

    }

    @Override
    public void onError(Exception e) {
        status.setText(e.getMessage());
    }

    @Override
    public void onTimeout() {
        Log.d("VOICE_TAG", "onTimeout");
    }


    private Boolean checkThresholdVal() {

        EditText et = findViewById(R.id.test_etThreshold);


        if (!et.getText().toString().isEmpty()) {

            int checkVal = Integer.parseInt(et.getText().toString());

            if (checkVal < 1 || checkVal > 50) {
                et.setError("Out of range");
                return false;
            } else {
                et.setError(null);
            }

            threshold = et.getText().toString();
        }

        value = Float.parseFloat("1e-" + threshold + "f");
        Log.d("VOICE_TAG", "Threshold" + value + " ");

        return true;

    }



    private void setupRecognizer(File assetsDir) throws IOException {


        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
//                .setKeywordThreshold(1e-45f)
                .setKeywordThreshold(value)
                .setBoolean("-allphone_ci", true)
                .getRecognizer();


        recognizer.addListener(this);

        recognizer.addKeyphraseSearch(getWord(), getWord());

    }

    private String getWord() {

        String word = "mmm";

        if (!etWord.getText().toString().isEmpty()) {
            word = etWord.getText().toString();
        }

        return word.toLowerCase();
    }

    private void setStatus(int status) {

        //1 idle
        //2 listening
        //3 word matched

        switch (status) {

            case 1:
                findViewById(R.id.test_tvPressStart).setVisibility(View.VISIBLE);
                findViewById(R.id.test_tvListening).setVisibility(View.INVISIBLE);
                findViewById(R.id.test_tvMatched).setVisibility(View.INVISIBLE);
                break;

            case 2:
                findViewById(R.id.test_tvPressStart).setVisibility(View.INVISIBLE);
                findViewById(R.id.test_tvListening).setVisibility(View.VISIBLE);
                findViewById(R.id.test_tvMatched).setVisibility(View.INVISIBLE);
                break;

            case 3:
                findViewById(R.id.test_tvPressStart).setVisibility(View.INVISIBLE);
                findViewById(R.id.test_tvListening).setVisibility(View.INVISIBLE);
                findViewById(R.id.test_tvMatched).setVisibility(View.VISIBLE);
                break;


        }

    }

}


