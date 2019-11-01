package tech.naru.deezer_playback

import android.os.Build
import android.content.Intent;
import android.os.Bundle;
import android.util.Log
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import com.deezer.sdk.model.Artist
import com.deezer.sdk.model.Track
import com.deezer.sdk.network.connect.DeezerConnect
import com.deezer.sdk.network.connect.event.DialogError
import com.deezer.sdk.network.connect.event.DialogListener
import com.deezer.sdk.network.request.event.DeezerError
import com.deezer.sdk.player.exception.TooManyPlayersExceptions
import com.deezer.sdk.player.networkcheck.WifiAndMobileNetworkStateChecker
import com.deezer.sdk.model.Permissions
import com.deezer.sdk.network.connect.SessionStore
import com.deezer.sdk.network.request.DeezerRequestFactory
import com.deezer.sdk.network.request.event.JsonRequestListener
import com.deezer.sdk.player.*
import com.deezer.sdk.player.event.*


import tech.naru.deezer.BaseControl
import tech.naru.deezer.PlaybackControls
import tech.naru.deezer.SeekControls
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.PluginRegistry
import java.sql.Connection
import kotlin.concurrent.fixedRateTimer

class DeezerPlaybackPlugin(private var registrar: PluginRegistry.Registrar) : MethodCallHandler {

    // The Deezer global reference
    private var trackPlayer: TrackPlayer? = null
    private var mPlayer: PlayerWrapper? = null
    private var mDeezerConnect: DeezerConnect? = null
    private var permissions = arrayOf(Permissions.BASIC_ACCESS, Permissions.MANAGE_LIBRARY, Permissions.LISTENING_HISTORY)

    private val playbackStateStreamHandler = PlaybackStateStreamHandler()
    private val playbackProgressStreamHandler = PlaybackProgressStreamHandler()
    private val playbackErrorStreamHandler = PlaybackErrorStreamHandler()

    lateinit var connectResultHandler: Result
    // The listener for authentication events
    private val listener = object : DialogListener {

        override fun onComplete(values: Bundle) {
            // store the current authentication info
            val sessionStore = SessionStore()
            sessionStore.save(mDeezerConnect, registrar.context())
            trackPlayer = TrackPlayer(registrar.activity().application, mDeezerConnect, WifiAndMobileNetworkStateChecker())
            connectResultHandler.success(true)
            // Launch the Home activity
        }

        override fun onException(exception: Exception) {}

        override fun onCancel() {}
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val plugin = DeezerPlaybackPlugin(registrar)
            // Register the spotify playback method channel to call the methods from dart
            val channel = MethodChannel(registrar.messenger(), "deezer_playback")
            channel.setMethodCallHandler(plugin)

            val playbackStateEventChannel = EventChannel(registrar.messenger(), "deezer_playback_state_event_channel")
            playbackStateEventChannel.setStreamHandler(plugin.playbackStateStreamHandler)

            val playbackProgressEventChannel = EventChannel(registrar.messenger(), "deezer_playback_progress_event_channel")
            playbackProgressEventChannel.setStreamHandler(plugin.playbackProgressStreamHandler)

            val playbackErrorEventChannel = EventChannel(registrar.messenger(), "deezer_playback_error_event_channel")
            playbackErrorEventChannel.setStreamHandler(plugin.playbackErrorStreamHandler)
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val playbackControls = PlaybackControls(mDeezerConnect = mDeezerConnect, trackPlayer = trackPlayer)
        val seekControls = SeekControls(mDeezerConnect = mDeezerConnect, trackPlayer = trackPlayer)
        when {
            call.method == "iniatilizeDeezer" -> iniatilizeDeezer(call.argument("appId"), result)
            call.method == "connectDeezer" -> connectToDeezer(result)
            call.method == "disconnectDeezer" -> disconnectFromDeezer(result)
            call.method == "getCurrentUser" -> getCurrentUser(result)
            call.method == "getArtist" -> getArtist(call.argument("artistId")!!, result)
            call.method == "getArtistTopTracks" -> getArtistTopTracks(call.argument("artistId")!!, result)
            call.method == "playDeezer" -> play(trackID = call.argument("id"), result = result)
            call.method == "pauseDeezer" -> playbackControls.pause(result = result)
            call.method == "resumeDeezer" -> playbackControls.resume(result = result)
            call.method == "playbackPositionDeezer" -> getPlaybackPosition(result)
            call.method == "isConnected" -> connected(result)
            //call.method == "getCurrentlyPlayingTrack" -> getCurrentlyPlayingTrack(result = result)
            call.method == "skipNext" -> playbackControls.skipNext(result = result)
            call.method == "skipPrevious" -> playbackControls.skipPrevious(result = result)
            call.method == "seekTo" -> seekControls.seekTo(time = call.argument("time"), result = result)
            call.method == "seekToRelativePosition" -> seekControls.seekToRelativePosition(
                    relativeTime = call.argument("relativeTime"), result = result
            )
            call.method == "toggleRepeat" -> playbackControls.toggleRepeat(result = result)
            /**  call.method == "getAuthToken" -> getAuthToken(
            clientId = call.argument("clientId"),
            redirectUrl = call.argument("redirectUrl"),
            result = result)*/
        }
    }

    private fun iniatilizeDeezer(appId: String?, result: Result) {
        if (mDeezerConnect == null) {
            mDeezerConnect = DeezerConnect(registrar.context(), appId)
        }

        if (mDeezerConnect!!.isSessionValid) {
            result.success(true)
        } else if (appId != null) {
            mDeezerConnect = DeezerConnect(registrar.context(), appId)
            result.success(true)
        } else {
            result.error("connect", "error", "the Id you're making is not valid")
        }
    }

    private fun connectToDeezer(result: Result) {
        if (mDeezerConnect!!.isSessionValid) {
            result.success(true)
        } else if (mDeezerConnect != null) {
            connectResultHandler = result
            mDeezerConnect!!.authorize(registrar.activity(), permissions, listener)
        } else {
            result.error("connect", "error", "the Id you're making is not valid")
        }
    }

    private fun disconnectFromDeezer(result: Result) {
        mDeezerConnect?.logout(registrar.activity().application)
        mDeezerConnect = null
        result.success(true)
    }

    private fun getCurrentUser(result: Result) {
        if (mDeezerConnect == null || !mDeezerConnect!!.isSessionValid) {
            result.error("getCurrentUser", "error", "Not connected")
            return
        }

        if (mDeezerConnect!!.isSessionValid) {
            val currentUser = mDeezerConnect!!.currentUser
            val responseMap = mapOf<String, Any>(
                    "id" to currentUser.id,
                    "email" to currentUser.email,
                    "name" to currentUser.name,
                    "pictureUrl" to currentUser.pictureUrl
            )
            result.success(responseMap)
        }
    }

    private fun getArtist(artistId: Long, result: Result) {
        if (!checkConnected(result)) {
            return
        }

        val listener: JsonRequestListener = object : JsonRequestListener() {
            override fun onResult(rs: Any?, requestId: Any?) {
                val artist: Artist = rs as Artist
                val rsMap = mapOf<String, Any>(
                        "id" to artist.id,
                        "name" to artist.name,
                        "pictureUrl" to artist.pictureUrl,
                        "bigImageUrl" to artist.bigImageUrl,
                        "nbAlbums" to artist.nbAlbums,
                        "nbFans" to artist.nbFans
                )
                result.success(rsMap)
            }

            override fun onUnparsedResult(p0: String?, p1: Any?) {
            }

            override fun onException(e: java.lang.Exception?, requestId: Any?) {
                e?.printStackTrace()
                result.error("getArtist", "error", e?.message)
            }
        }

        val request = DeezerRequestFactory.requestArtist(artistId)
        mDeezerConnect?.requestAsync(request, listener)
    }

    private fun getArtistTopTracks(artistId: Long, result: Result) {
        if (!checkConnected(result)) {
            return
        }

        val listener: JsonRequestListener = object : JsonRequestListener() {
            override fun onResult(rs: Any?, requestId: Any?) {
                val topTracks: List<Track> = rs as List<Track>
                val topTracksList = topTracks.map {
                    mapOf(
                            "id" to it.id,
                            "duration" to it.duration,
                            "trackPosition" to it.trackPosition,
                            "shortTitle" to it.shortTitle,
                            "title" to it.title,
                            "previewUrl" to it.previewUrl,
                            "albumCoverUrl" to it.album.coverUrl,
                            "artistName" to it.artist.name
                    )
                }
                result.success(mapOf("tracks" to topTracksList))
            }

            override fun onUnparsedResult(p0: String?, p1: Any?) {
            }

            override fun onException(e: java.lang.Exception?, requestId: Any?) {
                e?.printStackTrace()
                result.error("getArtistTopTracks", "error", e?.message)
            }
        }

        val request = DeezerRequestFactory.requestArtistTopTracks(artistId)
        mDeezerConnect?.requestAsync(request, listener)
    }

    private fun play(trackID: String?, result: Result) {
        if (mDeezerConnect == null || trackID == null) {
            result.error("play", "error", "no Deezer Playlist")
            return
        }

        val playerStateChangeListener = DeezerPlayerStateChangeListener(playbackStateStreamHandler, trackID)
        trackPlayer?.addOnPlayerStateChangeListener(playerStateChangeListener)

        val playerProgressListener = DeezerPlayerProgressListener(playbackProgressStreamHandler, trackID)
        trackPlayer?.addOnPlayerProgressListener(playerProgressListener)

        val playerErrorListener = DeezerPlayerErrorListener(playbackErrorStreamHandler, trackID)
        trackPlayer?.addOnPlayerErrorListener(playerErrorListener)

        trackPlayer!!.playTrack(trackID.toLong())
        result.success(true)
    }

    private fun getPlaybackPosition(result: Result) {
        if (mDeezerConnect != null) {
            result.success(trackPlayer!!.position)
        }
    }

    private fun connected(result: Result) {
        return if (mDeezerConnect != null) {
            result.success(mDeezerConnect!!.isSessionValid)
        } else {
            result.success(false)
        }
    }

    private fun checkConnected(result: Result): Boolean {
        if (mDeezerConnect == null || !(mDeezerConnect?.isSessionValid!!)) {
            result.error("getArtist", "error", "Call connectToDeezer first")
            return false
        }
        return true
    }
}

class DeezerPlayerStateChangeListener(
        private val playbackStateStreamHandler: PlaybackStateStreamHandler,
        private val trackID: String
) : OnPlayerStateChangeListener {

    override fun onPlayerStateChange(state: PlayerState?, timePos: Long) {
        playbackStateStreamHandler.stateChanged(state?.name ?: "", trackID, timePos)
    }
}

class DeezerPlayerProgressListener(
        private val playbackProgressStreamHandler: PlaybackProgressStreamHandler,
        private val trackID: String
) : OnPlayerProgressListener {

    override fun onPlayerProgress(timePos: Long) {
        playbackProgressStreamHandler.progressChanged(timePos, trackID)
    }
}

class DeezerPlayerErrorListener(
        private val playbackErrorStreamHandler: PlaybackErrorStreamHandler,
        private val trackID: String
) : OnPlayerErrorListener {
    override fun onPlayerError(ex: java.lang.Exception?, timePos: Long) {
        playbackErrorStreamHandler.onError(ex!!, trackID, timePos)
    }

}

class PlaybackStateStreamHandler : StreamHandler {
    private var eventSink: EventSink? = null

    fun stateChanged(newState: String, trackID: String, timePos: Long) {
        eventSink?.success(mapOf("state" to newState, "trackID" to trackID, "timePos" to timePos))
    }

    override fun onListen(args: Any?, events: EventSink?) {
        eventSink = events
    }

    override fun onCancel(p0: Any?) {
        eventSink = null
    }
}

class PlaybackProgressStreamHandler : StreamHandler {
    private var eventSink: EventSink? = null

    fun progressChanged(timePos: Long, trackID: String) {
        eventSink?.success(mapOf("timePos" to timePos, "trackID" to trackID))
    }

    override fun onListen(args: Any?, events: EventSink?) {
        eventSink = events
    }

    override fun onCancel(args: Any?) {
        eventSink = null
    }
}

class PlaybackErrorStreamHandler : StreamHandler {
    private var eventSink: EventSink? = null

    fun onError(ex: java.lang.Exception, trackID: String, timePos: Long) {
        eventSink?.error(ex.message, ex.message, "Error on playback error stream for track $trackID at time $timePos")
    }

    override fun onListen(args: Any?, events: EventSink?) {
        eventSink = events
    }

    override fun onCancel(args: Any?) {
        eventSink = null
    }
}