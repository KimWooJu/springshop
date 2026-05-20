package com.springshop.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findAllByProductId(Long productId);

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    int countByProductId(Long productId);

    void deleteByProductId(Long productId);
}
