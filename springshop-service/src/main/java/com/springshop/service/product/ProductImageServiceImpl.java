package com.springshop.service.product;

import com.springshop.domain.product.ProductImage;
import com.springshop.domain.product.ProductImageRepository;
import com.springshop.domain.common.exception.InvalidStateException;
import com.springshop.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * 상품 이미지 관리 보조 구현체.
 *
 * <p>기본 CRUD는 {@link ProductImageService.Impl}이 담당. 본 구현체는 다음 역할:
 * <ul>
 *   <li>이미지 개수 제한 검증 (최대 10개)</li>
 *   <li>대표 이미지 설정/해제 (단일 보장)</li>
 *   <li>이미지 순서 변경 (드래그앤드롭 지원)</li>
 *   <li>이미지 일괄 삭제 / 일괄 등록</li>
 * </ul>
 */
@Slf4j
@Service("productImageServiceExtension")
@RequiredArgsConstructor
@Transactional
public class ProductImageServiceImpl {

    /**
     * 상품당 등록 가능한 최대 이미지 수.
     */
    public static final int MAX_IMAGES_PER_PRODUCT = 10;

    /**
     * 허용되는 이미지 확장자.
     */
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
        ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    private final ProductImageRepository imageRepository;

    /**
     * 이미지 추가.
     */
    public ProductImage addImage(Long productId, String url, String altText, boolean asMain) {
        validateUrl(url);
        long current = imageRepository.countByProductId(productId);
        if (current >= MAX_IMAGES_PER_PRODUCT) {
            throw new InvalidStateException(
                "상품당 이미지 한도 초과: 최대 " + MAX_IMAGES_PER_PRODUCT + "개");
        }
        var image = ProductImage.create(productId, url, altText, (int) current);
        if (asMain || current == 0) {
            unsetMainOnAll(productId);
            image.markAsMain();
        }
        var saved = imageRepository.save(image);
        log.info("이미지 추가: productId={}, imageId={}, main={}",
            productId, saved.getId(), saved.isMain());
        return saved;
    }

    /**
     * 이미지 일괄 추가.
     */
    public List<ProductImage> addImages(Long productId, List<String> urls) {
        if (urls == null || urls.isEmpty()) return List.of();
        long current = imageRepository.countByProductId(productId);
        if (current + urls.size() > MAX_IMAGES_PER_PRODUCT) {
            throw new InvalidStateException(
                "이미지 일괄 추가 시 한도 초과: " + (current + urls.size())
                    + " > " + MAX_IMAGES_PER_PRODUCT);
        }
        var saved = new java.util.ArrayList<ProductImage>();
        for (int i = 0; i < urls.size(); i++) {
            saved.add(addImage(productId, urls.get(i), null, i == 0 && current == 0));
        }
        return saved;
    }

    /**
     * 이미지 삭제.
     */
    public void deleteImage(Long imageId) {
        var image = loadImage(imageId);
        var productId = image.getProductId();
        boolean wasMain = image.isMain();
        imageRepository.delete(image);
        log.info("이미지 삭제: imageId={}, wasMain={}", imageId, wasMain);
        if (wasMain) {
            promoteNewMain(productId);
        }
    }

    /**
     * 전체 이미지 삭제.
     */
    public void deleteAllByProduct(Long productId) {
        var images = imageRepository.findAllByProductId(productId);
        images.forEach(imageRepository::delete);
        log.info("상품 전체 이미지 삭제: productId={}, count={}", productId, images.size());
    }

    /**
     * 이미지 순서 변경 — 새 순서대로 imageIds 전달.
     */
    public void reorder(Long productId, List<Long> orderedIds) {
        var images = imageRepository.findAllByProductId(productId);
        if (orderedIds == null || orderedIds.size() != images.size()) {
            throw new InvalidStateException("순서 변경 대상 ID 수가 일치하지 않습니다.");
        }
        var idSet = new HashSet<>(orderedIds);
        if (idSet.size() != images.size()) {
            throw new InvalidStateException("중복된 이미지 ID가 포함되어 있습니다.");
        }
        for (var image : images) {
            if (!idSet.contains(image.getId())) {
                throw new InvalidStateException(
                    "현재 이미지가 순서 목록에 없습니다: " + image.getId());
            }
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            var id = orderedIds.get(i);
            final int order = i;
            imageRepository.findById(id).ifPresent(img -> {
                img.changeDisplayOrder(order);
                imageRepository.save(img);
            });
        }
        log.info("이미지 순서 변경 완료: productId={}, count={}", productId, orderedIds.size());
    }

    /**
     * 대표 이미지 지정.
     */
    public ProductImage setMain(Long productId, Long imageId) {
        var image = loadImage(imageId);
        if (!image.getProductId().equals(productId)) {
            throw new InvalidStateException("상품과 이미지의 소속이 일치하지 않습니다.");
        }
        unsetMainOnAll(productId);
        image.markAsMain();
        var saved = imageRepository.save(image);
        log.info("대표 이미지 변경: productId={}, imageId={}", productId, imageId);
        return saved;
    }

    /**
     * 대표 이미지 해제 — 다른 이미지를 자동으로 승격.
     */
    public void unsetMain(Long productId) {
        unsetMainOnAll(productId);
        promoteNewMain(productId);
    }

    /**
     * 상품의 모든 이미지 조회 (순서대로).
     */
    @Transactional(readOnly = true)
    public List<ProductImage> listByProduct(Long productId) {
        return imageRepository.findAllByProductId(productId).stream()
            .sorted(Comparator.comparingInt(ProductImage::getDisplayOrder))
            .toList();
    }

    /**
     * 대표 이미지 조회.
     */
    @Transactional(readOnly = true)
    public Optional<ProductImage> findMain(Long productId) {
        return imageRepository.findAllByProductId(productId).stream()
            .filter(ProductImage::isMain)
            .findFirst();
    }

    /**
     * 상품의 이미지 개수.
     */
    @Transactional(readOnly = true)
    public long countByProduct(Long productId) {
        return imageRepository.countByProductId(productId);
    }

    /**
     * 이미지 단건 조회.
     */
    @Transactional(readOnly = true)
    public ProductImage findById(Long imageId) {
        return loadImage(imageId);
    }

    /**
     * 이미지 URL/alt 업데이트.
     */
    public ProductImage updateImage(Long imageId, String url, String altText) {
        validateUrl(url);
        var image = loadImage(imageId);
        image.updateUrl(url, altText);
        return imageRepository.save(image);
    }

    // ---- helpers ----

    private ProductImage loadImage(Long imageId) {
        return imageRepository.findById(imageId)
            .orElseThrow(() -> new ResourceNotFoundException("이미지를 찾을 수 없습니다: " + imageId));
    }

    private void unsetMainOnAll(Long productId) {
        imageRepository.findAllByProductId(productId).stream()
            .filter(ProductImage::isMain)
            .forEach(img -> {
                img.unmarkAsMain();
                imageRepository.save(img);
            });
    }

    private void promoteNewMain(Long productId) {
        imageRepository.findAllByProductId(productId).stream()
            .min(Comparator.comparingInt(ProductImage::getDisplayOrder))
            .ifPresent(next -> {
                next.markAsMain();
                imageRepository.save(next);
                log.info("대표 이미지 자동 승격: imageId={}", next.getId());
            });
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidStateException("이미지 URL은 필수입니다.");
        }
        var lc = url.toLowerCase();
        boolean validExtension = ALLOWED_EXTENSIONS.stream().anyMatch(lc::endsWith);
        if (!validExtension) {
            throw new InvalidStateException(
                "허용되지 않은 이미지 확장자입니다: " + url
                + " (허용: " + String.join(", ", ALLOWED_EXTENSIONS) + ")");
        }
        if (!lc.startsWith("http://") && !lc.startsWith("https://") && !lc.startsWith("/")) {
            throw new InvalidStateException("이미지 URL은 http/https/절대경로여야 합니다.");
        }
    }
}
