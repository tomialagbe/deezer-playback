import 'dart:async';

import 'package:deezer_playback/deezer_track.dart';
import 'package:deezer_playback/player_events.dart';
import 'package:dio/dio.dart';
import 'package:flutter/services.dart';

import 'deezer_artist.dart';
import 'deezer_user.dart';

class DeezerPlayback {
  static const MethodChannel _channel = const MethodChannel('deezer_playback');
  static const EventChannel _playerStateEventChannel = const EventChannel('deezer_playback_state_event_channel');
  static const EventChannel _playerProgressEventChannel = const EventChannel('deezer_playback_progress_event_channel');
  static const EventChannel _playerErrorEventChannel = const EventChannel('deezer_playback_error_event_channel');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Stream<PlayerStateEvent> get playerStateEvents {
    return _playerStateEventChannel.receiveBroadcastStream().map((dynamic event) => _mapToPlayerStateEvent(event));
  }

  static Stream<PlayerProgressEvent> get playerProgressEvents {
    return _playerProgressEventChannel
        .receiveBroadcastStream()
        .map((dynamic event) => _mapToPlayerProgressEvent(event));
  }

  /// The spotifyConnect method can be called to initialize spotify
  static Future<bool> iniatilizeDeezer({String appId}) async {
    final bool inialized = await _channel.invokeMethod("iniatilizeDeezer", {"appId": appId});
    return inialized;
  }

  /// The spotifyConnect method can be called to initialize spotify
  static Future<bool> connectToDeezer() async {
    final bool connect = await _channel.invokeMethod("connectDeezer");
    return connect;
  }

  static Future<bool> disconnectDeezer() async {
    final bool ok = await _channel.invokeMethod("disconnectDeezer");
    return ok;
  }

  static Future<DeezerUser> getCurrentUser() async {
    final Map result = await _channel.invokeMethod("getCurrentUser");
    return DeezerUser.fromMap(result);
  }

  static Future<DeezerArtist> getArtist(int artistId) async {
    final Map result = await _channel.invokeMethod("getArtist", {"artistId": artistId});
    return DeezerArtist.fromMap(result);
  }

  static Future<List<DeezerTrack>> getArtistTopTracks(int artistId) async {
    final Map result = await _channel.invokeMethod("getArtistTopTracks", {"artistId": artistId});
    final List tracks = result["tracks"];
    return tracks.map((dynamic map) {
      Map mp = map;
      return DeezerTrack.fromMap(mp);
    }).toList();
  }

  /// The play method is used to play an song/album/playlist
  static Future<bool> play(String id) async {
    final bool success = await _channel.invokeMethod("playDeezer", {"id": id});
    return success;
  }

  /// The pause method is used to pause the current playing song
  static Future<bool> pause() async {
    final bool paused = await _channel.invokeMethod("pauseDeezer");
    return paused;
  }

  /// The resume method resumes the currently paused song
  static Future<bool> resume() async {
    final bool resumed = await _channel.invokeMethod("resumeDeezer");
    return resumed;
  }

  /// The getPlaybackPosition method is used to get the current tracks playback position
  static Future<int> getPlaybackPosition() async {
    final int position = await _channel.invokeMethod("playbackPositionDeezer");
    return position;
  }

  /// The is connected method is used to check if
  /// the Spotify player is correctly Initialized
  static Future<bool> isConnected() async {
    final bool connected = await _channel.invokeMethod("isConnected");
    return connected;
  }

  /// The skipNext method is used to play the next song
  static Future<bool> skipNext() async {
    final bool success = await _channel.invokeMethod("skipNext");
    return success;
  }

  /// The skipPrevious method is used to play the previous song
  static Future<bool> skipPrevious() async {
    final bool success = await _channel.invokeMethod("skipPrevious");
    return success;
  }

  /// The toggleRepeat method is used to toggle the repeat types
  static Future<bool> toggleRepeat() async {
    final bool success = await _channel.invokeMethod("toggleRepeat");
    return success;
  }

  /// The seekTo method is used to seek to a specific time in a song
  static Future<bool> seekTo(int time) async {
    final bool success = await _channel.invokeMethod("seekTo", {"time": time.toString()});
    return success;
  }

  static Future<bool> seekToRelativePosition(int relativeTime) async {
    final bool success =
        await _channel.invokeMethod("seekToRelativePosition", {"relativeTime": relativeTime.toString()});
    return success;
  }

  static Future<List> searchTracks(String search) async {
    Dio dio = new Dio();
    try {
      Response response = await dio.get("https://api.deezer.com/search?q=track:" + search + "&strict=off");
      var jsons = (response.data)["data"] as List;
      return jsons;

      //return tracks.toList();
    } catch (e) {
      print(e);
      return null;
    }
  }

  static Future<Object> getTrack(String id) async {
    Dio dio = new Dio();
    try {
      Response response = await dio.get("https://api.deezer.com/track/" + id);

      var json = (response.data);
      return json;
      //return tracks.toList();
    } catch (e) {
      print(e);
      return null;
    }
  }

  static PlayerStateEvent _mapToPlayerStateEvent(Map eventData) {
    final stateStr = eventData['state'];
    final playerState = PlayerState.values.firstWhere((s) => s.toString() == stateStr);
    final trackId = eventData['trackID'];
    final int timePos = int.tryParse(eventData['timePos'].toString()) ?? 0;
    return PlayerStateEvent(trackId, playerState, timePos);
  }

  static PlayerProgressEvent _mapToPlayerProgressEvent(Map eventData) {
    final trackId = eventData['trackID'];
    final int timePos = int.tryParse(eventData['timePos'].toString()) ?? 0;
    return PlayerProgressEvent(trackId, timePos);
  }
}
