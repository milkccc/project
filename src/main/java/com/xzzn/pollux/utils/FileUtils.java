package com.xzzn.pollux.utils;

import com.xzzn.pollux.common.exception.DirectoryCreationException;
import com.xzzn.pollux.common.exception.FileCreationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

@Slf4j
public class FileUtils {

    private FileUtils() {
    }

    public static final String PDF = "pdf";

    public static final String DOC = "doc";

    public static final String DOCX = "docx";

    public static final String CSV = "csv";

    public static final String ZIP = "zip";

    public static final String TXT = "txt";

    public static final String JSON = "json";

    public static final int BUFFER_SIZE = 256;

    public static void unZip(File srcFile, String destDirPath) throws ZipException {
        long start = System.currentTimeMillis();

        log.debug("解压压缩包: {}", srcFile.getName());
        try (ZipFile zipFile = getZipFile(srcFile)) {
            Enumeration<?> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                if (shouldSkipEntry(entry)) {
                    continue;
                }
                log.debug("解压文件: {} ", entry.getName());
                if (entry.isDirectory()) {
                    createDirectory(destDirPath, entry.getName());
                } else {
                    createFileAndCopyContent(zipFile, destDirPath, entry);
                }
            }

            long end = System.currentTimeMillis();
            log.debug("解压完成,耗时：" + (end - start) + " ms");
        } catch (Exception e) {
            throw new ZipException("解压出错");
        }
    }

    private static ZipFile getZipFile(File zipFile) throws IOException {
        ZipFile zip;
        try {
            zip = new ZipFile(zipFile, Charset.forName("GBK"));
            return zip;
        } catch (Exception e) {
            zip = new ZipFile(zipFile, StandardCharsets.UTF_8);
            return zip;
        }
    }


    public static boolean validateExtension(String ext) {
        return !ext.equals(PDF) && !ext.equals(TXT) && !ext.equals(DOC) && !ext.equals(DOCX) && !ext.equals(ZIP) && !ext.equals(CSV) && !ext.equals(JSON);
    }

    private static boolean shouldSkipEntry(ZipEntry entry) {
        return validateExtension(FilenameUtils.getExtension(entry.getName())) && !entry.isDirectory();
    }

    private static void createDirectory(String destDirPath, String entryName) {
        String dirPath = destDirPath + File.separator + entryName;
        File dir = new File(dirPath);
        if (!dir.mkdirs()) {
            throw new DirectoryCreationException("无法创建目录: " + dirPath);
        }
    }

    private static void createFileAndCopyContent(ZipFile zipFile, String destDirPath, ZipEntry entry) throws IOException {
        File targetFile = new File(destDirPath + File.separator + entry.getName());
        createParentDirectories(targetFile);
        createNewFile(targetFile);
        copyContent(zipFile, entry, targetFile);
    }

    private static void copyContent(ZipFile zipFile, ZipEntry entry, File targetFile) throws IOException {
        try (InputStream is = zipFile.getInputStream(entry);
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        }
    }

    private static void createParentDirectories(File file) {
        File parentFile = file.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new DirectoryCreationException("无法创建目录: " + parentFile.getPath());
        }
    }

    private static void createNewFile(File file) throws IOException {
        if (!file.createNewFile()) {
            throw new FileCreationException("无法创建文件: " + file.getPath());
        }
    }


}
