import '../models/folder.dart';
import '../models/question.dart';

class ArchiveData {
  final List<Folder> folders;
  final List<Question> questions;

  const ArchiveData(this.folders, this.questions);
}

ArchiveData parseArchive(Map<String, dynamic> data) {
  if (data['version'] != 1) {
    throw const FormatException('只支持 version = 1 的题库归档');
  }
  final rawFolders = data['folders'];
  final rawItems = data['qaItems'];
  if (rawFolders is! List || rawItems is! List) {
    throw const FormatException('归档缺少 folders 或 qaItems');
  }

  final folders = rawFolders
      .map((e) => Folder.fromJson(Map<String, dynamic>.from(e as Map)))
      .toList();
  final questions = rawItems
      .map((e) => Question.fromJson(Map<String, dynamic>.from(e as Map)))
      .toList();

  final folderIds = <String>{};
  final names = <String>{};
  for (final f in folders) {
    if (f.id.isEmpty || f.name.trim().isEmpty || f.name.trim().length > 60) {
      throw const FormatException('存在无效文件夹');
    }
    if (!folderIds.add(f.id)) throw const FormatException('文件夹 ID 重复');
    if (!names.add(f.name.trim().toLowerCase())) {
      throw const FormatException('存在同名文件夹');
    }
  }

  final ids = <String>{};
  for (final q in questions) {
    if (q.id.isEmpty || q.folderId.isEmpty || q.question.trim().isEmpty) {
      throw const FormatException('存在无效问题');
    }
    if (q.question.trim().length > 500 || q.answer.length > 50000) {
      throw const FormatException('问题或答案长度超限');
    }
    if (!ids.add(q.id)) throw const FormatException('问题 ID 重复');
    if (!folderIds.contains(q.folderId)) {
      throw const FormatException('存在问题引用了不存在的文件夹');
    }
  }

  return ArchiveData(folders, questions);
}
