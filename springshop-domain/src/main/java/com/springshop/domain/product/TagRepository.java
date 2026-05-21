package com.springshop.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);

    List<Tag> findByNameIn(List<String> names);

    @Query("SELECT t FROM Tag t ORDER BY t.useCount DESC")
    List<Tag> findTopTags(org.springframework.data.domain.Pageable pageable);
}
