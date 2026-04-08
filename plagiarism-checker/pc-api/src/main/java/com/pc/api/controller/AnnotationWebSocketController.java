package com.pc.api.controller;

import com.pc.api.entity.AnnotationEntity;
import com.pc.api.repository.AnnotationRepository;
import com.pc.api.repository.UserRepository;
import com.pc.api.entity.UserEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

/**
 * Feature 18 — Live Collaborative Review via WebSocket + STOMP.
 *
 * <p>Reviewers send annotations to {@code /app/annotations/{resultId}}.
 * The broker broadcasts to {@code /topic/annotations/{resultId}} so all
 * connected clients see updates in real time without a page refresh.
 */
@Controller
public class AnnotationWebSocketController {

    private final SimpMessagingTemplate messaging;
    private final AnnotationRepository  annotationRepo;
    private final UserRepository        userRepo;

    public AnnotationWebSocketController(SimpMessagingTemplate messaging,
                                         AnnotationRepository annotationRepo,
                                         UserRepository userRepo) {
        this.messaging      = messaging;
        this.annotationRepo = annotationRepo;
        this.userRepo       = userRepo;
    }

    /**
     * Receives an annotation from a reviewer via STOMP.
     * Persists it and broadcasts to all subscribers of that result's topic.
     */
    @MessageMapping("/annotations/{resultId}")
    public void handleAnnotation(@DestinationVariable String resultId,
                                  AnnotationMessage msg,
                                  java.security.Principal principal) {
        AnnotationEntity ann = new AnnotationEntity();
        ann.setResultId(java.util.UUID.fromString(resultId));
        ann.setVerdict(msg.verdict());
        ann.setNote(msg.note());
        ann.setSpanStart(msg.spanStart());

        if (principal != null) {
            userRepo.findByEmail(principal.getName())
                    .map(UserEntity::getId)
                    .ifPresent(ann::setReviewerId);
        }

        annotationRepo.save(ann);

        // Broadcast to all subscribers
        messaging.convertAndSend("/topic/annotations/" + resultId, Map.of(
                "resultId",  resultId,
                "verdict",   ann.getVerdict(),
                "note",      ann.getNote() != null ? ann.getNote() : "",
                "spanStart", ann.getSpanStart(),
                "reviewer",  principal != null ? principal.getName() : "anonymous",
                "timestamp", Instant.now().toString()
        ));
    }

    public record AnnotationMessage(String verdict, String note, int spanStart) {}
}
