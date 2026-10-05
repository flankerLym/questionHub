package com.questionhub.desktop.ui;

import com.questionhub.desktop.service.AssetService;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Offline answer renderer.
 *
 * Supports:
 * - plain text paragraphs
 * - fenced code blocks: ```java ... ```
 * - inline code: `code`
 * - inline LaTeX: $x^2$
 * - block LaTeX: $$\frac{a}{b}$$ or multiline $$ ... $$
 *
 * LaTeX is rendered locally to PNG with JLaTeXMath, so the desktop app does not
 * need network access or a CDN.
 */
public final class AnswerRenderer {
    private AnswerRenderer() {}

    public static String toHtml(String source, String emptyText) {
        return toHtml(source, emptyText, null);
    }

    public static String toHtml(String source, String emptyText, Path dataDir) {
        String text = source == null ? "" : source.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder body = new StringBuilder();

        if (text.isBlank()) {
            body.append("<div class=\"empty\">")
                    .append(escape(emptyText == null ? "还没有整理内容" : emptyText))
                    .append("</div>");
            return page(body.toString());
        }

        String[] lines = text.split("\n", -1);
        boolean inCode = false;
        String codeLanguage = "";
        StringBuilder code = new StringBuilder();

        boolean inMath = false;
        StringBuilder math = new StringBuilder();

        for (String line : lines) {
            String trim = line.trim();

            if (inCode) {
                if (trim.startsWith("```")) {
                    body.append(codeBlock(codeLanguage, code.toString()));
                    code.setLength(0);
                    codeLanguage = "";
                    inCode = false;
                } else {
                    code.append(line).append('\n');
                }
                continue;
            }

            if (inMath) {
                if (trim.endsWith("$$")) {
                    String before = line.substring(0, line.lastIndexOf("$$"));
                    if (!before.isBlank()) math.append(before).append('\n');
                    body.append(mathBlock(math.toString().trim()));
                    math.setLength(0);
                    inMath = false;
                } else {
                    math.append(line).append('\n');
                }
                continue;
            }

            if (trim.startsWith("```")) {
                inCode = true;
                codeLanguage = trim.length() > 3 ? trim.substring(3).trim() : "";
                continue;
            }

            if (trim.startsWith("$$")) {
                String rest = trim.substring(2);
                int close = rest.lastIndexOf("$$");
                if (close >= 0) {
                    body.append(mathBlock(rest.substring(0, close).trim()));
                } else {
                    inMath = true;
                    if (!rest.isBlank()) math.append(rest).append('\n');
                }
                continue;
            }

            java.util.regex.Matcher image = AssetService.IMAGE_TOKEN.matcher(trim);
            if (image.matches()) {
                body.append(imageBlock(
                        dataDir,
                        image.group(1),
                        Integer.parseInt(image.group(2))));
            } else if (trim.isEmpty()) {
                body.append("<div class=\"gap\"></div>");
            } else if (trim.startsWith("### ")) {
                body.append("<h3>").append(inline(trim.substring(4))).append("</h3>");
            } else if (trim.startsWith("## ")) {
                body.append("<h2>").append(inline(trim.substring(3))).append("</h2>");
            } else if (trim.startsWith("# ")) {
                body.append("<h1>").append(inline(trim.substring(2))).append("</h1>");
            } else if (trim.startsWith("- ") || trim.startsWith("* ")) {
                body.append("<div class=\"bullet\"><span>•</span><div>")
                        .append(inline(trim.substring(2)))
                        .append("</div></div>");
            } else if (trim.startsWith("> ")) {
                body.append("<blockquote>").append(inline(trim.substring(2))).append("</blockquote>");
            } else {
                body.append("<p>").append(inline(line)).append("</p>");
            }
        }

        if (inCode) body.append(codeBlock(codeLanguage, code.toString()));
        if (inMath) body.append(mathBlock(math.toString().trim()));

        return page(body.toString());
    }

    private static String inline(String text) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '\\' && i + 1 < text.length() && text.charAt(i + 1) == '$') {
                out.append("$");
                i += 2;
                continue;
            }

            if (text.charAt(i) == '`') {
                int end = text.indexOf('`', i + 1);
                if (end > i) {
                    out.append("<code class=\"inline-code\">")
                            .append(escape(text.substring(i + 1, end)))
                            .append("</code>");
                    i = end + 1;
                    continue;
                }
            }

            if (text.charAt(i) == '$') {
                int end = findMathEnd(text, i + 1);
                if (end > i + 1) {
                    out.append(mathInline(text.substring(i + 1, end)));
                    i = end + 1;
                    continue;
                }
            }

            if (i + 1 < text.length() && text.startsWith("**", i)) {
                int end = text.indexOf("**", i + 2);
                if (end > i + 2) {
                    out.append("<strong>")
                            .append(escape(text.substring(i + 2, end)))
                            .append("</strong>");
                    i = end + 2;
                    continue;
                }
            }

            int next = nextSpecial(text, i + 1);
            out.append(escape(text.substring(i, next)));
            i = next;
        }
        return out.toString();
    }

    private static int findMathEnd(String text, int start) {
        for (int i = start; i < text.length(); i++) {
            if (text.charAt(i) == '$' && (i == 0 || text.charAt(i - 1) != '\\')) return i;
        }
        return -1;
    }

    private static int nextSpecial(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '`' || c == '$' || c == '\\' || (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*')) {
                return i;
            }
        }
        return text.length();
    }

    private static String imageBlock(Path dataDir, String fileName, int width) {
        int safeWidth = Math.max(20, Math.min(100, width));
        if (dataDir == null) {
            return missingImage("图片资源不可用：" + fileName);
        }

        try {
            Path assetsDir = dataDir.resolve("assets").toAbsolutePath().normalize();
            Path file = assetsDir.resolve(fileName).normalize();
            if (!file.startsWith(assetsDir) || !Files.isRegularFile(file)) {
                return missingImage("图片不存在：" + fileName);
            }

            String mime = fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")
                    ? "image/jpeg"
                    : "image/png";

            byte[] bytes = Files.readAllBytes(file);
            BufferedImage source = ImageIO.read(file.toFile());
            int naturalWidth = source == null ? 900 : Math.max(1, source.getWidth());

            // width 表示“相对原图”的缩放比例，而不是“相对窗口”的宽度。
            // 这样主界面和放大窗口里看到的图片尺寸一致，不会因为窗口变大而被二次放大。
            int imageWidth = Math.max(80, (int) Math.round(naturalWidth * (safeWidth / 100.0)));
            int frameWidth = imageWidth + 24; // 左右各 12px 的图片卡片内边距

            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "<div class=\"answer-image-row\">"
                    + "<figure class=\"answer-image\" style=\"width:" + frameWidth + "px;max-width:100%;box-sizing:border-box;\">"
                    + "<img style=\"width:100%;height:auto;max-width:100%;object-fit:contain;\" "
                    + "src=\"data:" + mime + ";base64," + base64 + "\" alt=\"截图\"/>"
                    + "<figcaption>截图 · 原图 " + safeWidth + "%</figcaption>"
                    + "</figure>"
                    + "</div>";
        } catch (Exception e) {
            return missingImage("图片读取失败：" + fileName);
        }
    }

    private static String missingImage(String text) {
        return "<div class=\"missing-image\">" + escape(text) + "</div>";
    }

    private static String codeBlock(String language, String code) {
        String lang = language == null || language.isBlank() ? "CODE" : language.toUpperCase();
        return "<section class=\"code-card\">"
                + "<div class=\"code-head\">" + escape(lang) + "</div>"
                + "<pre><code>" + escape(stripTrailingNewline(code)) + "</code></pre>"
                + "</section>";
    }

    private static String mathInline(String latex) {
        String image = latexImage(latex, false);
        if (image != null) return "<span class=\"math-inline\">" + image + "</span>";
        return "<code class=\"latex-fallback\">$" + escape(latex) + "$</code>";
    }

    private static String mathBlock(String latex) {
        if (latex == null || latex.isBlank()) return "";
        String image = latexImage(latex, true);
        if (image != null) return "<div class=\"math-block\">" + image + "</div>";
        return "<div class=\"latex-fallback block\">$$" + escape(latex) + "$$</div>";
    }

    private static String latexImage(String latex, boolean display) {
        try {
            TeXFormula formula = new TeXFormula(latex);
            TeXIcon icon = formula.createTeXIcon(
                    display ? TeXConstants.STYLE_DISPLAY : TeXConstants.STYLE_TEXT,
                    display ? 21f : 17f
            );
            icon.setInsets(new Insets(2, 2, 2, 2));

            BufferedImage image = new BufferedImage(
                    Math.max(1, icon.getIconWidth()),
                    Math.max(1, icon.getIconHeight()),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            JLabel label = new JLabel();
            label.setForeground(new Color(38, 50, 41));
            icon.paintIcon(label, g, 0, 0);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            String base64 = Base64.getEncoder().encodeToString(out.toByteArray());
            return "<img alt=\"" + escapeAttr(latex) + "\" src=\"data:image/png;base64," + base64 + "\"/>";
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String page(String body) {
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    html, body {
                      margin: 0;
                      padding: 0;
                      background: #fffefa;
                      color: #263229;
                      font-family: "Microsoft YaHei UI", "Segoe UI", sans-serif;
                      font-size: 15px;
                      line-height: 1.78;
                    }
                    body { padding: 24px 26px 38px; box-sizing: border-box; }
                    p { margin: 0 0 10px; white-space: pre-wrap; word-break: break-word; }
                    h1, h2, h3 { color: #203026; margin: 18px 0 9px; line-height: 1.4; }
                    h1 { font-size: 22px; } h2 { font-size: 19px; } h3 { font-size: 16px; }
                    strong { font-weight: 700; color: #203026; }
                    .gap { height: 8px; }
                    .empty {
                      margin: 24px 0;
                      padding: 24px;
                      border: 1px dashed #dce3da;
                      border-radius: 14px;
                      color: #929b93;
                      background: #f8faf7;
                      text-align: center;
                    }
                    .bullet { display: flex; gap: 9px; margin: 5px 0; }
                    .bullet > span { color: #63816a; font-weight: 800; }
                    blockquote {
                      margin: 12px 0;
                      padding: 10px 14px;
                      border-left: 4px solid #8ca291;
                      background: #f4f7f2;
                      color: #59645c;
                      border-radius: 0 10px 10px 0;
                    }
                    .inline-code, .latex-fallback {
                      font-family: Consolas, "JetBrains Mono", monospace;
                      background: #f0f3ee;
                      border: 1px solid #e2e7df;
                      border-radius: 6px;
                      padding: 2px 5px;
                      color: #395044;
                    }
                    .code-card {
                      margin: 14px 0 16px;
                      border: 1px solid #dfe5dc;
                      border-radius: 12px;
                      overflow: hidden;
                      background: #f7f8f6;
                    }
                    .code-head {
                      padding: 7px 12px;
                      background: #edf2ec;
                      color: #66756a;
                      font-size: 11px;
                      font-weight: 700;
                      letter-spacing: .5px;
                      border-bottom: 1px solid #dfe5dc;
                    }
                    pre {
                      margin: 0;
                      padding: 15px 16px 18px;
                      overflow-x: auto;
                      white-space: pre;
                      tab-size: 4;
                    }
                    pre code {
                      font-family: Consolas, "JetBrains Mono", "Cascadia Code", monospace;
                      font-size: 13.5px;
                      line-height: 1.62;
                      color: #25332b;
                    }
                    .math-inline {
                      display: inline-flex;
                      align-items: center;
                      vertical-align: middle;
                      margin: 0 2px;
                    }
                    .math-inline img { max-height: 34px; width: auto; vertical-align: middle; }
                    .math-block {
                      margin: 16px 0;
                      padding: 14px 16px;
                      text-align: center;
                      overflow-x: auto;
                      background: #fbfcf9;
                      border: 1px solid #e5e9e2;
                      border-radius: 12px;
                    }
                    .math-block img { max-width: 100%; height: auto; }
                    .latex-fallback.block { display: block; padding: 12px; margin: 10px 0; }
                    .answer-image-row {
                      display: flex;
                      justify-content: center;
                      align-items: flex-start;
                      width: 100%;
                      margin: 18px 0;
                    }
                    .answer-image {
                      margin: 0;
                      padding: 12px;
                      background: #f8faf7;
                      border: 1px solid #e0e6dd;
                      border-radius: 14px;
                      text-align: center;
                      overflow: hidden;
                    }
                    .answer-image img {
                      display: block;
                      margin: 0 auto;
                      border-radius: 8px;
                      box-shadow: 0 4px 16px rgba(48,66,53,.10);
                    }
                    .answer-image figcaption {
                      margin-top: 8px;
                      color: #8a958c;
                      font-size: 11px;
                    }
                    .missing-image {
                      margin: 12px 0;
                      padding: 12px;
                      border: 1px dashed #d8a6a3;
                      border-radius: 10px;
                      color: #a65450;
                      background: #fff7f6;
                    }
                    ::selection { background: #dceadd; }
                  </style>
                </head>
                <body>
                """ + body + """
                </body>
                </html>
                """;
    }

    private static String stripTrailingNewline(String s) {
        if (s == null) return "";
        return s.endsWith("\n") ? s.substring(0, s.length() - 1) : s;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String escapeAttr(String s) {
        return escape(s).replace("'", "&#39;");
    }
}
