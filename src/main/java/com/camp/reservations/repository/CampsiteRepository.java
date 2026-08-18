package com.camp.reservations.repository;

import com.camp.reservations.domain.Campsite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampsiteRepository extends JpaRepository<Campsite, Long> {

    List<Campsite> findByActiveTrue();

    List<Campsite> findByOwnerIdOrderByNameAsc(Long ownerId);

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(Long ownerId, String name, Long id);
}
