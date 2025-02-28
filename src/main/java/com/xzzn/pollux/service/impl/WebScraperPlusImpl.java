package com.xzzn.pollux.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.service.S3Service;
import com.xzzn.pollux.mapper.PageHashMapper;
import com.xzzn.pollux.entity.PageHash;
import com.xzzn.pollux.model.pojo.FileParseRequest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class WebScraperPlusImpl {

    @Value("${qa.task.exchange}")
    private String qaTaskExchange;

    @Resource
    private S3Service s3Service;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private PageHashMapper pageHashMapper;

    private static final int FILES_TO_GENERATE = 1; // 生成文件的数量
    private static final int MIN_CHARACTERS_PER_FILE = 200000; // 每个文件字数不少于 万字

    public void webScraper(String baseFilename) {
        Stack<String> urlsToVisit = new Stack<>();
        Set<String> visitedUrls = new HashSet<>();
        int fileCount = 0; // 记录生成的文件数量

        List<String> urls = Arrays.asList("https://military.china.com/",
                "https://junshi.china.com/wuqi/",
                "https://mil.ifeng.com/",
                "https://www.guancha.cn/",
                "http://mil.m4.cn/"
        );

        String startUrl = getRandomUrl(urls);
        log.info("爬取的网址是: {}", startUrl);
        urlsToVisit.push(startUrl);

        // 用于保存爬取的内容
        StringBuilder contentBuilder = new StringBuilder();
        int totalCharacterCount = 0; // 记录总字数

        while (!urlsToVisit.isEmpty() && fileCount < FILES_TO_GENERATE) {
            String url = urlsToVisit.pop();
            if (url.endsWith("#")) {
                url = url.substring(0, url.length() - 1);
            }
            if (!visitedUrls.contains(url)) {
                visitedUrls.add(url);
            }
            try {
                // 检查是否有该URL的记录
                Optional<PageHash> optionalPageHash = pageHashMapper.findByUrl(url);
                String pageContent = "";

                if (!optionalPageHash.isPresent()) {
                    // 记录不存在，抓取页面内容
                    Document document = Jsoup.connect(url).get();
                    pageContent = document.text();
                    totalCharacterCount += pageContent.length();
                    String contentHash = DigestUtils.md5DigestAsHex(pageContent.getBytes());

                    // 保存内容到 builder 中
                    contentBuilder.append(pageContent).append("\n\n");
                    
                    // 保存新的 URL 记录
                    PageHash newPage = PageHash.builder()
                            .url(url)
                            .contentHash(contentHash)
                            .lastUpdated(LocalDateTime.now())
                            .build();
                    pageHashMapper.insert(newPage);


                } else {
                    log.info("URL已经存在: {}", url);
                }

                // 提取新 URL 加入待爬取队列
                Document document = Jsoup.connect(url).get();
                Elements links = document.select("a[href]");
                for (Element link : links) {
                    String linkHref = link.absUrl("href");
                    if (linkHref.endsWith("#")) {
                        log.info("跳过以 # 结尾的链接: {}", linkHref);
                        continue;
                    }
                    String domain = new URL(startUrl).getHost();
                    if (linkHref.startsWith(startUrl) && !visitedUrls.contains(linkHref)) {
                    //if (linkHref.contains(domain) && !visitedUrls.contains(linkHref)) {
                        urlsToVisit.push(linkHref);
                    }
                }

                // 如果达到每个文件字数限制，保存文件内容并重置计数器
                if (totalCharacterCount >= MIN_CHARACTERS_PER_FILE) {
                    log.info("Generating file {} with {} characters", fileCount + 1, totalCharacterCount);
                    String fileName = baseFilename + "_" + (fileCount + 1) + ".txt";
                    log.info("开始存入minIO，并新建任务");
                    savePageContent(fileName, contentBuilder.toString(), startUrl);
                    fileCount++;
                    contentBuilder.setLength(0);
                    totalCharacterCount = 0; // 重置字数计数
                }
            } catch (IOException e) {
                log.error("未能抓取页面 " + url + ": " + e.getMessage());
            }
        }

        // 爬取结束后保存剩余内容
        if (fileCount < FILES_TO_GENERATE && contentBuilder.length() > 0 && totalCharacterCount >= MIN_CHARACTERS_PER_FILE) {
            String fileName = baseFilename + "_" + (fileCount + 1) + ".txt";
            savePageContent(fileName, contentBuilder.toString(), startUrl);
        }

        log.info("爬取结束，共爬取文件: {}", fileCount);
    }



    private void savePageContent(String fileName, String content, String url) {
        try {
            // 使用文件名创建临时文件
            File tempFile = File.createTempFile(fileName, ".txt");
            String name = url.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
            FileWriter writer = new FileWriter(tempFile);
            writer.write(content);
            writer.close();

            if (s3Service == null) {
                throw new IllegalStateException("s3Service is not initialized");
            }

            String s3Path = s3Service.uploadObject(fileName, "crawler", tempFile, name);
            if (s3Path == null) {
                throw new IOException("Failed to upload file to S3, s3Path is null");
            }
            log.info("Saved " + fileName + " to " + s3Path);

            SendcrawlerfileToMQ(fileName, s3Path);

            // 删除临时文件
            tempFile.delete();
        } catch (IOException e) {
            log.error("Failed to save content for {}", fileName + " {} " + e.getMessage());
        }
    }

    private void SendcrawlerfileToMQ(String fileName, String filePath) {
        FileParseRequest fileParseRequest = FileParseRequest.builder()
                .fileName(fileName)
                .filePath(filePath)
                .build();
        try {
            String message = objectMapper.writeValueAsString(fileParseRequest);
            rabbitTemplate.convertAndSend(qaTaskExchange, "crawler.file.txt", message);
            log.debug("数据集开始解析,发送消息: {}", message);
        } catch (JsonProcessingException e) {
            log.error("数据集解析文件失败,原因: {}", e.getMessage());
        }
    }

    private static String getRandomUrl(List<String> urls) {
        Random random = new Random();
        int index = random.nextInt(urls.size());
        return urls.get(index);
    }
}
