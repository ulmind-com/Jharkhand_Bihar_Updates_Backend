package com.soumyajit.jharkhand_project.repository;

import com.soumyajit.jharkhand_project.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByUserId(Long userId);

    Optional<Vendor> findByVendorSlug(String vendorSlug);

    boolean existsByVendorSlug(String vendorSlug);

    boolean existsByVendorPhone(String vendorPhone);

    List<Vendor> findByVendorStatus(Vendor.VendorStatus vendorStatus);

    @Query("SELECT v FROM Vendor v WHERE v.vendorStatus = 'ACCEPTED' ORDER BY v.createdAt DESC")
    Page<Vendor> findAllAcceptedVendors(Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE v.vendorStatus = 'PENDING' ORDER BY v.createdAt ASC")
    List<Vendor> findAllPendingVendors();

    @Query("SELECT COUNT(v) FROM Vendor v WHERE v.vendorStatus = 'ACCEPTED'")
    long countAcceptedVendors();
}
