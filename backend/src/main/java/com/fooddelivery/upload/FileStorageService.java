package com.fooddelivery.upload;

import com.fooddelivery.common.exception.BusinessException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path root;
    private final long maxBytes;

    public FileStorageService(
            @Value("${app.upload.dir:uploads}") String dir,
            @Value("${app.upload.max-mb:5}") long maxMb) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        this.maxBytes = maxMb * 1024 * 1024;
        try {
            Files.createDirectories(root);
            Files.createDirectories(root.resolve("thumbs"));
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được thư mục upload: " + root, e);
        }
    }

    public record StoredFile(String url, String thumbUrl, String filename, long size) {}

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "File rỗng", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException("FILE_TOO_LARGE",
                "Ảnh quá lớn, tối đa " + (maxBytes / 1024 / 1024) + "MB", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("INVALID_IMAGE_TYPE",
                "Chỉ nhận ảnh jpg/png/webp/gif", HttpStatus.BAD_REQUEST);
        }
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = extension(original).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            // Suy ra từ content-type nếu tên file lạ
            ext = switch (contentType) {
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                case "image/gif" -> "gif";
                default -> "jpg";
            };
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dest = root.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("UPLOAD_FAILED",
                "Không lưu được ảnh, thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // Verify ảnh đọc được + sinh thumbnail 480px (fallback: dùng ảnh gốc nếu lỗi)
        String thumbName = "thumb_" + filename;
        try {
            BufferedImage src = ImageIO.read(dest.toFile());
            if (src == null) {
                Files.deleteIfExists(dest);
                throw new BusinessException("INVALID_IMAGE",
                    "File không phải ảnh hợp lệ", HttpStatus.BAD_REQUEST);
            }
            BufferedImage thumb = resize(src, 480);
            Path thumbDest = root.resolve("thumbs").resolve(thumbName);
            ImageIO.write(thumb, ext.equals("png") || ext.equals("gif") ? ext : "jpg", thumbDest.toFile());
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            // Thumbnail lỗi thì vẫn dùng ảnh gốc, không fail upload
            thumbName = null;
        }
        String url = "/uploads/" + filename;
        String thumbUrl = thumbName == null ? url : "/uploads/thumbs/" + thumbName;
        try {
            return new StoredFile(url, thumbUrl, filename, Files.size(dest));
        } catch (IOException e) {
            return new StoredFile(url, thumbUrl, filename, file.getSize());
        }
    }

    private static String extension(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) return "";
        return name.substring(i + 1);
    }

    private static BufferedImage resize(BufferedImage src, int maxWidth) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxWidth) return src;
        int nh = (int) ((double) h / w * maxWidth);
        BufferedImage out = new BufferedImage(maxWidth, nh,
            src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, maxWidth, nh, null);
        g.dispose();
        return out;
    }

    public Path getRoot() { return root; }
}
