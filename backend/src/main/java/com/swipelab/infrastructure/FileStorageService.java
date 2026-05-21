package com.swipelab.infrastructure;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final int URL_TIMEOUT_MS = 15_000;

    private final Path fileStorageLocation;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.lastIndexOf(".") > 0) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    /**
     * Downloads a remote image from {@code imageUrl} and saves it locally so we
     * never rely on the external link remaining alive. The extension is inferred
     * from the response Content-Type; falls back to the URL path, then ".jpg".
     *
     * @throws RuntimeException if the URL is unreachable, returns a non-2xx status,
     *                          or the Content-Type is not an image type.
     */
    public String storeFileFromUrl(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(imageUrl);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(URL_TIMEOUT_MS);
            connection.setReadTimeout(URL_TIMEOUT_MS);
            // Mimic a browser so servers don't block us with 403
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new RuntimeException("Remote URL returned HTTP " + status);
            }

            String contentType = connection.getContentType();
            String extension = extensionFromContentType(contentType, uri.getPath());

            String fileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = fileStorageLocation.resolve(fileName);

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not download image from URL: " + imageUrl, ex);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /** Infers a file extension from Content-Type, falling back to the URL path, then ".jpg". */
    private String extensionFromContentType(String contentType, String urlPath) {
        if (contentType != null) {
            String ct = contentType.toLowerCase();
            if (ct.contains("png"))  return ".png";
            if (ct.contains("gif"))  return ".gif";
            if (ct.contains("webp")) return ".webp";
            if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
            if (ct.contains("bmp"))  return ".bmp";
        }
        // Fallback: try to get extension from the path portion of the URL
        if (urlPath != null) {
            String lower = urlPath.toLowerCase();
            for (String ext : new String[]{".png", ".gif", ".webp", ".jpg", ".jpeg", ".bmp"}) {
                if (lower.endsWith(ext)) return ext.equals(".jpeg") ? ".jpg" : ext;
            }
        }
        return ".jpg";
    }

    /**
     * Loads a stored file as a Resource for streaming. Only resolves paths that
     * were produced by storeFile() / storeFileFromUrl() (i.e. start with "/uploads/").
     */
    public Resource loadFile(String srcPath) {
        try {
            String filename = Paths.get(srcPath).getFileName().toString();
            Path filePath = fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or not readable: " + filename);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Could not load file", ex);
        }
    }
}

