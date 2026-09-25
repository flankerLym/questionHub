import 'dart:async';
import 'package:flutter/material.dart';

import '../models/question.dart';
import '../services/database_service.dart';
import 'detail_page.dart';

class SearchPage extends StatefulWidget {
  const SearchPage({super.key});

  @override
  State<SearchPage> createState() => _SearchPageState();
}

class _SearchPageState extends State<SearchPage> {
  final ctrl = TextEditingController();
  Timer? timer;
  List<Question> items = [];

  @override
  void dispose() {
    timer?.cancel();
    ctrl.dispose();
    super.dispose();
  }

  Future<void> run(String text) async {
    final data = await DatabaseService.instance.globalSearch(text);
    if (mounted) setState(() => items = data);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('全局搜索')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: TextField(
              controller: ctrl,
              autofocus: true,
              decoration: const InputDecoration(
                hintText: '搜索全部问题和答案…',
                prefixIcon: Icon(Icons.search_rounded),
              ),
              onChanged: (v) {
                timer?.cancel();
                timer = Timer(const Duration(milliseconds: 250), () => run(v));
              },
            ),
          ),
          Expanded(
            child: items.isEmpty
                ? const Center(child: Text('输入关键词开始搜索'))
                : ListView.builder(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                    itemCount: items.length,
                    itemBuilder: (context, i) => Card(
                      child: ListTile(
                        title: Text(items[i].question),
                        trailing: const Icon(Icons.chevron_right_rounded),
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(builder: (_) => DetailPage(item: items[i])),
                        ),
                      ),
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}
