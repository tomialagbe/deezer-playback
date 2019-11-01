class DeezerTrack {
  int id;
  int duration;
  int trackPosition;
  String shortTitle;
  String title;
  String previewUrl;
  String albumCoverUrl;
  String artistName;

  DeezerTrack();

  factory DeezerTrack.fromMap(Map map) {
    return DeezerTrack()
      ..id = int.tryParse(map['id'].toString())
      ..duration = int.tryParse(map['duration'].toString())
      ..trackPosition = int.tryParse(map['trackPosition'].toString())
      ..shortTitle = map['shortTitle']
      ..title = map['title']
      ..previewUrl = map['previewUrl']
      ..albumCoverUrl = map['albumCoverUrl']
      ..artistName = map['artistName'];
  }
}
