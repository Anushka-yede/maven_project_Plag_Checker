package com.pc.api.service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pc.api.entity.SubmissionEntity;
import com.pc.api.entity.UserEntity;
import com.pc.api.repository.MatchedSpanRepository;
import com.pc.api.repository.SimilarityResultRepository;
import com.pc.api.repository.SubmissionRepository;
import com.pc.api.repository.UserRepository;
import com.pc.batch.job.SubmissionJob;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

/**
 * Core submission service — handles file ingestion, MinIO storage,
 * SHA-256 hashing, DB persistence, and Spring Batch job triggering.
 */
@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository         submissionRepo;
    private final SimilarityResultRepository   resultRepo;
    private final MatchedSpanRepository        spanRepo;
    private final UserRepository               userRepo;
    private final JobLauncher                  jobLauncher;
    private final SubmissionJob                submissionJobConfig;
    private final MinioClient                  minioClient;
    private final PasswordEncoder              passwordEncoder;

    private final PlagiarismAnalysisService    plagiarismAnalysisService;
    private final AtomicBoolean                bucketChecked = new AtomicBoolean(false);

    @Value("${minio.bucket:submissions}")
    private String minioBucket;

    @Value("${lucene.index-dir:./lucene-index}")
    private String luceneIndexDir;

    public SubmissionService(SubmissionRepository submissionRepo,
                             SimilarityResultRepository resultRepo,
                             MatchedSpanRepository spanRepo,
                             UserRepository userRepo,
                             JobLauncher jobLauncher,
                             SubmissionJob submissionJobConfig,
                             MinioClient minioClient,
                             PasswordEncoder passwordEncoder,
                             PlagiarismAnalysisService plagiarismAnalysisService) {
        this.submissionRepo     = submissionRepo;
        this.resultRepo         = resultRepo;
        this.spanRepo           = spanRepo;
        this.userRepo           = userRepo;
        this.jobLauncher        = jobLauncher;
        this.submissionJobConfig = submissionJobConfig;
        this.minioClient        = minioClient;
        this.passwordEncoder    = passwordEncoder;
        this.plagiarismAnalysisService = plagiarismAnalysisService;
    }

    /**
     * Ingests a file: hash → MinIO → DB → batch job.
     *
     * @param file       uploaded multipart file
     * @param userEmail  authenticated user's email
     * @param assignmentId optional assignment grouping key
     * @return persisted submission entity
     */
    @Transactional
    public SubmissionEntity ingest(MultipartFile file, String userEmail, String assignmentId) throws Exception {
        byte[] bytes = file.getBytes();

        // 1. SHA-256 hash (tamper-proof receipt, Feature 16)
        String sha256 = sha256Hex(bytes);

        // 2. Store in MinIO
        String storageKey = UUID.randomUUID() + "/" + file.getOriginalFilename();
        ensureBucketExists();
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioBucket)
                .object(storageKey)
                .stream(new java.io.ByteArrayInputStream(bytes), bytes.length, -1)
                .contentType(file.getContentType())
                .build());

        // 3. Persist to DB
        UserEntity user = userRepo.findByEmail(userEmail).orElse(null);
        SubmissionEntity entity = new SubmissionEntity();
        entity.setFilename(file.getOriginalFilename());
        entity.setStorageKey(storageKey);
        entity.setSha256Hash(sha256);
        entity.setUserId(user != null ? user.getId() : null);
        entity.setStatus("PENDING");
        entity.setAssignmentId(assignmentId);
        entity = submissionRepo.save(entity);

        // 4. Run Analysis (Replaces Spring Batch for synchronous/reliable scoring)
        final UUID submissionId = entity.getId();
        plagiarismAnalysisService.runAnalysis(entity, bytes);

        return entity;
    }

    /**
     * Verifies tamper-proofing: re-hashes stored file, compares with DB hash.
     */
    public boolean verifyIntegrity(UUID submissionId) throws Exception {
        SubmissionEntity sub = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission not found: " + submissionId));

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(minioBucket).object(sub.getStorageKey()).build())) {
            byte[] bytes  = stream.readAllBytes();
            String rehash = sha256Hex(bytes);
            return rehash.equals(sub.getSha256Hash());
        }
    }

    public Optional<SubmissionEntity> findById(UUID id) {
        return submissionRepo.findById(id);
    }

    public List<SubmissionEntity> findByUser(String email) {
        return userRepo.findByEmail(email)
                .map(u -> submissionRepo.findByUserIdOrderByCreatedAtDesc(u.getId()))
                .orElse(List.of());
    }

    public List<SubmissionEntity> findAll() {
        return submissionRepo.findAll();
    }

    // ──────────────── Helpers ────────────────

    private void launchBatchJob(UUID submissionId, byte[] bytes, String filename) {
        new Thread(() -> {
            try {
                JobParameters params = new JobParametersBuilder()
                        .addString("submissionId", submissionId.toString())
                        .addString("filename", filename)
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters();

                // Put file bytes in a shared cache (in production use Redis/DB)
                BatchJobContextHolder.put(submissionId.toString(), bytes);

                JobExecution execution = jobLauncher.run(
                        submissionJobConfig.submissionProcessingJob(null, null), params);

                String finalStatus = execution.getStatus().name();
                submissionRepo.findById(submissionId).ifPresent(s -> {
                    s.setStatus("COMPLETED".equals(finalStatus) ? "DONE" : "FAILED");
                    submissionRepo.save(s);
                });
            } catch (Exception e) {
                log.error("Batch job failed for submission {}: {}", submissionId, e.getMessage());
                submissionRepo.findById(submissionId).ifPresent(s -> {
                    s.setStatus("FAILED");
                    submissionRepo.save(s);
                });
            }
        }, "batch-" + submissionId).start();
    }

    private void ensureBucketExists() throws Exception {
        if (bucketChecked.get()) {
            return;
        }
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioBucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
        }
        bucketChecked.set(true);
    }

    private String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
