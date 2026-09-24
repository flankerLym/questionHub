import 'package:flutter/material.dart';

import 'pages/home_page.dart';
import 'services/database_service.dart';
import 'theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await DatabaseService.instance.db;
  runApp(const QuestionArchiveApp());
}

class QuestionArchiveApp extends StatelessWidget {
  const QuestionArchiveApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: '我的题库',
      theme: AppTheme.light,
      home: const HomePage(),
    );
  }
}
