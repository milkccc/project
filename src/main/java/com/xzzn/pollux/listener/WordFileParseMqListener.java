package com.xzzn.pollux.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.enums.DatasetStatusEnum;
import com.xzzn.pollux.common.enums.FileStatusEnum;
import com.xzzn.pollux.common.exception.FileProcessingException;
import com.xzzn.pollux.entity.DatasetInfo;
import com.xzzn.pollux.entity.FileInfo;
import com.xzzn.pollux.mapper.DatasetInfoMapper;
import com.xzzn.pollux.model.pojo.FileParseRequest;
import com.xzzn.pollux.service.S3Service;
import com.xzzn.pollux.service.impl.DatasetInfoServiceImpl;
import com.xzzn.pollux.service.impl.FileInfoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.StyleDescription;
import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.usermodel.*;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Slf4j
@Component
public class WordFileParseMqListener {
    @Resource
    private S3Service s3Service;

    @Resource
    private FileInfoServiceImpl fileInfoService;

    @Resource
    private DatasetInfoServiceImpl datasetInfoService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private DatasetInfoMapper datasetInfoMapper;


    @RabbitListener(queues = "file.parse.word.queue", concurrency = "1")
    public void listenWordFileParseQueue(String message) {
        try {
            log.info("队列 file.parse.word.queue收到消息 {}", message);

            FileParseRequest fileParseRequest = objectMapper.readValue(message, FileParseRequest.class);
            String datasetId = fileParseRequest.getDatasetId();
            String fileId = fileParseRequest.getFileId();
            String fileName = fileParseRequest.getFileName();
            String filePath = fileParseRequest.getFilePath();
            log.debug("word文件id {}, 文件路径{}", fileId, filePath);

            parseFile(datasetId, fileId, fileName, filePath);
        }
        catch (FileProcessingException e) {
            log.error("解析Word文件 {} 错误: {}", e.getFileId(), e.getMessage());
            setDatasetAndFileFailed(e.getFileId(), "解析Word文件错误:" + e.getMessage());
        }
        catch (JsonProcessingException e) {
            log.error("监听器转换消息错误: {}", e.getMessage());
        }

}

    private void parseFile(String datasetId, String fileId, String fileName, String filePath) throws FileProcessingException {
        log.info("开始转换docx文件");
        File mdTempFile = null;
        FileInputStream fis = null;
        File tempFile = null;
        Closeable document = null;

        try {
            log.info("处理文件名以确保其合法");
            String safeFileName = "temp_" + System.currentTimeMillis(); // 使用时间戳确保唯一性

            // URL编码处理
            String encodedFilePath = encodeURL(filePath);
            URL url = new URL(encodedFilePath);

            // 下载文件到本地临时文件
            tempFile = File.createTempFile(safeFileName, ".tmp");
            try (InputStream in = url.openStream(); FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // 设置文件扩展名和临时文件
            String mdFileName = fileName + ".md";
            String ext = FilenameUtils.getExtension(fileName);
            mdTempFile = File.createTempFile(safeFileName, ".md");

            fis = new FileInputStream(tempFile);

            String parsedText = "";
            log.info("文件扩展名: {}", ext);

            // 判断文件类型
            if (ext.equalsIgnoreCase("doc")) {
                document = new HWPFDocument(fis);
                parsedText = parseDOCFile((HWPFDocument) document);
            } else if (ext.equalsIgnoreCase("docx")) {
                document = new XWPFDocument(fis);
                XWPFStyles docxStyles = ((XWPFDocument) document).getStyles();
                parsedText = parseDOCXFile((XWPFDocument) document, docxStyles);
            } else {
                throw new IllegalArgumentException("不支持的文件类型: " + ext);
            }
            //log.info("判断文件类型完成, 解析结果: {}", parsedText);

            try (FileWriter writer = new FileWriter(mdTempFile)) {
                writer.write(parsedText);
            }

            String s3Path = s3Service.uploadObject(datasetId, "parse", mdTempFile, mdFileName);
            updateFileAndDatasetInfo(fileId, s3Path);
            log.debug("Word文件{}解析完成", fileName);

        } catch (IOException e) {
            log.error("文件IO异常: {}", e.getMessage());
            throw new FileProcessingException("文件读取失败", fileId);
        } catch (POIXMLException e) {
            log.error("POI处理异常: {}", e.getMessage());
            throw new FileProcessingException("文件解析失败", fileId);
        } catch (Exception e) {
            log.error("未知异常: {}", e.getMessage());
            throw new FileProcessingException("文件解析失败", fileId);
        } finally {
            try {
                if (document != null) {
                    document.close();
                }
                if (fis != null) {
                    fis.close();
                }
                if (mdTempFile != null && mdTempFile.exists()) {
                    mdTempFile.delete();
                }
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            } catch (IOException e) {
                log.error("关闭文件流失败: {}", e.getMessage());
            }
        }
    }

    private String encodeURL(String url) {
        try {
            URL u = new URL(url);
            return new URI(u.getProtocol(), u.getUserInfo(), u.getHost(), u.getPort(), u.getPath(), u.getQuery(), u.getRef()).toASCIIString();
        } catch (Exception e) {
            log.error("URL编码失败: {}", e.getMessage());
            return url;
        }
    }

    private String parseDOCFile(HWPFDocument document) {
        StringBuffer content = new StringBuffer();
        StyleSheet styleSheet = document.getStyleSheet();
        Range range = document.getRange();
        int numParagraphs = range.numParagraphs();
        for (int index=0; index<numParagraphs; index++) {
            Paragraph paragraph = range.getParagraph(index);
            int styleIndex = paragraph.getStyleIndex();
            StyleDescription styleDescription = styleSheet.getStyleDescription(styleIndex);
            if (styleDescription!=null){
                String stylename = styleDescription.getName();
                String parsedText = parseParagraph(paragraph.text(), stylename);
                content.append(parsedText);
            } else {
                content.append(paragraph.text());
            }
            content.append("\n");
        }
        return content.toString();
    }

    private String parseDOCXFile(XWPFDocument document, XWPFStyles styles) {
        StringBuffer content = new StringBuffer();
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (IBodyElement element : document.getBodyElements())  {
            if (element instanceof  XWPFParagraph) {
                XWPFParagraph paragraph = (XWPFParagraph) element;
                XWPFStyle style = styles.getStyle(paragraph.getStyleID());
                if (style != null) {
                    String parsedParagraph = parseParagraph(
                            paragraph.getText(),
                            style.getName()
                    );
                    content.append(parsedParagraph);
                } else {
                    content.append(paragraph.getText());
                }
            } else if (element instanceof XWPFTable) {
                XWPFTable table = (XWPFTable) element;
                StringBuffer tableBuffer = new StringBuffer();
                boolean isfirstline = true;
                for (XWPFTableRow row : table.getRows()) {
                    StringBuffer rowBuffer = new StringBuffer(" | ");
                    for (XWPFTableCell cell : row.getTableCells()) {
                        // 获取单元格的文本内容
                        String cellText = cell.getText().replace("\n", "</br>");
                        rowBuffer.append(cell.getText()).append(" | ");
                    }
                    rowBuffer.append("\n");
                    tableBuffer.append(rowBuffer);
                    if (isfirstline) {
                        StringBuffer splitRow = new StringBuffer(" | ");
                        for (XWPFTableCell cell : row.getTableCells()) {
                            splitRow.append("-----").append(" | ");
                        }
                        splitRow.append("\n");
                        tableBuffer.append(splitRow);
                    }
                    isfirstline = false;
                }
                content.append(tableBuffer);
            }
            content.append("\n\n");
        }
        return content.toString();
    }

    private String parseParagraph(String paragraph, String styleName) {
        if (styleName.contains("heading") || styleName.contains("标题")) {
            // 定义正则表达式，匹配数字
            String regex = "\\d+";

            // 编译正则表达式
            Pattern pattern = Pattern.compile(regex);

            // 创建匹配器对象
            Matcher matcher = pattern.matcher(styleName);
            String number = "0";
            while (matcher.find()){
                number = matcher.group();
                break;
            }
            int num = Math.min(Integer.parseInt(number), 6);
            String prefix = generateHashSequence(num);
            return prefix + " " + paragraph;
        } else if (styleName.contains("List") || styleName.contains("列表")) {
            return "* " + paragraph;
        }
        return paragraph;
    }

    public static String generateHashSequence(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append("#");
        }
        return sb.toString();
    }

    private void setDatasetAndFileFailed(String fileId, String failReason) {
        fileInfoService.lambdaUpdate()
                .eq(FileInfo::getId, fileId)
                .set(FileInfo::getFileStatus, FileStatusEnum.FAILED)
                .set(FileInfo::getFailReason, failReason)
                .update();
        FileInfo fileInfo = fileInfoService.getById(fileId);
        datasetInfoService.lambdaUpdate()
                .eq(DatasetInfo::getId, fileInfo.getDatasetId())
                .set(DatasetInfo::getDatasetStatus, DatasetStatusEnum.ERROR)
                .update();
    }
    @Transactional
    public void updateFileAndDatasetInfo(String fileId, String s3Path) throws FileProcessingException {
        try {
            fileInfoService.lambdaUpdate()
                    .eq(FileInfo::getId, fileId)
                    .set(FileInfo::getParseFilePath, s3Path)
                    .set(FileInfo::getFileStatus, DatasetStatusEnum.SUCCESS)
                    .update();
            FileInfo fileInfo = fileInfoService.getById(fileId);
            String datasetId = fileInfo.getDatasetId();

            datasetInfoMapper.incrementComplete(datasetId);
            datasetInfoMapper.updateStatus(datasetId);
        } catch (Exception e) {
            throw new FileProcessingException("更新文件对应数据库信息失败", fileId);
        }
    }
}
