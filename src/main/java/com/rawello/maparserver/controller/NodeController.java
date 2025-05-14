package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.Node;
import com.rawello.maparserver.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    @Autowired
    private NodeRepository nodeRepository;

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping
    public List<Node> getAllNodes() {
        return nodeRepository.findAll();
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping
    public ResponseEntity<Node> createNode(@RequestBody Node node) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || Objects.equals(authentication.getName(), "anonymous")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(nodeRepository.save(node));
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || Objects.equals(authentication.getName(), "anonymous")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        nodeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PutMapping("/{id}")
    public ResponseEntity<Node> updateNode(@PathVariable Long id, @RequestBody Node node) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || Objects.equals(authentication.getName(), "anonymous")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Node> existingNode = nodeRepository.findById(id);
        if (existingNode.isPresent()) {
            Node updatedNode = existingNode.get();
            updatedNode.setName(node.getName());
            updatedNode.setTranslation(node.getTranslation());
            updatedNode.setQuaternion(node.getQuaternion());
            updatedNode.setModelPath(node.getModelPath());
            updatedNode.setTitle(node.getTitle());
            updatedNode.setNeighbors(node.getNeighbors());
            nodeRepository.save(updatedNode);
            return new ResponseEntity<>(updatedNode, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}