package com.nazran.uploadlargefile.service;

import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPOutputStream;

@Service
public class FileUploadService {

    private static final int CHUNK_SIZE_MB = 10;
    private static final int THREAD_POOL = 10;

    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.port}")
    private int sftpPort;

    @Value("${sftp.username}")
    private String sftpUsername;

    @Value("${sftp.password}")
    private String sftpPassword;

    private final Session sftpSession;
    private final ProgressService progressService;

    public FileUploadService(Session sftpSession, ProgressService progressService) {
        this.sftpSession = sftpSession;
        this.progressService = progressService;
    }

    // Main method to handle the upload request
    public void uploadFile(File file, String remoteDir) throws Exception {
        List<File> compressedChunkFiles = splitAndCompressFile(file);
        uploadFilesInParallel(compressedChunkFiles, remoteDir, file.getName());
    }

    // Split and compress the file into chunks
    private List<File> splitAndCompressFile(File file) throws IOException {
        List<File> chunkFiles = new ArrayList<>();
        byte[] buffer = new byte[getChunkSizeMb() * 1024 * 1024]; // Buffer for chunks
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()); // Timestamp

        try (FileInputStream fis = new FileInputStream(file)) {
            int partCounter = 1;
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                File chunkFile = new File(file.getParent(), file.getName() + "_part" + partCounter++ + "_" + timestamp + ".gz");
                try (FileOutputStream fos = new FileOutputStream(chunkFile);
                     GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                    gzos.write(buffer, 0, bytesRead);
                }
                chunkFiles.add(chunkFile); // Add the compressed chunk to the list
            }
        }
        return chunkFiles;
    }

    // Upload files in parallel using multithreading
    private void uploadFilesInParallel(List<File> chunkFiles, String remoteDir, String originalFileName) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(getFixedThreadPool()); // Thread pool
        List<Future<Void>> futures = new ArrayList<>();
        long totalSize = chunkFiles.stream().mapToLong(File::length).sum(); // Total size of all chunks
        long[] uploadedSize = {0}; // To track the uploaded size

        for (File chunkFile : chunkFiles) {
            Future<Void> future = executor.submit(() -> {
                uploadToSFTP(chunkFile, remoteDir);
                synchronized (uploadedSize) {
                    uploadedSize[0] += chunkFile.length(); // Update progress
                    double progress = (double) uploadedSize[0] / totalSize * 100;
                    progressService.updateProgress(originalFileName, progress); // Update the progress service
                    System.out.printf("Upload progress: %.2f%%%n", progress); // Print progress
                }
                return null;
            });
            futures.add(future);
        }

        // Wait for all threads to complete the upload
        for (Future<Void> future : futures) {
            future.get();  // Wait for completion
        }
        progressService.clearProgress(originalFileName); // Clear progress once done
        executor.shutdown();
    }

    // Upload each compressed file to the SFTP server
    private void uploadToSFTP(File file, String remoteDir) throws JSchException, SftpException, FileNotFoundException {
        Session session = createNewSession();
        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();

        try (FileInputStream fis = new FileInputStream(file)) {
            sftpChannel.put(fis, remoteDir + "/" + file.getName()); // Upload chunk
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            sftpChannel.disconnect();
            session.disconnect(); // Disconnect the session
        }
    }

    private Session createNewSession() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(sftpUsername, sftpHost, sftpPort);
        session.setPassword(sftpPassword);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        return session;
    }

    private int getChunkSizeMb() {

        return CHUNK_SIZE_MB;
    }

    private int getFixedThreadPool() {

        return THREAD_POOL;
    }

    public void calculation(int fileSizeMB) {

//        int fileSizeMB = 400;            // A2
        int perFileChunkMB = 10;         // B2
        int maxLimitMB = 25;             // C2
        int maxThread = 15;              // E2

        // Calculate part count
        int partCount = calculatePartCount(fileSizeMB, perFileChunkMB, maxLimitMB);
        System.out.println("Part Count: " + partCount);

        // Calculate parallel thread
        int parallelThread = calculateParallelThread(partCount, maxThread);
        System.out.println("Parallel Thread: " + parallelThread);
    }

    // Method to calculate part count based on formula: IF(A2/B2 > C2, A2/C2, A2/B2)
    public static int calculatePartCount(int fileSizeMB, int perFileChunkMB, int maxLimitMB) {
        int partCount;
        if (fileSizeMB / perFileChunkMB > maxLimitMB) {
            partCount = fileSizeMB / maxLimitMB;
        } else {
            partCount = fileSizeMB / perFileChunkMB;
        }
        return partCount;
    }

    // Method to calculate parallel thread based on formula: IF(D2 < E2, D2, E2)
    public static int calculateParallelThread(int partCount, int maxThread) {
        return Math.min(partCount, maxThread);
    }
}