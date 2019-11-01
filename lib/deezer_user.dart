class DeezerUser {
  int id;
  String email;
  String name;
  String pictureUrl;

  DeezerUser();

  factory DeezerUser.fromMap(Map map) {
    return DeezerUser()
      ..id = int.tryParse(map["id"].toString())
      ..email = map["email"]
      ..name = map["name"]
      ..pictureUrl = map["pictureUrl"];
  }
}
