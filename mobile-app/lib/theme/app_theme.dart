import 'package:flutter/material.dart';

class AppTheme {
  static const background = Color(0xFFF4EFE6);
  static const card = Color(0xFFFFFBF5);
  static const wood = Color(0xFF9A7A5B);
  static const woodDark = Color(0xFF6F533C);
  static const sage = Color(0xFF8FA18A);
  static const ink = Color(0xFF202020);
  static const muted = Color(0xFF6B625A);
  static const border = Color(0xFFD8CBBB);

  static ThemeData get light {
    final scheme = ColorScheme.fromSeed(
      seedColor: wood,
      brightness: Brightness.light,
      surface: card,
    ).copyWith(
      primary: woodDark,
      secondary: sage,
      surface: card,
      onSurface: ink,
      onPrimary: Colors.white,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: background,
      fontFamilyFallback: const ['Noto Sans CJK SC', 'Microsoft YaHei', 'sans-serif'],
      textTheme: ThemeData.light().textTheme.apply(
            bodyColor: ink,
            displayColor: ink,
          ),
      appBarTheme: const AppBarTheme(
        backgroundColor: background,
        foregroundColor: ink,
        centerTitle: false,
        elevation: 0,
      ),
      cardTheme: CardThemeData(
        color: card,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
          side: const BorderSide(color: border),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: card,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: border),
        ),
      ),
    );
  }
}
