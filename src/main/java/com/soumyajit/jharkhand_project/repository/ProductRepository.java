package com.soumyajit.jharkhand_project.repository;

import com.soumyajit.jharkhand_project.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByVendorIdAndIsActiveTrue(Long vendorId);

    @Query("SELECT p FROM Product p WHERE p.vendor.vendorStatus = 'ACCEPTED' AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findAllActiveProductsFromAcceptedVendors();

    @Query("SELECT p FROM Product p WHERE p.vendor.id = :vendorId ORDER BY p.createdAt DESC")
    List<Product> findAllByVendorId(@Param("vendorId") Long vendorId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.vendor.id = :vendorId")
    long countByVendorId(@Param("vendorId") Long vendorId);

    @Query("SELECT p FROM Product p WHERE p.vendor.vendorSlug = :vendorSlug AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findByVendorSlugAndIsActiveTrue(@Param("vendorSlug") String vendorSlug);

    @Query("SELECT p FROM Product p WHERE p.hasDiscount = true AND p.vendor.vendorStatus = 'ACCEPTED' AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findAllProductsWithDiscount();

    Optional<Product> findByIdAndVendorId(Long productId, Long vendorId);

    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :productId")
    void incrementViewCount(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE Product p SET p.cartAddCount = p.cartAddCount + 1 WHERE p.id = :productId")
    void incrementCartAddCount(@Param("productId") Long productId);
}
