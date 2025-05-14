package com.rawello.maparserver.repository;

import com.rawello.maparserver.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, Long> {
}