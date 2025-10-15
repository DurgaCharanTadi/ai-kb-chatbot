package com.dct.kb.ai_chat.web;

import com.dct.kb.ai_chat.service.BedrockKbService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class BedrockKbController {

    private final BedrockKbService service;

    // ---------- POST /api/rag ----------
    @PostMapping(path = "/rag", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BedrockKbService.RagResult rag(@RequestBody @Validated RagRequest req) {
        return service.rag(
                req.getQuestion(),
                req.getKnowledgeBaseId(),
                req.getModelArn(),
                req.getMaxResults()
        );
    }

    // ---------- POST /api/retrieve ----------
    @PostMapping(path = "/retrieve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BedrockKbService.RetrieveResult retrieve(@RequestBody @Validated RetrieveRequest req) {
        return service.retrieve(
                req.getQuery(),
                req.getKnowledgeBaseId(),
                req.getMaxResults()
        );
    }

    // ====== Request DTOs ======

    @Data
    public static class RagRequest {
        @NotBlank @Length(max = 8000)
        private String question;

        // Optional; uses defaults from application.yml if absent
        private String knowledgeBaseId;
        private String modelArn;

        // Optional conversational session
        private String sessionId;

        // Optional retrieval top-k (default 5)
        private Integer maxResults;
    }

    @Data
    public static class RetrieveRequest {
        @NotBlank @Length(max = 8000)
        private String query;

        // Optional; uses default from application.yml if absent
        private String knowledgeBaseId;

        // Optional retrieval top-k (default 5)
        private Integer maxResults;
    }
}
