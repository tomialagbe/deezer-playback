enum PlayerState {
  STARTED,
  INITIALIZING,
  READY,
  PLAYING,
  PAUSED,
  PLAYBACK_COMPLETED,
  WAITING_FOR_DATA,
  STOPPED,
  RELEASED
}

class PlayerStateEvent {
  final String trackId;
  final PlayerState state;
  final int playerPosition;

  PlayerStateEvent(this.trackId, this.state, this.playerPosition);
}

class PlayerProgressEvent {
  final String trackId;
  final int playerPosition;

  PlayerProgressEvent(this.trackId, this.playerPosition);
}
