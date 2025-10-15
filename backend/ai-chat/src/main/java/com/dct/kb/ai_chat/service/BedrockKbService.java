package com.dct.kb.ai_chat.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.Citation;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseQuery;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseRetrievalConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseRetrievalResult;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseRetrieveAndGenerateConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievedReference;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateType;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievalResultLocation;
// REMOVED: import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievalResultS3Location;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievalResultWebLocation;
// keep this for /retrieve (works in your SDK)
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseVectorSearchConfiguration;

@Service
@RequiredArgsConstructor
public class BedrockKbService {

    private final BedrockAgentRuntimeClient agentClient;

    @Value("${app.aws.defaultKnowledgeBaseId:}")
    private String defaultKbId;

    @Value("${app.aws.defaultModelArn:}")
    private String defaultModelArn;

    // ---------- Retrieve+Generate (RAG) ----------
    public RagResult rag(String question, String kbId, String modelArn, Integer maxResults) {
        String effectiveKbId = isBlank(kbId) ? defaultKbId : kbId;
        String effectiveModelArn = isBlank(modelArn) ? defaultModelArn : modelArn;

        if (isBlank(effectiveKbId)) {
            throw new IllegalArgumentException("knowledgeBaseId is required (configure default or pass in body)");
        }
        if (isBlank(effectiveModelArn)) {
            throw new IllegalArgumentException("modelArn is required (configure default or pass in body)");
        }

        RetrieveAndGenerateInput input = RetrieveAndGenerateInput.builder()
                .text(question)
                .build();

        KnowledgeBaseRetrieveAndGenerateConfiguration kbCfg =
                KnowledgeBaseRetrieveAndGenerateConfiguration.builder()
                        .knowledgeBaseId(effectiveKbId)
                        .modelArn(effectiveModelArn)
                        .build();

        RetrieveAndGenerateConfiguration ragCfg =
                RetrieveAndGenerateConfiguration.builder()
                        .type(RetrieveAndGenerateType.KNOWLEDGE_BASE)
                        .knowledgeBaseConfiguration(kbCfg)
                        .build();

        // NOTE: In your SDK version, RetrieveAndGenerateRequest.Builder does NOT expose retrievalConfiguration(...)
        RetrieveAndGenerateRequest.Builder req = RetrieveAndGenerateRequest.builder()
                .input(input)
                .retrieveAndGenerateConfiguration(ragCfg);

//        if (!isBlank(sessionId)) {
//            req.sessionId(sessionId);
//        }

        RetrieveAndGenerateResponse resp = agentClient.retrieveAndGenerate(req.build());

        String answer = (resp.output() != null && resp.output().text() != null)
                ? resp.output().text()
                : "";

        List<CitationInfo> citations = resp.citations() == null ? List.of()
                : resp.citations().stream().map(this::toCitationInfo).collect(Collectors.toList());

        return RagResult.builder()
                .answer(answer)
                .citations(citations)
                .sessionId(resp.sessionId())
                .build();
    }

    // ---------- Retrieve only ----------
    public RetrieveResult retrieve(String query, String kbId, Integer maxResults) {
        String effectiveKbId = isBlank(kbId) ? defaultKbId : kbId;
        if (isBlank(effectiveKbId)) {
            throw new IllegalArgumentException("knowledgeBaseId is required (configure default or pass in body)");
        }

        RetrieveRequest req = RetrieveRequest.builder()
                .knowledgeBaseId(effectiveKbId)
                .retrievalQuery(KnowledgeBaseQuery.builder().text(query).build())
                .retrievalConfiguration(KnowledgeBaseRetrievalConfiguration.builder()
                        .vectorSearchConfiguration(KnowledgeBaseVectorSearchConfiguration.builder()
                                .numberOfResults(maxResults == null ? 5 : maxResults)
                                .build())
                        .build())
                .build();

        RetrieveResponse resp = agentClient.retrieve(req);

        List<RetrieveHit> hits = resp.retrievalResults() == null ? List.of()
                : resp.retrievalResults().stream()
                .map(this::toRetrieveHit)
                .collect(Collectors.toList());

        return new RetrieveResult(hits);
    }

    // ====== Mapping helpers ======

    private CitationInfo toCitationInfo(Citation c) {
        String snippet = "";
        if (c.generatedResponsePart() != null
                && c.generatedResponsePart().textResponsePart() != null
                && c.generatedResponsePart().textResponsePart().text() != null) {
            snippet = c.generatedResponsePart().textResponsePart().text();
        }

        List<RetrievedRef> refs = c.retrievedReferences() == null ? List.of()
                : c.retrievedReferences().stream()
                .map(this::toRetrievedRef)
                .collect(Collectors.toList());

        return new CitationInfo(snippet, refs);
    }

    private RetrievedRef toRetrievedRef(software.amazon.awssdk.services.bedrockagentruntime.model.RetrievedReference r) {
        String title = extractMetadataString(r.metadata(), "x-amz-bedrock-kb-doc-title");
        String source = extractLocation(r.location());
        return new RetrievedRef(title, source);
    }

    private RetrieveHit toRetrieveHit(KnowledgeBaseRetrievalResult r) {
        String title = extractMetadataString(r.metadata(), "x-amz-bedrock-kb-doc-title");
        String source = extractLocation(r.location());
        double score = r.score() == null ? 0.0 : r.score();
        String content = (r.content() != null && r.content().text() != null) ? r.content().text() : "";
        return new RetrieveHit(title, source, score, content);
    }

    private String extractMetadataString(Map<String, Document> meta, String key) {
        if (meta == null) return "";
        Document d = meta.get(key);
        if (d == null) return "";
        try {
            if (d.isString()) return d.asString();
            return d.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String extractLocation(RetrievalResultLocation loc) {
        if (loc == null) return "";
        // Your SDK location object reliably exposes web URL; S3 fields vary across versions.
        RetrievalResultWebLocation web = loc.webLocation();
        if (web != null && web.url() != null) {
            return web.url();
        }
        // No stable S3 accessors in your version → omit to avoid compile errors
        return "";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ====== DTOs ======

    @Data @Builder
    public static class RagResult {
        private String answer;
        private List<CitationInfo> citations;
        private String sessionId;
    }

    @Data
    public static class RetrieveResult {
        private final List<RetrieveHit> hits;
    }

    @Data
    public static class CitationInfo {
        private final String snippetFromAnswer;
        private final List<RetrievedRef> references;
    }

    @Data
    public static class RetrievedRef {
        private final String title;
        private final String source;
    }

    @Data
    public static class RetrieveHit {
        private final String title;
        private final String source;
        private final double score;
        private final String contentPreview;
    }
}
