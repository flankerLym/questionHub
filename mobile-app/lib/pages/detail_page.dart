import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

import '../models/question.dart';
import '../theme/app_theme.dart';

class DetailPage extends StatelessWidget {
  final Question item;
  const DetailPage({super.key, required this.item});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('问题详情'),
        actions: [
          IconButton(
            tooltip: '复制答案',
            onPressed: () async {
              await Clipboard.setData(ClipboardData(text: item.answer));
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('答案已复制')),
                );
              }
            },
            icon: const Icon(Icons.copy_rounded),
          ),
        ],
      ),
      body: SelectionArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 28),
          children: [
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                color: AppTheme.card,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: AppTheme.border),
              ),
              child: Text(
                item.question,
                style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                  height: 1.45,
                  color: AppTheme.ink,
                ),
              ),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                color: AppTheme.card,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: AppTheme.border),
              ),
              child: item.answer.trim().isEmpty
                  ? const Text('暂无答案', style: TextStyle(color: AppTheme.muted))
                  : MarkdownBody(
                      data: item.answer,
                      selectable: true,
                      styleSheet: MarkdownStyleSheet(
                        p: const TextStyle(fontSize: 16, height: 1.7, color: AppTheme.ink),
                        code: const TextStyle(
                          fontFamily: 'monospace',
                          fontSize: 14,
                          color: AppTheme.ink,
                          backgroundColor: Color(0xFFECE5DB),
                        ),
                        codeblockDecoration: BoxDecoration(
                          color: const Color(0xFFECE5DB),
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
