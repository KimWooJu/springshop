package com.springshop.web.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Locale;

/**
 * {@link FileUploadConstraint} 검증 구현.
 *
 * <p>파일 크기, 확장자, MIME 타입, 파일명 길이를 검증한다.
 */
public class FileUploadValidator implements ConstraintValidator<FileUploadConstraint, MultipartFile> {

    private long maxSizeBytes;
    private String[] allowedExtensions;
    private String[] allowedMimeTypes;
    private boolean allowEmpty;
    private int maxFilenameLength;

    @Override
    public void initialize(FileUploadConstraint constraint) {
        this.maxSizeBytes = constraint.maxSizeBytes();
        this.allowedExtensions = constraint.allowedExtensions();
        this.allowedMimeTypes = constraint.allowedMimeTypes();
        this.allowEmpty = constraint.allowEmpty();
        this.maxFilenameLength = constraint.maxFilenameLength();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return allowEmpty;
        }
        context.disableDefaultConstraintViolation();

        if (file.getSize() > maxSizeBytes) {
            addViolation(context, "파일 크기가 " + (maxSizeBytes / 1024 / 1024) + "MB를 초과합니다.");
            return false;
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            addViolation(context, "파일명이 비어있습니다.");
            return false;
        }
        if (filename.length() > maxFilenameLength) {
            addViolation(context, "파일명이 너무 깁니다.");
            return false;
        }
        if (containsPathTraversal(filename)) {
            addViolation(context, "파일명에 경로 문자가 포함될 수 없습니다.");
            return false;
        }

        if (allowedExtensions != null && allowedExtensions.length > 0) {
            String ext = extractExtension(filename);
            if (ext == null || !Arrays.asList(allowedExtensions).contains(ext)) {
                addViolation(context, "허용되지 않는 파일 확장자: " + ext);
                return false;
            }
        }

        if (allowedMimeTypes != null && allowedMimeTypes.length > 0) {
            String contentType = file.getContentType();
            if (contentType == null || !Arrays.asList(allowedMimeTypes).contains(contentType)) {
                addViolation(context, "허용되지 않는 MIME 타입: " + contentType);
                return false;
            }
        }

        return true;
    }

    private boolean containsPathTraversal(String filename) {
        return filename.contains("..") || filename.contains("/") || filename.contains("\\");
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return null;
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    /** 외부에서 단일 파일 즉시 검증. */
    public static boolean isAllowedImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String name = file.getOriginalFilename();
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".gif")
                || lower.endsWith(".webp");
    }
}
