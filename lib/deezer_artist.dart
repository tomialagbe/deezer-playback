class DeezerArtist {
  int id;
  String name;
  String pictureUrl;
  String bigImageUrl;
  int nbAlbums;
  int nbFans;

  DeezerArtist();

  factory DeezerArtist.fromMap(Map map) {
    return DeezerArtist()
      ..id = int.tryParse((map['id'].toString()))
      ..name = map['name']
      ..pictureUrl = map['pictureUrl']
      ..bigImageUrl = map['bigImageUrl']
      ..nbAlbums = map['nbAlbums']
      ..nbFans = map['nbFans'];
  }
}
