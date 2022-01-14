package com.geekylikes.app.controllers;


import com.geekylikes.app.models.auth.User;
import com.geekylikes.app.models.developer.Developer;
import com.geekylikes.app.models.relationships.ERelationship;
import com.geekylikes.app.models.relationships.Relationship;
import com.geekylikes.app.payload.response.MessageResponse;
import com.geekylikes.app.repositories.DeveloperRepository;
import com.geekylikes.app.repositories.RelationshipRepository;
import com.geekylikes.app.sevices.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.SecondaryTable;
import java.util.Optional;
import java.util.Set;

@CrossOrigin
@RestController
@RequestMapping("/api/relationships")
public class RelationshipController {
    @Autowired
    private RelationshipRepository repository;

    @Autowired
    private UserService userService;

    @Autowired
    private DeveloperRepository developerRepository;

    @PostMapping("/add/{rId}")
    public ResponseEntity<MessageResponse>  addRelationship(@PathVariable Long rId) {
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            return new ResponseEntity<>(new MessageResponse("Invalid User"), HttpStatus.BAD_REQUEST);

        }

        Developer originator = developerRepository.findByUser_id(currentUser.getId()).orElseThrow(()->
            new ResponseStatusException(HttpStatus.NOT_FOUND));

        Developer recipient = developerRepository.findById(rId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));


        Optional<Relationship> rel = repository.findByOriginator_idAndRecipient_id(originator.getId(), recipient.getId());
        if (rel.isPresent()) {
            return new ResponseEntity<>(new MessageResponse("Nice try, be patient"), HttpStatus.OK);
        }

        //check for existing  recipient user relationship.
        //if no relationship create it
        // if pending approve it
        // if approved do nothing
        // if blocked do nothing

        Optional<Relationship> invRel = repository.findByOriginator_idAndRecipient_id(recipient.getId(), originator.getId());

        if (invRel.isPresent()) {
            switch (invRel.get().getType()) {
                case PENDING:
                    invRel.get().setType(ERelationship.ACCEPTED);
                    repository.save(invRel.get());
                    return new ResponseEntity<>(new MessageResponse("Success"), HttpStatus.CREATED);
                case ACCEPTED:
                    return new ResponseEntity<>(new MessageResponse("Your are friends already, stop taxing our system"),HttpStatus.OK);
                case BLOCKED:
                    return new ResponseEntity<>(new MessageResponse("Ok"), HttpStatus.OK);
                default:
                    return new ResponseEntity<>(new MessageResponse("SERVER ERROR _ DEFAULT RELATIONSHIP"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        try {
            repository.save(new Relationship(originator, recipient, ERelationship.PENDING));
        }catch (Exception e) {
            System.out.println("error" + e.getLocalizedMessage());
            return new ResponseEntity<>(new MessageResponse("server error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }


        return new ResponseEntity<>(new MessageResponse("Success"), HttpStatus.CREATED);
    }

    @PostMapping("/block/{rId}")
    public ResponseEntity<MessageResponse> blockRecipient(@PathVariable Long rId) {
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            return new ResponseEntity<>(new MessageResponse("Invalid User"), HttpStatus.BAD_REQUEST);

        }

        Developer originator = developerRepository.findByUser_id(currentUser.getId()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND));

        Developer recipient = developerRepository.findById(rId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<Relationship> rel = repository.findByOriginator_idAndRecipient_id(originator.getId(), recipient.getId());
        if (rel.isPresent()) {
            switch (rel.get().getType()) {
                case PENDING:
                case ACCEPTED:
                    rel.get().setType(ERelationship.BLOCKED);
                    repository.save(rel.get());
                    return new ResponseEntity<>(new MessageResponse("Blocked"), HttpStatus.OK);
                case BLOCKED:
                    return new ResponseEntity<>(new MessageResponse("Blocked"), HttpStatus.OK);
                default:
                    return new ResponseEntity<>(new MessageResponse("SERVER_ERROR: INVALID RELATIONSHIP STATUS"), HttpStatus.INTERNAL_SERVER_ERROR);
            }

        };
        Optional<Relationship> invRel = repository.findByOriginator_idAndRecipient_id(recipient.getId(), originator.getId());
        if (invRel.isPresent()) {
            switch (invRel.get().getType()) {
                case PENDING:
                case ACCEPTED:
                    invRel.get().setType(ERelationship.BLOCKED);
                    repository.save(invRel.get());
                    return new ResponseEntity<>(new MessageResponse("Blocked"), HttpStatus.OK);
                case BLOCKED:
                    return new ResponseEntity<>(new MessageResponse("Blocked"),HttpStatus.OK);
                default:
                    return new ResponseEntity<>(new MessageResponse("SERVER ERROR _ DEFAULT RELATIONSHIP"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }


        //create edge cases
        //if pending changed to block
        //if approve change to bock
        //if blocked do nothing

        //N t exists
        // if pending change to block
        //if approve change to block
        //v if blocked do nothing


        try {
            repository.save(new Relationship(originator, recipient, ERelationship.BLOCKED));


        }catch (Exception e) {
            System.out.println("error" + e.getLocalizedMessage());
            return new ResponseEntity<>(new MessageResponse("server error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new MessageResponse("Success"), HttpStatus.CREATED);

    }

    @PostMapping("/approve/{id}")
    private ResponseEntity<MessageResponse> approvalRelationship(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();

        if(currentUser == null) {
            return new ResponseEntity<>(new MessageResponse("Invalid User"), HttpStatus.BAD_REQUEST);
        }

        Developer recipient = developerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Relationship rel = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (rel.getRecipient().getId() != recipient.getId()) {
            return new ResponseEntity<>(new MessageResponse("Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        if(rel.getType() == ERelationship.PENDING) {
            rel.setType(ERelationship.ACCEPTED);
            repository.save(rel);
        }

        return new ResponseEntity<>(new MessageResponse("Approved"), HttpStatus.OK);

    }


    @DeleteMapping("/remove/{id}")
    private ResponseEntity<MessageResponse> destroyRelationship(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return new ResponseEntity<>(new MessageResponse("Invalid User"), HttpStatus.BAD_REQUEST);

        }

        Developer developer = developerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Relationship rel = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (rel.getOriginator().getId() != developer.getId() && rel.getRecipient().getId() != developer.getId()) {
            new ResponseEntity<>(new MessageResponse("Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        if (rel.getType() != ERelationship.BLOCKED) {
            repository.delete(rel);
        }
        return new ResponseEntity<>(new MessageResponse("Success"), HttpStatus.OK);
    }

    @GetMapping("/friends")
    public ResponseEntity<?> getFriends() {
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            return new ResponseEntity<>(new MessageResponse("Invalid User"), HttpStatus.BAD_REQUEST);

        }

        Developer developer = developerRepository.findByUser_id(currentUser.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Set<Relationship> rels = repository.findAllByOriginator_idAndType(developer.getId(), ERelationship.ACCEPTED);
        Set<Relationship> invRels = repository.findAllByRecipient_idAndType(developer.getId(), ERelationship.ACCEPTED);

        rels.addAll(invRels);
        return new ResponseEntity<>(rels, HttpStatus.OK);

    }









}
