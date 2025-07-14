package com.magicalstory.music.utils.text;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本差异比较工具类
 * 实现类似于 diff-match-patch 库的基本功能
 */
public class TextDiffUtils {

    /**
     * 差异类型枚举
     */
    public enum DiffType {
        DELETE(-1), // 删除
        EQUAL(0),   // 相等
        INSERT(1);  // 插入
        
        private final int value;
        
        DiffType(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }

    /**
     * 差异结果类
     */
    public static class Diff {
        public DiffType type;
        public String text;

        public Diff(DiffType type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    /**
     * 比较模式枚举
     */
    public enum CompareMode {
        CHARS,  // 字符对比
        WORDS,  // 词语对比
        LINES   // 行对比
    }

    /**
     * 统计信息类
     */
    public static class DiffStats {
        public int similarity;      // 相似度百分比
        public int added;          // 新增字符数
        public int removed;        // 删除字符数
        public int modified;       // 修改字符数
        public List<String> addedTexts = new ArrayList<>();      // 新增的文本片段
        public List<String> removedTexts = new ArrayList<>();    // 删除的文本片段
    }

    /**
     * 执行文本差异比较
     * @param text1 原始文本
     * @param text2 对比文本
     * @param mode 比较模式
     * @return 差异结果列表
     */
    public static List<Diff> diff(String text1, String text2, CompareMode mode) {
        if (text1 == null) text1 = "";
        if (text2 == null) text2 = "";

        List<Diff> diffs = new ArrayList<>();

        switch (mode) {
            case WORDS:
                diffs = diffWords(text1, text2);
                break;
            case LINES:
                diffs = diffLines(text1, text2);
                break;
            default: // CHARS
                diffs = diffChars(text1, text2);
                break;
        }

        // 清理语义上的差异
        cleanupSemantic(diffs);
        return diffs;
    }

    /**
     * 字符级别的差异比较
     */
    private static List<Diff> diffChars(String text1, String text2) {
        List<Diff> diffs = new ArrayList<>();
        
        // 使用动态规划算法进行字符比较
        int m = text1.length();
        int n = text2.length();
        
        // 创建DP表
        int[][] dp = new int[m + 1][n + 1];
        
        // 填充DP表
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        // 回溯构建差异序列
        int i = m, j = n;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && text1.charAt(i - 1) == text2.charAt(j - 1)) {
                // 字符相同
                i--; j--;
                if (diffs.isEmpty() || diffs.get(0).type != DiffType.EQUAL) {
                    diffs.add(0, new Diff(DiffType.EQUAL, String.valueOf(text1.charAt(i))));
                } else {
                    diffs.get(0).text = text1.charAt(i) + diffs.get(0).text;
                }
            } else if (j > 0 && (i == 0 || dp[i][j - 1] <= dp[i - 1][j])) {
                // 插入
                j--;
                if (diffs.isEmpty() || diffs.get(0).type != DiffType.INSERT) {
                    diffs.add(0, new Diff(DiffType.INSERT, String.valueOf(text2.charAt(j))));
                } else {
                    diffs.get(0).text = text2.charAt(j) + diffs.get(0).text;
                }
            } else if (i > 0) {
                // 删除
                i--;
                if (diffs.isEmpty() || diffs.get(0).type != DiffType.DELETE) {
                    diffs.add(0, new Diff(DiffType.DELETE, String.valueOf(text1.charAt(i))));
                } else {
                    diffs.get(0).text = text1.charAt(i) + diffs.get(0).text;
                }
            }
        }
        
        return diffs;
    }

    /**
     * 词语级别的差异比较
     */
    private static List<Diff> diffWords(String text1, String text2) {
        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");
        
        return diffArrays(words1, words2, " ");
    }

    /**
     * 行级别的差异比较
     */
    private static List<Diff> diffLines(String text1, String text2) {
        String[] lines1 = text1.split("\n");
        String[] lines2 = text2.split("\n");
        
        return diffArrays(lines1, lines2, "\n");
    }

    /**
     * 数组差异比较
     */
    private static List<Diff> diffArrays(String[] array1, String[] array2, String separator) {
        List<Diff> diffs = new ArrayList<>();
        
        // 处理空数组情况
        if (array1.length == 0 && array2.length == 0) {
            return diffs;
        }
        if (array1.length == 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array2.length; i++) {
                if (i > 0) sb.append(separator);
                sb.append(array2[i]);
            }
            diffs.add(new Diff(DiffType.INSERT, sb.toString()));
            return diffs;
        }
        if (array2.length == 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array1.length; i++) {
                if (i > 0) sb.append(separator);
                sb.append(array1[i]);
            }
            diffs.add(new Diff(DiffType.DELETE, sb.toString()));
            return diffs;
        }
        
        int m = array1.length;
        int n = array2.length;
        
        // 使用最长公共子序列算法
        int[][] lcs = new int[m + 1][n + 1];
        
        // 构建LCS表
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (array1[i - 1].equals(array2[j - 1])) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }
        
        // 回溯构建差异
        int i = m, j = n;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && array1[i - 1].equals(array2[j - 1])) {
                // 相同元素
                String text = array1[i - 1];
                if (i < m || j < n) text += separator; // 不是最后一个元素时添加分隔符
                diffs.add(0, new Diff(DiffType.EQUAL, text));
                i--; j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                // 插入
                String text = array2[j - 1];
                if (j > 1 || i > 0) text += separator; // 不是第一个元素时添加分隔符
                diffs.add(0, new Diff(DiffType.INSERT, text));
                j--;
            } else if (i > 0) {
                // 删除
                String text = array1[i - 1];
                if (i > 1 || j > 0) text += separator; // 不是第一个元素时添加分隔符
                diffs.add(0, new Diff(DiffType.DELETE, text));
                i--;
            }
        }
        
        return diffs;
    }

    /**
     * 清理语义上的差异，合并相邻的同类型差异
     */
    private static void cleanupSemantic(List<Diff> diffs) {
        if (diffs.size() <= 1) return;
        
        for (int i = 0; i < diffs.size() - 1; i++) {
            Diff current = diffs.get(i);
            Diff next = diffs.get(i + 1);
            
            if (current.type == next.type) {
                // 合并相同类型的差异
                current.text += next.text;
                diffs.remove(i + 1);
                i--; // 重新检查当前位置
            }
        }
    }

    /**
     * 计算统计信息
     * @param diffs 差异列表
     * @param originalLength 原始文本长度
     * @return 统计信息
     */
    public static DiffStats calculateStats(List<Diff> diffs, int originalLength) {
        DiffStats stats = new DiffStats();
        
        int unchangedChars = 0;
        int totalAdded = 0;
        int totalRemoved = 0;
        
        for (Diff diff : diffs) {
            switch (diff.type) {
                case EQUAL:
                    unchangedChars += diff.text.length();
                    break;
                case INSERT:
                    totalAdded += diff.text.length();
                    String trimmedAdd = diff.text.trim();
                    if (!trimmedAdd.isEmpty()) {
                        stats.addedTexts.add(trimmedAdd);
                    }
                    break;
                case DELETE:
                    totalRemoved += diff.text.length();
                    String trimmedRemove = diff.text.trim();
                    if (!trimmedRemove.isEmpty()) {
                        stats.removedTexts.add(trimmedRemove);
                    }
                    break;
            }
        }
        
        stats.added = totalAdded;
        stats.removed = totalRemoved;
        stats.modified = Math.min(totalAdded, totalRemoved);
        
        // 计算相似度
        int maxLength = Math.max(originalLength, originalLength - totalRemoved + totalAdded);
        if (maxLength > 0) {
            stats.similarity = Math.round((float) unchangedChars / maxLength * 100);
        } else {
            stats.similarity = 100;
        }
        
        return stats;
    }
} 