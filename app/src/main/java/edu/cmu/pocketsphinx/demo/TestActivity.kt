package edu.cmu.pocketsphinx.demo

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import edu.cmu.pocketsphinx.Assets
import edu.cmu.pocketsphinx.Hypothesis
import edu.cmu.pocketsphinx.RecognitionListener
import edu.cmu.pocketsphinx.SpeechRecognizer
import edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup
import edu.cmu.pocketsphinx.demo.utils.hide
import edu.cmu.pocketsphinx.demo.utils.show
import kotlinx.android.synthetic.main.activity_test.*
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference

class TestActivity : AppCompatActivity(), RecognitionListener {

    companion object {
        private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }

    private var recognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null

    private var resultText = ""
    private var threshold = "45"
    internal var value = 1e-45f

    private val audioManager by lazy { applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager }

    private var testFlag = 0

    private val word: String
        get() {

            var word = "mmm"

            if (!test_etWord.text.toString().isEmpty()) {
                word = test_etWord!!.text.toString()
            }

            return word.toLowerCase()
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)


        test_tvResults.movementMethod = ScrollingMovementMethod()

        setStatus(1)

        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        Log.d("VOICE_TAG", max.toString() + "")

        val vol = max / 2
        Log.d("VOICE_TAG", vol.toString() + "")
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI)


        val permissionCheck = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSIONS_REQUEST_RECORD_AUDIO)
            return
        }



        test_btnStop.setOnClickListener { btnStop() }


        test_btnStart.setOnClickListener {
            btnStart()
            test_tvResults.text = ""
        }


        test_rg.setOnCheckedChangeListener { _, checkedId ->

            if (checkedId == test_vol25.id) {

                val volume = Math.abs(max / 3)
                Log.d("VOICE_TAG", vol.toString() + "")
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)

            } else if (checkedId == test_vol25.id) {

                val volume = max / 2
                Log.d("VOICE_TAG", vol.toString() + "")
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)

            }
        }



        test_rgVoice.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                test_rbSong1.id -> testFlag = 1
                test_rbSong2.id -> testFlag = 2
                test_rbVoice.id -> testFlag = 0
            }
        }


        findViewById<View>(R.id.test_btnClear).setOnClickListener {
            resultText = ""
            setResultText("")
        }


        test_rbVoice.isChecked = true
        test_vol50.isChecked = true



        test_tvResults.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {


                if (count > 100) {
                    stopProcess()
                }

            }

            override fun afterTextChanged(s: Editable) {

            }
        })

    }

    private fun setResultText(s: String) {
        test_tvResults.text = s
    }


    private fun btnStart() {

        stopProcess()

        if (checkThresholdVal()) {
            test_progress.show()
            SetupTask(this).execute()
        }

    }


    private fun btnStop() {
        setStatus(1)
        stopProcess()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                SetupTask(this).execute()

            } else {
                finish()
            }
        }
    }


    public override fun onDestroy() {
        super.onDestroy()

        recognizer?.let {
            it.cancel()
            it.shutdown()
        }

        mediaPlayer?.let {
            it.release()
            mediaPlayer = null
        }
    }


    private fun switchSearch() {

        if (testFlag == 1) {

            mediaPlayer = MediaPlayer.create(this@TestActivity, R.raw.hello)
            mediaPlayer!!.seekTo(13000)
            mediaPlayer!!.start()

        } else if (testFlag == 2) {

            mediaPlayer = MediaPlayer.create(this@TestActivity, R.raw.song2)
            mediaPlayer!!.seekTo(25000)
            mediaPlayer!!.start()

        } else {

            if (mediaPlayer != null) {
                mediaPlayer!!.stop()
                mediaPlayer = null
            }

        }


        setStatus(2)


        recognizer?.startListening(word)

    }

    override fun onBeginningOfSpeech() {
        Log.d("VOICE_TAG", "onBeginningOfSpeech")

    }

    override fun onEndOfSpeech() {
        Log.d("VOICE_TAG", "onEndOfSpeech")

    }

    override fun onPartialResult(hypothesis: Hypothesis?) {

        if (hypothesis == null)
            return


        val text = hypothesis.hypstr

        Log.d("VOICE_TAG", "onPartialResult $text")


        resultText += "$resultText$text"
        test_tvResults.text = resultText


        if (text == word) {
            setStatus(3)
            stopProcess()
        }


    }


    private fun stopProcess() {

        recognizer?.let {
            it.stop()
            it.shutdown()
            recognizer = null
        }

        mediaPlayer?.let {
            it.stop()
            mediaPlayer = null
        }

        resultText = ""
    }

    override fun onResult(hypothesis: Hypothesis) {
        Log.d("VOICE_TAG", "onResult")

    }

    override fun onError(e: Exception) {
        test_tvStatus.text = e.message
    }

    override fun onTimeout() {
        Log.d("VOICE_TAG", "onTimeout")
    }


    private fun checkThresholdVal(): Boolean {

        if (!test_etThreshold.text.toString().isEmpty()) {

            val checkVal = Integer.parseInt(test_etThreshold.text.toString())

            if (checkVal < 1 || checkVal > 50) {
                test_etThreshold.error = "Out of range"
                return false
            } else {
                test_etThreshold.error = null
            }

            threshold = test_etThreshold.text.toString()
        }

        value = "1e-${threshold}f".toFloat()
        Log.d("VOICE_TAG", "Threshold $value ")

        return true

    }


    @Throws(IOException::class)
    private fun setupRecognizer(assetsDir: File) {

        recognizer = defaultSetup()
                .setAcousticModel(File(assetsDir, "en-us-ptm"))
                .setDictionary(File(assetsDir, "cmudict-en-us.dict"))
                .setKeywordThreshold(value)
                .setBoolean("-allphone_ci", true)
                .recognizer

        recognizer?.addListener(this)

        recognizer?.addKeyphraseSearch(word, word)

    }

    private fun setStatus(status: Int) {

        //1 idle
        //2 listening
        //3 word matched

        when (status) {
            1 -> {
                test_tvPressStart.show()
                test_tvListening.hide()
                test_tvMatched.hide()
            }

            2 -> {
                test_tvPressStart.hide()
                test_tvListening.show()
                test_tvMatched.hide()
            }

            3 -> {
                test_tvPressStart.hide()
                test_tvListening.hide()
                test_tvMatched.show()
            }
        }

    }




    inner class SetupTask internal constructor(activity: TestActivity) : AsyncTask<Void, Void, Exception>() {

        private var activityReference: WeakReference<TestActivity> = WeakReference(activity)

        override fun doInBackground(vararg params: Void): Exception? {
            try {
                val assets = Assets(activityReference.get())
                val assetDir = assets.syncAssets()
                activityReference.get()!!.setupRecognizer(assetDir)
            } catch (e: IOException) {
                return e
            }

            return null
        }

        override fun onPostExecute(result: Exception?) {

            activityReference.get()!!.findViewById<View>(R.id.test_progress).visibility = View.INVISIBLE

            if (result != null) {
                (activityReference.get()!!.findViewById<View>(R.id.test_tvStatus) as TextView).text = "Failed to init recognizer $result"
            } else {
                activityReference.get()!!.switchSearch()
            }
        }
    }

}

