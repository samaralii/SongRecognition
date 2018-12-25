package edu.cmu.pocketsphinx.demo

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.*
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import edu.cmu.pocketsphinx.*
import edu.cmu.pocketsphinx.demo.utils.BROADCAST_ACTION
import java.io.File


class BackgroundService: Service(), RecognitionListener {


    companion object {
        const val PROCESS_STOPPED = "process_stopped"
        const val BG_WORD = "bg_word"
        const val BG_THRESHOLD= "bg_threshold"
    }

    private var recognizer: SpeechRecognizer? = null


    private val CHANNEL_ID = "update_status_1"
    private val NOTIFICATION_ID = 1
    private val notification_title = "PocketSphinx Status"
    private val notification_description = "Listening..."
    private val importance by lazy { NotificationManagerCompat.IMPORTANCE_MAX }

    private val mNotificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val notification by lazy { NotificationCompat.Builder(this, CHANNEL_ID) }

    private var intent: Intent? = null


    private var word = "start"
    private var threshold = 1e-45f





    private fun setupNotification() {

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_action_file)

        val intent = Intent(this, TestActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        notification.apply {
            setSmallIcon(R.drawable.ic_action_file)
            setLargeIcon(bitmap)
            setContentTitle(notification_title)
            setContentText(notification_description)
            setContentIntent(pendingIntent)
            setDefaults(DEFAULT_ALL)
            priority = importance
            setOngoing(true)
            setAutoCancel(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(CHANNEL_ID, notification_title, importance)
            mNotificationManager.createNotificationChannel(mChannel)
        }

//        mNotificationManager.notify(1, notification.build())

        with(NotificationManagerCompat.from(this)){
            notify(NOTIFICATION_ID, notification.build())
        }

    }

    private fun successNotification(str: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(CHANNEL_ID, notification_title, importance)
            mNotificationManager.createNotificationChannel(mChannel)
        }

        notification.apply {
            setSmallIcon(R.drawable.ic_action_file)
            setOngoing(false)
            setContentText("Word Matched : $str")
        }

        mNotificationManager.notify(1, notification.build())
    }

    private fun errorNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(CHANNEL_ID, notification_title, importance)
            mNotificationManager.createNotificationChannel(mChannel)
        }

        notification.apply {
            setSmallIcon(R.drawable.ic_action_file)
            setOngoing(false)
            setContentText("Error, Please try again.")
        }

        mNotificationManager.notify(1, notification.build())
    }


    override fun onBind(intent: Intent?): IBinder? {
        Log.d("BG_SERVICE", "onBind")
        return null
    }


    override fun onCreate() {
        super.onCreate()
        Log.d("BG_SERVICE", "onCreate")
        intent = Intent(BROADCAST_ACTION)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BG_SERVICE", "onStartCommand")

        intent?.let {
            word = it.getStringExtra(BG_WORD)
            threshold = it.getStringExtra(BG_THRESHOLD).toFloat()
            Log.d("BG_SERVICE", "Word : $word || Threshold : $threshold")
        }

        setupNotification()

        try {
            setUpVoiceRecognition()
        } catch (e: Exception) {
            e.printStackTrace()
            errorNotification()
        }


        return START_STICKY
    }


    private fun setUpVoiceRecognition() {

        val assets = Assets(this)
        val assetDir = assets.syncAssets()


        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(File(assetDir, "en-us-ptm"))
                .setDictionary(File(assetDir, "cmudict-en-us.dict"))
                .setBoolean("-allphone_ci", true)
                .setKeywordThreshold(threshold)
                .recognizer

        recognizer?.addListener(this)

        recognizer?.addKeyphraseSearch(word, word)

        recognizer?.startListening(word)


        Log.d("BG_SERVICE", "Listening...")

    }

    override fun onResult(p0: Hypothesis?) {
    }

    override fun onPartialResult(hypothesis: Hypothesis?) {


        if (hypothesis == null)
            return


        val text = hypothesis.hypstr

        Log.d("BG_SERVICE", "onPartialResult $text")


        if (text == word) {
            Log.d("BG_SERVICE", "onPartialResult DETECTED $text")
            successNotification(text)
            onStopVr(text)
            stopProcess()
        }


        //TODO show notification
    }

    override fun onTimeout() {
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onEndOfSpeech() {
    }

    override fun onError(p0: Exception?) {
        p0?.printStackTrace()
    }


    private fun stopProcess() {
        recognizer?.let {
            it.stop()
            it.shutdown()
            recognizer = null
        }
    }

    override fun onDestroy() {
        mNotificationManager.cancelAll()
        stopProcess()
        Log.d("BG_SERVICE", "onDestroy")
        super.onDestroy()
    }


    private fun onStopVr(str: String) {
        intent?.putExtra(PROCESS_STOPPED, 1)
        intent?.putExtra(BG_WORD, str)
        sendBroadcast(intent)
    }


}