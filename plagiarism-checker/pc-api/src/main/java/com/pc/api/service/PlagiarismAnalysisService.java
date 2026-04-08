package com.pc.api.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.pc.ai.AiDetectorService;
import com.pc.api.entity.MatchedSpanEntity;
import com.pc.api.entity.SimilarityResultEntity;
import com.pc.api.entity.SubmissionEntity;
import com.pc.api.repository.MatchedSpanRepository;
import com.pc.api.repository.SimilarityResultRepository;
import com.pc.api.repository.SubmissionRepository;
import com.pc.engine.preprocessing.TextPreprocessor;
import com.pc.engine.similarity.NGramScorer;
import com.pc.parser.DocumentParser;
import com.pc.parser.ParserFactory;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;

@Service
public class PlagiarismAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PlagiarismAnalysisService.class);

    private final SubmissionRepository submissionRepo;
    private final SimilarityResultRepository resultRepo;
    private final MatchedSpanRepository spanRepo;
    private final MinioClient minioClient;
    private final AiDetectorService aiDetectorService;

    private final TextPreprocessor preprocessor;
    private final NGramScorer nGramScorer;

    @Value("${minio.bucket:submissions}")
    private String minioBucket;

    public PlagiarismAnalysisService(SubmissionRepository submissionRepo,
                                     SimilarityResultRepository resultRepo,
                                     MatchedSpanRepository spanRepo,
                                     MinioClient minioClient,
                                     AiDetectorService aiDetectorService) {
        this.submissionRepo = submissionRepo;
        this.resultRepo = resultRepo;
        this.spanRepo = spanRepo;
        this.minioClient = minioClient;
        this.aiDetectorService = aiDetectorService;
        this.preprocessor = new TextPreprocessor();
        this.nGramScorer = new NGramScorer();
    }

    @Async
    public void runAnalysis(SubmissionEntity newSubmission, byte[] fileBytes) {
        try {
            log.info("Starting Plagiarism Analysis for Submission: {}", newSubmission.getId());

            // 1. Parse Document Text
            DocumentParser parser = ParserFactory.forFilename(newSubmission.getFilename());
            String rawText = parser.parse(new ByteArrayInputStream(fileBytes));

            if (rawText == null || rawText.isBlank()) {
                throw new IllegalStateException("Parsed text is empty.");
            }

            // 2. Preprocess
            String cleanedText = preprocessor.cleanText(rawText);

            // 3. Fetch past submissions to compare against (the corpus)
            List<SubmissionEntity> previousSubmissions = submissionRepo.findAll();
            
            double maxSimilarity = 0.0;
            SubmissionEntity mostSimilarDoc = null;

            log.info("Comparing against {} existing documents in the database.", previousSubmissions.size() - 1);

            // 4. Compare vs Database Corpus
            for (SubmissionEntity historicalDoc : previousSubmissions) {
                if (historicalDoc.getId().equals(newSubmission.getId())) continue;

                // Fetch historical doc text from MinIO
                String historicalRawText = fetchTextFromMinio(historicalDoc);
                if (historicalRawText == null) continue;

                String historicalCleanText = preprocessor.cleanText(historicalRawText); 
                
                double score = nGramScorer.calculateSimilarity(cleanedText, historicalCleanText);
                
                if (score > maxSimilarity) {
                    maxSimilarity = score;
                    mostSimilarDoc = historicalDoc;
                }
            }

            // 5. Run AI Authorship Detection 
            double aiProbability = 0.0;
            try {
                com.pc.ai.AiDetectorService.DetectionResult aiResult = aiDetectorService.detect(rawText);
                aiProbability = aiResult.aiProbability();
            } catch(Exception e) {
                log.warn("AI Detection failed, defaulting to 0.0", e);
            }

            // 6. Store Similarity Result
            SimilarityResultEntity result = new SimilarityResultEntity();
            result.setDocAId(newSubmission.getId());
            if (mostSimilarDoc != null) {
                result.setDocBId(mostSimilarDoc.getId());
            }
            result.setFinalScore(BigDecimal.valueOf(maxSimilarity)); // Keep it 0-1 scale like the entity probably expects
            result.setAlgorithm("N-GRAM-JACCARD");
            result.setAiProbability(BigDecimal.valueOf(aiProbability));
            
            resultRepo.save(result);

            if (mostSimilarDoc != null && maxSimilarity > 0) {
                 // Generate a basic matched span reference if found
                 MatchedSpanEntity span = new MatchedSpanEntity();
                 span.setResultId(result.getId());
                 span.setDocId(newSubmission.getId());
                 span.setStartChar(0); // Real diff engine would provide this
                 span.setEndChar((int) (cleanedText.length() * maxSimilarity)); // mock span
                 span.setMatchedText(cleanedText.substring(0, Math.min(cleanedText.length(), span.getEndChar())));
                 spanRepo.save(span);
            }

            // Update status
            newSubmission.setStatus("DONE");
            submissionRepo.save(newSubmission);
            log.info("Analysis completed for {}. Max Similarity: {}%", newSubmission.getId(), maxSimilarity * 100);

        } catch (Exception e) {
            log.error("Analysis Failed for Submission {}", newSubmission.getId(), e);
            newSubmission.setStatus("FAILED");
            submissionRepo.save(newSubmission);
        }
    }

    private String fetchTextFromMinio(SubmissionEntity doc) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(minioBucket).object(doc.getStorageKey()).build())) {
            
            byte[] bytes = stream.readAllBytes();
            DocumentParser parser = ParserFactory.forFilename(doc.getFilename());
            return parser.parse(new ByteArrayInputStream(bytes));
            
        } catch (Exception e) {
            log.warn("Could not fetch or parse historical doc {} from MinIO.", doc.getId());
            return null;
        }
    }
}
