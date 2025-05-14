package com.rawello.maparserver.repository;

import com.rawello.maparserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.favoriteBuildings = :favoriteBuildings WHERE u.id = :userId")
    void updateFavoriteBuildings(@Param("userId") Long userId, @Param("favoriteBuildings") List<Long> favoriteBuildings);
}