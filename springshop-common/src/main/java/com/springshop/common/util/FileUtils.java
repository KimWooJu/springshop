package com.springshop.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 파일 관련 유틸리티 (확장자, 크기, 저장 경로).
 */
public final class FileUtils {

    public static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    public static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv"
    );

    public static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "jar", "msi"
    );

    private static final long KB = 1024L;
    private static final long MB = 1024L * KB;
    private static final long GB = 1024L * MB;
    private static final long TB = 1024L * GB;

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private FileUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isImageFile(String filename) {
        String ext = getExtension(filename);
        return ext != null && ALLOWED_IMAGE_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    public static boolean isDocumentFile(String filename) {
        String ext = getExtension(filename);
        return ext != null && ALLOWED_DOCUMENT_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    public static boolean isDangerousFile(String filename) {
        String ext = getExtension(filename);
        return ext != null && DANGEROUS_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    /**
     * 확장자 추출 (소문자, 점 제외).
     */
    public static String getExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return null;
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    public static String getBaseName(String filename) {
        if (filename == null) return null;
        // 경로 분리
        int sep = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = sep < 0 ? filename : filename.substring(sep + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    /**
     * 저장 경로 생성. {userId}/{yyyy/MM/dd}/{uuid}.{ext}
     */
    public static String generateStoragePath(String originalName, Long userId) {
        String ext = getExtension(originalName);
        String datePart = LocalDate.now().format(DATE_PATH);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String filename = ext != null ? uuid + "." + ext : uuid;
        return "%d/%s/%s".formatted(userId, datePart, filename);
    }

    public static String generateThumbnailPath(String originalPath) {
        if (originalPath == null) return null;
        int dot = originalPath.lastIndexOf('.');
        if (dot < 0) return originalPath + "_thumb";
        return originalPath.substring(0, dot) + "_thumb" + originalPath.substring(dot);
    }

    /**
     * 사람이 읽기 좋은 사이즈 표시. "1.5 MB" 등.
     */
    public static String humanReadableSize(long bytes) {
        if (bytes < KB) return bytes + " B";
        if (bytes < MB) return "%.1f KB".formatted(bytes / (double) KB);
        if (bytes < GB) return "%.1f MB".formatted(bytes / (double) MB);
        if (bytes < TB) return "%.2f GB".formatted(bytes / (double) GB);
        return "%.2f TB".formatted(bytes / (double) TB);
    }

    public static boolean isFileSizeAllowed(long bytes, int maxMB) {
        if (bytes <= 0) return false;
        return bytes <= (long) maxMB * MB;
    }

    public static boolean isImageSizeAllowed(long bytes) {
        return isFileSizeAllowed(bytes, 10); // 10MB
    }

    /**
     * 파일명 정제: 특수문자 제거, 안전한 문자만 남김.
     */
    public static String sanitizeFilename(String name) {
        if (name == null) return null;
        // 경로 구분자 제거
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        // 연속 점 제거
        cleaned = cleaned.replaceAll("\\.+", ".");
        // 앞뒤 공백/점 제거
        cleaned = cleaned.trim();
        if (cleaned.startsWith(".")) cleaned = cleaned.substring(1);
        if (cleaned.isEmpty()) return "unnamed";
        // 길이 제한 (확장자 보존)
        if (cleaned.length() > 200) {
            String ext = getExtension(cleaned);
            String base = getBaseName(cleaned);
            base = base.substring(0, Math.min(base.length(), 195));
            return ext != null ? base + "." + ext : base;
        }
        return cleaned;
    }

    public static String getContentType(String filename) {
        String ext = getExtension(filename);
        if (ext == null) return "application/octet-stream";
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html" -> "text/html";
            default -> "application/octet-stream";
        };
    }
}
