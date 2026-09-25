import 'dart:io';
import 'package:flutter/material.dart';

import '../models/folder.dart';
import '../services/database_service.dart';
import '../theme/app_theme.dart';
import 'questions_page.dart';
import 'search_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  bool loading = true;
  List<Folder> folders = [];
  Map<String, int> counts = {};

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final data = await DatabaseService.instance.folders();
    final map = <String, int>{};
    for (final f in data) {
      map[f.id] = await DatabaseService.instance.questionCount(f.id);
    }
    if (mounted) setState(() { folders = data; counts = map; loading = false; });
  }

  Future<void> _import() async {
    try {
      final data = await DatabaseService.instance.importFromJsonFile();
      await _load();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('已全量覆盖：${data.folders.length} 个文件夹，${data.questions.length} 道题')),
        );
      }
    } on FileSystemException catch (e) {
      if (!e.message.contains('IMPORT_CANCELLED') && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('导入失败：$e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('我的题库', style: TextStyle(fontWeight: FontWeight.w750)),
            Text('Question Archive', style: TextStyle(fontSize: 12, color: AppTheme.muted)),
          ],
        ),
        actions: [
          IconButton(
            tooltip: '全局搜索',
            icon: const Icon(Icons.search_rounded),
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const SearchPage()),
            ),
          ),
          IconButton(
            tooltip: '导入 JSON 全量覆盖',
            icon: const Icon(Icons.file_download_outlined),
            onPressed: _import,
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : folders.isEmpty
              ? _Empty(onImport: _import)
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
                    children: [
                      Container(
                        padding: const EdgeInsets.all(18),
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(
                            colors: [Color(0xFFE9DDCC), Color(0xFFF7F1E8)],
                          ),
                          borderRadius: BorderRadius.circular(22),
                          border: Border.all(color: AppTheme.border),
                        ),
                        child: const Row(
                          children: [
                            Icon(Icons.menu_book_rounded, size: 34, color: AppTheme.woodDark),
                            SizedBox(width: 14),
                            Expanded(
                              child: Text(
                                '离线题库 · SQLite 本地保存\n从 Java 项目导出的 JSON 可随时全量覆盖',
                                style: TextStyle(height: 1.55, color: AppTheme.ink),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      ...folders.map((f) => Padding(
                            padding: const EdgeInsets.only(bottom: 10),
                            child: Card(
                              child: ListTile(
                                contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
                                leading: Container(
                                  width: 46,
                                  height: 46,
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFE4E9DF),
                                    borderRadius: BorderRadius.circular(14),
                                  ),
                                  child: const Icon(Icons.folder_outlined, color: AppTheme.woodDark),
                                ),
                                title: Text(
                                  f.name,
                                  style: const TextStyle(fontWeight: FontWeight.w700, color: AppTheme.ink),
                                ),
                                subtitle: Text('${counts[f.id] ?? 0} 个问题'),
                                trailing: const Icon(Icons.chevron_right_rounded),
                                onTap: () => Navigator.push(
                                  context,
                                  MaterialPageRoute(builder: (_) => QuestionsPage(folder: f)),
                                ),
                              ),
                            ),
                          )),
                    ],
                  ),
                ),
    );
  }
}

class _Empty extends StatelessWidget {
  final VoidCallback onImport;
  const _Empty({required this.onImport});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.inventory_2_outlined, size: 58, color: AppTheme.wood),
            const SizedBox(height: 16),
            const Text('题库还是空的', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
            const SizedBox(height: 8),
            const Text('导入 Java 项目导出的 JSON，数据会写入手机本地 SQLite。', textAlign: TextAlign.center),
            const SizedBox(height: 18),
            FilledButton.icon(
              onPressed: onImport,
              icon: const Icon(Icons.file_open_outlined),
              label: const Text('导入题库 JSON'),
            ),
          ],
        ),
      ),
    );
  }
}
