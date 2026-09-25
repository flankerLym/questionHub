class Folder {
  final String id;
  final String name;
  final int sort;
  final int createdAt;
  final int updatedAt;

  const Folder({
    required this.id,
    required this.name,
    required this.sort,
    required this.createdAt,
    required this.updatedAt,
  });

  factory Folder.fromJson(Map<String, dynamic> json) => Folder(
        id: json['id'] as String,
        name: json['name'] as String,
        sort: (json['sort'] as num?)?.toInt() ?? 0,
        createdAt: (json['createdAt'] as num?)?.toInt() ?? 0,
        updatedAt: (json['updatedAt'] as num?)?.toInt() ?? 0,
      );

  Map<String, Object?> toDb() => {
        'id': id,
        'name': name,
        'sort': sort,
        'created_at': createdAt,
        'updated_at': updatedAt,
      };

  factory Folder.fromDb(Map<String, Object?> row) => Folder(
        id: row['id'] as String,
        name: row['name'] as String,
        sort: row['sort'] as int,
        createdAt: row['created_at'] as int,
        updatedAt: row['updated_at'] as int,
      );
}
