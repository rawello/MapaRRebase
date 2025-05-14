package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.Building;
import com.rawello.maparserver.model.Node;
import com.rawello.maparserver.model.User;
import com.rawello.maparserver.repository.BuildingRepository;
import com.rawello.maparserver.repository.NodeRepository;
import com.rawello.maparserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping
    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/{id}/nodes")
    public ResponseEntity<List<Node>> getNodesForBuilding(@PathVariable Long id) {
        Optional<Building> building = buildingRepository.findById(id);
        if (building.isPresent()) {
            List<Node> nodes = nodeRepository.findByBuildingId(id);
            return ResponseEntity.ok(nodes);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping
    public ResponseEntity<Building> createBuilding(@RequestBody Building building) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || Objects.equals(authentication.getName(), "anonymous")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername());
        building.setUser(user);

        return ResponseEntity.ok(buildingRepository.save(building));
    }
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/{id}")
    public ResponseEntity<Building> getBuildingById(@PathVariable Long id) {
        Optional<Building> building = buildingRepository.findById(id);
        if (building.isPresent() && building.get().getUser().getId().equals(getCurrentUserId())) {
            return ResponseEntity.ok(building.get());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            return userRepository.findByUsername(userDetails.getUsername()).getId();
        } else if (principal instanceof String) {
            return userRepository.findByUsername((String) principal).getId();
        } else {
            throw new AccessDeniedException("Invalid principal type");
        }
    }

    @GetMapping("/{id}/is-owner")
    public ResponseEntity<Boolean> isBuildingOwner(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(false);
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername());

        Optional<Building> building = buildingRepository.findById(id);
        boolean isOwner = building.isPresent() && building.get().getUser().getId().equals(user.getId());
        return ResponseEntity.ok(isOwner);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Building> updateBuilding(@PathVariable Long id, @RequestBody Building building) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || Objects.equals(authentication.getName(), "anonymous")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Building> existingBuilding = buildingRepository.findById(id);
        if (existingBuilding.isPresent()) {
            Building updatedBuilding = existingBuilding.get();
            updatedBuilding.setName(building.getName());
            updatedBuilding.setAddress(building.getAddress());
            return ResponseEntity.ok(buildingRepository.save(updatedBuilding));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}