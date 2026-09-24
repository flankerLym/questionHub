import 'package:flutter_test/flutter_test.dart';
import 'package:question_archive_mobile/services/archive_validator.dart';

void main() {
  test('parses original archive format', () {
    final data = parseArchive({
      'version': 1,
      'folders': [
        {'id': 'folder_1', 'name': '算法', 'sort': 0, 'createdAt': 1, 'updatedAt': 1}
      ],
      'qaItems': [
        {
          'id': 'qa_1',
          'folderId': 'folder_1',
          'question': '三数之和',
          'answer': '排序 + 双指针',
          'createdAt': 1,
          'updatedAt': 1
        }
      ]
    });
    expect(data.folders.length, 1);
    expect(data.questions.length, 1);
  });
}
