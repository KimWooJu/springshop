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
import java.util.List;

/**
 * 상품 이미지 관리 서비스.
 *
 * <p>이미지 순서 변경, 대표 이미지 설정, 일괄 등록/삭제를 담당한다.
 */
public interface ProductImageService {

    ProductImage addImage(Long productId, ImageCommand command);

    List<ProductImage> addImages(Long productId, List<ImageCommand> commands);

    ProductImage updateImage(Long imageId, ImageCommand command);

    void deleteImage(Long imageId);

    void deleteAllByProduct(Long productId);

    ProductImage setMain(Long productId, Long imageId);

    void reorder(Long productId, List<Long> orderedImageIds);

    List<ProductImage> listByProduct(Long productId);

    record ImageCommand(
        String url,
        String altText,
        Integer displayOrder,
        boolean main
    ) {}

    @Slf4j
    @Service
    @RequiredArgsConstructor
    class Impl implements ProductImageService {

        private static final int MAX_IMAGES_PER_PRODUCT = 20;

        private final ProductImageRepository imageRepository;

        @Override
        @Transactional
        public ProductImage addImage(Long productId, ImageCommand cmd) {
            validate(cmd);
            long current = imageRepository.countByProductId(productId);
            if (current >= MAX_IMAGES_PER_PRODUCT)
                throw new InvalidStateException("상품당 이미지는 최대 " + MAX_IMAGES_PER_PRODUCT + "개");

            int order = cmd.displayOrder() != null ? cmd.displayOrder() : (int) current;
            var image = ProductImage.create(productId, cmd.url(), cmd.altText(), order);
            if (cmd.main() || current == 0) {
                clearMain(productId);
                image.markAsMain();
            }
            var saved = imageRepository.save(image);
            log.debug("이미지 추가: productId={}, imageId={}", productId, saved.getId());
            return saved;
        }

        @Override
        @Transactional
        public List<ProductImage> addImages(Long productId, List<ImageCommand> commands) {
            return commands.stream().map(c -> addImage(productId, c)).toList();
        }

        @Override
        @Transactional
        public ProductImage updateImage(Long imageId, ImageCommand cmd) {
            validate(cmd);
            var image = load(imageId);
            image.updateUrl(cmd.url(), cmd.altText());
            if (cmd.displayOrder() != null) image.changeDisplayOrder(cmd.displayOrder());
            if (cmd.main()) {
                clearMain(image.getProductId());
                image.markAsMain();
            }
            return imageRepository.save(image);
        }

        @Override
        @Transactional
        public void deleteImage(Long imageId) {
            var image = load(imageId);
            var wasMain = image.isMain();
            var productId = image.getProductId();
            imageRepository.delete(image);
            if (wasMain) {
                imageRepository.findAllByProductId(productId).stream()
                    .min(Comparator.comparingInt(ProductImage::getDisplayOrder))
                    .ifPresent(next -> {
                        next.markAsMain();
                        imageRepository.save(next);
                    });
            }
        }

        @Override
        @Transactional
        public void deleteAllByProduct(Long productId) {
            var images = imageRepository.findAllByProductId(productId);
            images.forEach(imageRepository::delete);
            log.info("상품의 모든 이미지 삭제: productId={}, count={}", productId, images.size());
        }

        @Override
        @Transactional
        public ProductImage setMain(Long productId, Long imageId) {
            var image = load(imageId);
            if (!image.getProductId().equals(productId))
                throw new InvalidStateException("상품과 이미지의 소속이 일치하지 않습니다.");
            clearMain(productId);
            image.markAsMain();
            return imageRepository.save(image);
        }

        @Override
        @Transactional
        public void reorder(Long productId, List<Long> orderedImageIds) {
            var images = imageRepository.findAllByProductId(productId);
            var idSet = new java.util.HashSet<>(orderedImageIds);
            if (idSet.size() != images.size()
                || !images.stream().map(ProductImage::getId).allMatch(idSet::contains)) {
                throw new InvalidStateException("순서 변경 대상 이미지 집합이 일치하지 않습니다.");
            }
            for (int i = 0; i < orderedImageIds.size(); i++) {
                final int order = i;
                imageRepository.findById(orderedImageIds.get(i)).ifPresent(img -> {
                    img.changeDisplayOrder(order);
                    imageRepository.save(img);
                });
            }
        }

        @Override
        public List<ProductImage> listByProduct(Long productId) {
            return imageRepository.findAllByProductId(productId).stream()
                .sorted(Comparator.comparingInt(ProductImage::getDisplayOrder))
                .toList();
        }

        // helpers
        private ProductImage load(Long imageId) {
            return imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("이미지를 찾을 수 없습니다: " + imageId));
        }

        private void clearMain(Long productId) {
            imageRepository.findAllByProductId(productId).stream()
                .filter(ProductImage::isMain)
                .forEach(img -> {
                    img.unmarkAsMain();
                    imageRepository.save(img);
                });
        }

        private void validate(ImageCommand cmd) {
            if (cmd.url() == null || cmd.url().isBlank())
                throw new InvalidStateException("이미지 URL은 필수");
        }
    }
}
