import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

import '../models/folder.dart';
import '../models/question.dart';
import 'archive_validator.dart';

class DatabaseService {
  DatabaseService._();
  static final instance = DatabaseService._();

  Database? _db;

  Future<Database> get db async {
    if (_db != null) return _db!;
    final path = p.join(await getDatabasesPath(), 'question_archive_mobile.db');
    _db = await openDatabase(
      path,
      version: 1,
      onCreate: (db, _) async {
        await db.execute('''
          CREATE TABLE folders(
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            sort INTEGER NOT NULL,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
          )
        ''');
        await db.execute('''
          CREATE TABLE questions(
            id TEXT PRIMARY KEY,
            folder_id TEXT NOT NULL,
            question TEXT NOT NULL,
            answer TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
          )
        ''');
        await db.execute('CREATE INDEX idx_questions_folder ON questions(folder_id)');
      },
    );
    return _db!;
  }

  Future<List<Folder>> folders() async {
    final d = await db;
    final rows = await d.query('folders', orderBy: 'sort ASC, created_at ASC');
    return rows.map(Folder.fromDb).toList();
  }

  Future<int> questionCount(String folderId) async {
    final d = await db;
    final rows = await d.rawQuery(
      'SELECT COUNT(*) AS c FROM questions WHERE folder_id = ?',
      [folderId],
    );
    return Sqflite.firstIntValue(rows) ?? 0;
  }

  Future<List<Question>> questions(String folderId, {String keyword = ''}) async {
    final d = await db;
    final key = keyword.trim();
    final rows = key.isEmpty
        ? await d.query(
            'questions',
            where: 'folder_id = ?',
            whereArgs: [folderId],
            orderBy: 'updated_at DESC',
          )
        : await d.query(
            'questions',
            where: 'folder_id = ? AND (question LIKE ? OR answer LIKE ?)',
            whereArgs: [folderId, '%$key%', '%$key%'],
            orderBy: 'updated_at DESC',
          );
    return rows.map(Question.fromDb).toList();
  }

  Future<List<Question>> globalSearch(String keyword) async {
    final key = keyword.trim();
    if (key.isEmpty) return [];
    final d = await db;
    final rows = await d.query(
      'questions',
      where: 'question LIKE ? OR answer LIKE ?',
      whereArgs: ['%$key%', '%$key%'],
      orderBy: 'updated_at DESC',
    );
    return rows.map(Question.fromDb).toList();
  }

  Future<ArchiveData> importFromJsonFile() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['json'],
      withData: false,
    );
    if (result == null || result.files.single.path == null) {
      throw const FileSystemException('IMPORT_CANCELLED');
    }

    final file = File(result.files.single.path!);
    if (await file.length() > 20 * 1024 * 1024) {
      throw const FormatException('归档文件不能超过 20MB');
    }

    final text = await file.readAsString();
    final decoded = jsonDecode(text);
    if (decoded is! Map) throw const FormatException('JSON 根节点必须是对象');
    final data = parseArchive(Map<String, dynamic>.from(decoded));

    final d = await db;
    await d.transaction((txn) async {
      await txn.delete('questions');
      await txn.delete('folders');
      for (final f in data.folders) {
        await txn.insert('folders', f.toDb());
      }
      for (final q in data.questions) {
        await txn.insert('questions', q.toDb());
      }
    });

    return data;
  }
}
