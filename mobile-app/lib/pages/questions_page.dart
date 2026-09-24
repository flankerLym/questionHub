import 'dart:async';

import 'package:flutter/material.dart';

import '../models/folder.dart';
import '../models/question.dart';
import '../services/database_service.dart';
import '../theme/app_theme.dart';
import 'detail_page.dart';

class QuestionsPage extends StatefulWidget {
  final Folder folder;
  const QuestionsPage({super.key, required this.folder});

  @override
  State<QuestionsPage> createState() => _QuestionsPageState();
}

class _QuestionsPageState extends State<QuestionsPage> {
  final _search = TextEditingController();
  Timer? _timer;
  bool loading = true;
  List<Question> items = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _search.dispose();
    super.dispose();
  }

  Future<void> _load([String keyword = '']) async {
    if (mounted) setState(() => loading = true);
    final data = await DatabaseService.instance.questions(widget.folder.id, keyword: keyword);
    if (mounted) setState(() { items = data; loading = false; });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.folder.name)),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 6, 16, 12),
            child: TextField(
              controller: _search,
              decoration: const InputDecoration(
                hintText: '搜索问题或答案…',
                prefixIcon: Icon(Icons.search_rounded),
              ),
              onChanged: (value) {
                _timer?.cancel();
                _timer = Timer(const Duration(milliseconds: 250), () => _load(value));
              },
            ),
          ),
          Expanded(
            child: loading
                ? const Center(child: CircularProgressIndicator())
                : items.isEmpty
                    ? const Center(child: Text('没有匹配的问题'))
                    : ListView.separated(
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                        itemCount: items.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 10),
                        itemBuilder: (context, i) {
                          final q = items[i];
                          return Card(
                            child: ListTile(
                              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                              title: Text(
                                q.question,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontWeight: FontWeight.w650,
                                  color: AppTheme.ink,
                                ),
                              ),
                              subtitle: q.answer.trim().isEmpty
                                  ? null
                                  : Text(
                                      q.answer.replaceAll('\n', ' '),
                                      maxLines: 2,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                              trailing: const Icon(Icons.chevron_right_rounded),
                              onTap: () => Navigator.push(
                                context,
                                MaterialPageRoute(builder: (_) => DetailPage(item: q)),
                              ),
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}
