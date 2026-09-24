class Question {
  final String id;
  final String folderId;
  final String question;
  final String answer;
  final int createdAt;
  final int updatedAt;

  const Question({
    required this.id,
    required this.folderId,
    required this.question,
    required this.answer,
    required this.createdAt,
    required this.updatedAt,
  });

  factory Question.fromJson(Map<String, dynamic> json) => Question(
        id: json['id'] as String,
        folderId: json['folderId'] as String,
        question: json['question'] as String,
        answer: json['answer'] as String,
        createdAt: (json['createdAt'] as num?)?.toInt() ?? 0,
        updatedAt: (json['updatedAt'] as num?)?.toInt() ?? 0,
      );

  Map<String, Object?> toDb() => {
        'id': id,
        'folder_id': folderId,
        'question': question,
        'answer': answer,
        'created_at': createdAt,
        'updated_at': updatedAt,
      };

  factory Question.fromDb(Map<String, Object?> row) => Question(
        id: row['id'] as String,
        folderId: row['folder_id'] as String,
        question: row['question'] as String,
        answer: row['answer'] as String,
        createdAt: row['created_at'] as int,
        updatedAt: row['updated_at'] as int,
      );
}
