package sa.kono.app.ui.chat

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/** Records an AAC/m4a voice note into the cache dir. */
class VoiceRecorder(private val context: Context) {
    var isRecording by mutableStateOf(false); private set
    var elapsed by mutableFloatStateOf(0f); private set
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L
    private var ticker: Job? = null

    fun start(scope: CoroutineScope): Boolean {
        if (isRecording) return true
        val f = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44100); r.setAudioEncodingBitRate(64000)
            r.setOutputFile(f.absolutePath)
            r.prepare(); r.start()
            recorder = r; file = f; startedAt = System.currentTimeMillis(); isRecording = true; elapsed = 0f
            ticker = scope.launch { while (isActive) { elapsed = (System.currentTimeMillis() - startedAt) / 1000f; delay(100) } }
            true
        } catch (e: Exception) { r.release(); false }
    }

    /** Stops and returns the recording with its duration in seconds, or null when too short / failed. */
    fun stop(): Pair<File, Int>? {
        val r = recorder ?: return null
        ticker?.cancel(); isRecording = false
        val secs = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
        val ok = runCatching { r.stop() }.isSuccess
        r.release(); recorder = null
        val f = file; file = null
        return if (ok && f != null && secs >= 1) f to secs else { f?.delete(); null }
    }

    fun cancel() { stop()?.first?.delete() }
}

/** One shared player so only one voice note plays at a time. */
object VoicePlayer {
    var playingUrl by mutableStateOf<String?>(null); private set
    var progress by mutableFloatStateOf(0f); private set
    private var player: MediaPlayer? = null
    private var ticker: Job? = null

    fun toggle(url: String, scope: CoroutineScope) {
        if (playingUrl == url) { stop(); return }
        stop()
        val p = MediaPlayer()
        try {
            p.setDataSource(url)
            p.setOnPreparedListener { it.start(); playingUrl = url }
            p.setOnCompletionListener { stop() }
            p.setOnErrorListener { _, _, _ -> stop(); true }
            p.prepareAsync()
            player = p
            ticker = scope.launch(Dispatchers.Main) {
                while (isActive) {
                    val pl = player
                    if (pl != null && playingUrl == url && pl.duration > 0) progress = pl.currentPosition / pl.duration.toFloat()
                    delay(150)
                }
            }
        } catch (e: Exception) { p.release() }
    }

    fun stop() {
        ticker?.cancel()
        runCatching { player?.stop() }; player?.release(); player = null
        playingUrl = null; progress = 0f
    }
}
