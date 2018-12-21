package edu.cmu.pocketsphinx.demo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import edu.cmu.pocketsphinx.*
import java.io.File
import android.media.RingtoneManager




class BackgroundService: Service(), RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private val word = "start"


    private val notification_id = "update_status_1"
    private val notification_title = "PocketSphinx Status"
    private val notification_description = "Listening..."
    private val importance by lazy { NotificationManagerCompat.IMPORTANCE_HIGH }

    private val mNotificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val notification by lazy { NotificationCompat.Builder(this, notification_id) }


    private fun setupNotification() {

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_action_file)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        notification.apply {
            setSmallIcon(R.drawable.ic_action_file)
            setLargeIcon(bitmap)
            setContentTitle(notification_title)
            setContentText(notification_description)
            priority = importance
            setSound(soundUri)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(notification_id, notification_title, importance)
            mNotificationManager.createNotificationChannel(mChannel)
        }

        mNotificationManager.notify(1, notification.build())
    }

    private fun successNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(notification_id, notification_title, importance)
            mNotificationManager.createNotificationChannel(mChannel)
        }

        notification.apply {
            setSmallIcon(R.drawable.ic_action_file)
            setContentText("Word Matched")
        }

        mNotificationManager.notify(1, notification.build())
    }

    private fun errorNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(notification_id, notification_title, importance)
            mNotificationManager.createNotificationChannel(mChannel)
        }

        notification.apply {
            setSmallIcon(R.drawable.ic_action_file)
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
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BG_SERVICE", "onStartCommand")
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
            successNotification()
            stopProcess()
            stopSelf()
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


    private fun showNotification() {


    }


    override fun onDestroy() {
        stopProcess()
        Log.d("BG_SERVICE", "onDestroy")
        super.onDestroy()
    }


}