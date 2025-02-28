package com.xzzn.pollux.service.impl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.entity.PageHash;
import com.xzzn.pollux.mapper.PageHashMapper;
import com.xzzn.pollux.model.pojo.FileParseRequest;
import com.xzzn.pollux.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;


@Slf4j
@Component
public class WebScraperImpl {
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


    private static final Set<String> visitedUrls = new HashSet<>();
    //private static final Stack<String> urlsToVisit = new Stack<>();
    private static final int MAX_PAGES_TO_VISIT = 100; // 最大爬取页数
    private static final int PAGES_PER_FILE = 20; // 每 个页面合并一个文件

    public void webScraper(String baseFilename) {
        Stack<String> urlsToVisit = new Stack<>();
        visitedUrls.clear();
        int pagesVisited = 0;
        List<String> urls = Arrays.asList("https://military.china.com/",
                "https://junshi.china.com/wuqi/",
                "https://mil.ifeng.com/",
                "https://www.guancha.cn/"
                );

        String startUrl = getRandomUrl(urls);
        log.info("start url: {}", startUrl);
        urlsToVisit.push(startUrl);

        // 用于保存爬取的内容
        StringBuilder contentBuilder = new StringBuilder();
        int pageCounter = 0;

        while (!urlsToVisit.isEmpty() && pagesVisited < MAX_PAGES_TO_VISIT) {
            String url = urlsToVisit.pop();
            if (!visitedUrls.contains(url)) {
                visitedUrls.add(url);
                try {
                    // 抓取页面内容
                    Document document = Jsoup.connect(url).get();
                    String pageContent = document.text();
                    String contentHash = DigestUtils.md5DigestAsHex(pageContent.getBytes());

                    // 检查是否有该URL的记录
                    Optional<PageHash> optionalPageHash = pageHashMapper.findByUrl(url);
                    boolean shouldSaveContent = false;

                    if (optionalPageHash.isPresent()) {
//                        PageHash existingPage = optionalPageHash.get();
//                        if (!existingPage.getContentHash().equals(contentHash)) {
//                            // 内容有更新，保存新内容并更新哈希值
//                            contentBuilder.append(pageContent).append("\n\n");
//                            existingPage.setContentHash(contentHash);
//                            existingPage.setLastUpdated(LocalDateTime.now());
//                            pageHashMapper.updateById(existingPage);
//                            shouldSaveContent = true; // 有更新，应该保存
//                        }
                    } else {
                        // 新的URL，保存记录
                        contentBuilder.append(pageContent).append("\n\n");
                        PageHash newPage = PageHash.builder()
                                .url(url)
                                .contentHash(contentHash)
                                .lastUpdated(LocalDateTime.now())
                                .build();
                        pageHashMapper.insert(newPage);
                        shouldSaveContent = true; // 是新URL，应该保存
                    }

                    // 如果有新内容或更新内容，保存到文件
                    if (shouldSaveContent) {
                        pageCounter++;
                        pagesVisited++;

                        // 如果达到每个文件页面限制，保存文件内容并重置计数器
                        if (pageCounter >= PAGES_PER_FILE || pagesVisited >= MAX_PAGES_TO_VISIT) {
                            log.info("pageCounter:{},pagesVisited:{}",pageCounter,pagesVisited);
                            String fileName = baseFilename + "_" + (pagesVisited / PAGES_PER_FILE) + ".txt";
                            savePageContent(fileName, contentBuilder.toString(), startUrl);
                            pageCounter = 0;
                            contentBuilder.setLength(0);
                        }
                    }

                    // 提取新URL加入待爬取队列
                    Elements links = document.select("a[href]");
                    for (Element link : links) {
                        String linkHref = link.absUrl("href");
                        if (linkHref.startsWith(startUrl) && !visitedUrls.contains(linkHref)) {
                            urlsToVisit.push(linkHref);
                        }
                    }
                } catch (IOException e) {
                    WebScraperImpl.log.error("Failed to fetch " + url + ": " + e.getMessage());
                }
            }
        }

        // 爬取结束后保存剩余内容
        if (contentBuilder.length() > 0) {
            String fileName = baseFilename + "_" + (pagesVisited / PAGES_PER_FILE) + ".txt";
            savePageContent(fileName, contentBuilder.toString(), startUrl);
        }

        log.info("Crawling finished. Total pages visited:{} ", pagesVisited);
    }

    private void savePageContent(String fileName, String content,String url) {
        try {
            // 使用文件名创建临时文件
            File tempFile = File.createTempFile(fileName, ".txt");
            String Name = url.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
            FileWriter writer = new FileWriter(tempFile);
            writer.write(content);
            writer.close();

            if (s3Service == null) {
                throw new IllegalStateException("s3Service is not initialized");
            }

            String s3Path = s3Service.uploadObject(fileName, "crawler", tempFile, Name);
            if (s3Path == null) {
                throw new IOException("Failed to upload file to S3, s3Path is null");
            }
            WebScraperImpl.log.info("Saved " + fileName + " to " + s3Path);

            SendcrawlerfileToMQ(fileName, s3Path);

            // 删除临时文件
            tempFile.delete();
        } catch (IOException e) {
            log.error("Failed to save content for {}" , fileName + " {} " + e.getMessage());
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
            WebScraperImpl.log.error("数据集解析文件失败,原因: {}", e.getMessage());
        }
    }
    private static String getRandomUrl(List<String> urls) {
        Random random = new Random();
        int index = random.nextInt(urls.size());  // 获取随机索引
        return urls.get(index);  // 返回随机选择的URL
    }

    private boolean isValidUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            return (parsedUrl.getProtocol().equals("http") || parsedUrl.getProtocol().equals("https"));
        } catch (MalformedURLException e) {
            return false;
        }
    }

}





