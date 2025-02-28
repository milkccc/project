package com.xzzn.pollux.utils;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomMultipartFile implements MultipartFile {

    private String name;
    private String originalFilename;
    private String contentType;
    private byte[] content;

    public CustomMultipartFile(File file) throws IOException {
        this.name = file.getName();
        this.originalFilename = file.getName();
        this.contentType = "text/plain"; // 你可以根据文件类型设置合适的内容类型
        this.content = readFileContent(file);
    }

    private byte[] readFileContent(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    @Override
    public long getSize() {
        return content == null ? 0 : content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (OutputStream outputStream = new FileOutputStream(dest)) {
            outputStream.write(content);
        }
    }
}
