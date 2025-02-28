package com.xzzn.pollux.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.entity.es.FileContent;
import com.xzzn.pollux.entity.es.QADocument;
import com.xzzn.pollux.model.vo.request.qa.QAData;
import com.xzzn.pollux.model.vo.response.task.FileContentQueryResponse;
import com.xzzn.pollux.model.vo.response.task.QAPairPageResponse;
import com.xzzn.pollux.model.vo.response.task.QATaskM2BResponse;
import com.xzzn.pollux.service.impl.FileInfoServiceImpl;
import com.xzzn.pollux.utils.ESUtils;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchScrollHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ESService {

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private ESUtils esUtils;

    @Resource
    private FileInfoServiceImpl fileInfoService;


    @Resource
    private ObjectMapper objectMapper;

    private static final String QA_DOCUMENT_INDEX_SUFFIX = "_qa_document";
    private static final String FILE_CONTENT_INDEX_SUFFIX = "_file_content";

    /**
     * 服务于listener，从消息队列中将QA存储到ES中
     *
     * @param taskId    任务id
     * @param fileId    文件id
     * @param chunkList 分段列表
     * @return QA对数量
     */
    public int saveQAToES(String taskId, String datasetId, String fileId, List<QATaskM2BResponse.Chunk> chunkList) {
        int totalQACount = 0;

        for (QATaskM2BResponse.Chunk chunk : chunkList) {
            FileContent fileContent = FileContent.builder()
                    .content(chunk.getChunkContent())
                    .build();
            String fileContentId = saveFileContent(fileContent, taskId).getId();

            List<QATaskM2BResponse.QAPairWithHighLightIdx> qaPairList = chunk.getQaPairList();

            totalQACount += qaPairList.size();

            for (QATaskM2BResponse.QAPairWithHighLightIdx qaPairWithHighLightIdx : qaPairList) {
                QADocument qaDocument = QADocument.builder()
                        .question(qaPairWithHighLightIdx.getQuestion())
                        .answer(qaPairWithHighLightIdx.getAnswer())
                        .highlightIdxList(qaPairWithHighLightIdx.getHighlightIdxList())
                        .fileContentId(fileContentId)
                        .fileId(fileId)
                        .datasetId(datasetId)
                        .isAllocated(false)
                        .isReview(false)
                        .isModify("false")
                        .build();
                saveQADocument(qaDocument, taskId);
            }
        }
        return totalQACount;
    }

    /**
     *从本地中将QA存储到ES中
     *
     * @param taskId    任务id
     * @return QA对数量
     */
    public int saveQAToESfromlocal(String userId,String taskId, MultipartFile importQAs ,String datasetId,String fileId)  {
        int totalQACount = 0;
        try {
            // 读取 JSON 文件
            List<QAData> qaDataList = objectMapper.readValue(importQAs.getInputStream(),
                    new TypeReference<List<QAData>>() {
            });

            // 遍历 JSON 数据并构建 QADocument 对象
            for (QAData qaData : qaDataList) {
                String reference = qaData.getReference() != null ? qaData.getReference() : "no reference";
                FileContent fileContent = FileContent.builder()
                        .content(reference)
                        .build();
                String fileContentId = saveFileContent(fileContent, taskId).getId();
                int referenceLength = reference.length();
                List<QADocument.HighlightIdx> highlightIdxList = new ArrayList<>();
                if (!reference.isEmpty()) {
                    highlightIdxList = Arrays.asList(
                            QADocument.HighlightIdx.builder()
                                    .start(0)
                                    .end(referenceLength)
                                    .build()
                    );
                }
                List<QAData.QAPair> qaPairList = qaData.getQaPairList() != null ? qaData.getQaPairList() : new ArrayList<>();
                if (qaPairList.isEmpty() && qaData.getQuestion() != null && qaData.getAnswer() != null) {
                    qaPairList = Arrays.asList(
                            QAData.QAPair.builder()
                                    .question(qaData.getQuestion())
                                    .answer(qaData.getAnswer())
                                    .highlightIdxList(highlightIdxList)
                                    .build()
                    );
                }
                for (QAData.QAPair qaPair : qaPairList) {
                    QADocument qaDocument = QADocument.builder()
                            .question(qaPair.getQuestion())
                            .answer(qaPair.getAnswer())
                            .fileContentId(fileContentId)
                            .highlightIdxList(highlightIdxList)
                            .fileId(fileId)
                            .datasetId(datasetId)
                            //.allocatedUserId(userId)
                            .isAllocated(false)
                            .isReview(false)
                            .isModify("false")
                            .build();
                    saveQADocument(qaDocument, taskId);
                    totalQACount++;
                }
            }

        }
            catch (IOException e) {
                e.printStackTrace();
            }
        return totalQACount;
    }



    /**
     * QA Index 是否存在
     *
     * @param taskId 任务id
     * @return 是否存在
     */
    public boolean existQAIndex(String taskId) {
        return esUtils.existIndex(taskId + QA_DOCUMENT_INDEX_SUFFIX);
    }

    public void createQAIndex(String taskId) {
        esUtils.createIndex(taskId + QA_DOCUMENT_INDEX_SUFFIX, QADocument.class);
    }

    /**
     * 删除QA Index
     * @param taskId 任务id
     */
    public void deleteQAIndex(String taskId) {
        esUtils.deleteIndex(taskId + QA_DOCUMENT_INDEX_SUFFIX);
    }

    /**
     * 保存fileContent到ES
     *
     * @param fileContent 文件内容
     * @param taskId      任务id
     * @return 保存后的fileContent
     */
    public FileContent saveFileContent(FileContent fileContent, String taskId) {
        String indexName = taskId + FILE_CONTENT_INDEX_SUFFIX;
        return esUtils.addData(indexName, fileContent);
    }

    /**
     * 保存qaDocument到ES
     *
     * @param qaDocument qaDocument
     * @param taskId     任务id
     * @return 保存后的qaDocument
     */
    public QADocument saveQADocument(QADocument qaDocument, String taskId) {
        String indexName = taskId + QA_DOCUMENT_INDEX_SUFFIX;
        return esUtils.addData(indexName, qaDocument);
    }

    /**
     * 从 es 中获取 QADocument,通过id
     *
     * @param taskId 任务id，用于拼接indexName
     * @param id     id
     * @return QADocument
     */
    public QADocument getQADocumentById(String taskId, String id) {
        String indexName = taskId + QA_DOCUMENT_INDEX_SUFFIX;
        return esUtils.getDataById(indexName, id, QADocument.class);
    }

    /**
     * 从 es 中获取 QADocument
     *
     * @param taskId 任务id，用于拼接indexName
     * @return QADocument
     */
    public List<QADocument> getAllQADocument(String taskId) {
        List<?> dataList = esUtils.getAllData(taskId + QA_DOCUMENT_INDEX_SUFFIX, QADocument.class);
        return (List<QADocument>) dataList;
    }

    /**
     * 从 es 中获取 FileContent
     *
     * @param taskId 任务id，用于拼接indexName
     * @return FileContent
     */
    public List<FileContent> getAllFileContent(String taskId) {
        List<?> dataList = esUtils.getAllData(taskId + FILE_CONTENT_INDEX_SUFFIX, FileContent.class);
        return (List<FileContent>) dataList;
    }

    /**
     * 从es中分页获取QA
     */
    public QAPairPageResponse getQADocumentPage(String taskId, Integer page, Integer pageSize,
                                                List<String> fileIdList, String keyword, Boolean isReview,
                                                String allocateUserId, String score) {
        BoolQueryBuilder boolQuery = buildBoolQuery(fileIdList, keyword, isReview, allocateUserId, score);

        Pageable pageable = PageRequest.of(page, pageSize);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(pageable)
                .build();
        List<QADocument> qaDocumentList = esUtils.searchQA(taskId, query);
        long total = esUtils.countIndex(taskId + QA_DOCUMENT_INDEX_SUFFIX, query);
        return QAPairPageResponse.builder()
                .qaDocumentPage(qaDocumentList)
                .total(total)
                .build();
    }

    private BoolQueryBuilder buildBoolQuery(List<String> fileIdList, String keyword, Boolean isReview,
                                            String allocateUserId, String score) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        addReviewFilter(boolQueryBuilder, isReview);
        addUserIdFilter(boolQueryBuilder, allocateUserId);
        addFileIdListFilter(boolQueryBuilder, fileIdList);
        addKeywordFilter(boolQueryBuilder, keyword);
        addScoreFilter(boolQueryBuilder, score);

        return boolQueryBuilder;
    }


    private void addReviewFilter(BoolQueryBuilder boolQueryBuilder, Boolean isReview) {
        if (isReview != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("is_review", isReview));
        }
    }

    private void addUserIdFilter(BoolQueryBuilder boolQueryBuilder, String userId) {
        if (userId != null && !userId.isEmpty()) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("allocated_user_id", userId));
        }
    }

    private void addFileIdListFilter(BoolQueryBuilder boolQueryBuilder, List<String> fileIdList) {
        if (fileIdList != null && !fileIdList.isEmpty()) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("file_id", fileIdList));
        }
    }

    private void addKeywordFilter(BoolQueryBuilder boolQueryBuilder, String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            BoolQueryBuilder orKeyWordBoolQueryBuilder = QueryBuilders.boolQuery();
            orKeyWordBoolQueryBuilder.should(QueryBuilders.matchQuery("question", keyword).analyzer("ik_max_word"));
            orKeyWordBoolQueryBuilder.should(QueryBuilders.matchQuery("answer", keyword).analyzer("ik_max_word"));
            orKeyWordBoolQueryBuilder.minimumShouldMatch(1);
            boolQueryBuilder.filter(orKeyWordBoolQueryBuilder);
        }
    }

    private void addScoreFilter(BoolQueryBuilder boolQueryBuilder, String score) {
        if (score != null && !score.isEmpty()) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("score.keyword", score));
        }
    }

    /**
     * 获取某个任务的QA数量
     *
     * @param taskId 任务id
     * @return QA数量
     */
    public long getQACountByTaskId(String taskId) {
        return esUtils.countIndex(taskId + QA_DOCUMENT_INDEX_SUFFIX, new NativeSearchQueryBuilder().build());
    }

    /**
     * 通过id-list获取QA
     *
     * @param taskId 任务id
     * @param ids    id列表
     * @return QA列表
     */
    public List<QADocument> getQADocumentByIds(String taskId, List<String> ids) {
        QueryBuilder queryBuilder = QueryBuilders.termsQuery("_id", ids);
        Pageable pageable = PageRequest.of(0, 10000);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .withPageable(pageable)
                .build();
        return esUtils.searchQA(taskId, query);
    }

    /**
     * 通过fileId获取QA
     *
     * @param taskId 任务id
     * @param fileId 文件id
     * @return QA列表
     */
    public List<QADocument> getQAByFile(String taskId, String fileId) {
        QueryBuilder queryBuilder = QueryBuilders.termQuery("file_id", fileId);
        // todo 修改为scroll查询
        Pageable pageable = PageRequest.of(0, 10000);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .withPageable(pageable)
                .build();
        return esUtils.searchQA(taskId, query);
    }

    /**
     * 通过fileId获取未被分配的QA,最大100条
     *
     * @param taskId 任务id
     * @param fileId 文件id
     * @return QA列表
     */
    public List<QADocument> getUnallocatedQAByFile(String taskId, String fileId, int leftNum) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("file_id", fileId))
                .filter(QueryBuilders.termQuery("is_allocated", false));
        Pageable pageable = PageRequest.of(0, leftNum);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageable)
                .build();
        return esUtils.searchQA(taskId, query);
    }

    /**
     *
     * @param taskId 任务id
     * @param fileId 文件id
     * @param allocated 是否已经分配
     * @return 是否分配数
     */
    public int getUnallocatedQAByFileCount(String taskId, String fileId,boolean allocated) {
        // 构建查询条件，查找指定 taskId 和 fileId 下 is_allocated 为 false 的 QA 对
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("file_id", fileId))
                .must(QueryBuilders.termQuery("is_allocated", allocated));

        final long scrollTimeout = 60000L; // scroll 保持的时间，单位毫秒
        final int scrollSize = 1000; // 每次获取的文档数量

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .withPageable(PageRequest.of(0, scrollSize))
                .build();

        SearchScrollHits<QADocument> scrollHits = elasticsearchRestTemplate.searchScrollStart(scrollTimeout, searchQuery, QADocument.class, IndexCoordinates.of(taskId + "_qa_document"));

        List<QADocument> unallocatedQADocuments = new ArrayList<>();
        String scrollId = scrollHits.getScrollId();

        while (scrollHits.hasSearchHits()) {
            unallocatedQADocuments.addAll(scrollHits.getSearchHits().stream()
                    .map(hit -> hit.getContent())
                    .collect(Collectors.toList()));

            scrollHits = elasticsearchRestTemplate.searchScrollContinue(scrollId, scrollTimeout, QADocument.class, IndexCoordinates.of(taskId + "_qa_document"));
        }

        elasticsearchRestTemplate.searchScrollClear(Collections.singletonList(scrollId));
        return unallocatedQADocuments.size();
    }

    /**
     * 从 ES 中获取 QADocument,通过子文件id,排除选中选项
     *
     * @param taskId 任务id
     * @param fileId 文件id
     * @param ids    选中的QA的id
     * @return QADocument
     */
    public List<QADocument> getQAByFileExcludeInfoList(String taskId, String fileId, List<String> ids) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("file_id", fileId))
                .mustNot(QueryBuilders.termsQuery("_id", ids));
        Pageable pageable = PageRequest.of(0, 10000);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageable)
                .build();
        return esUtils.searchQA(taskId, query);
    }

    /**
     * 通过ids删除QA对
     *
     * @param taskId 任务id
     * @param ids    id列表
     * @return QA对应数量
     */
    public int[] deleteByIds(String taskId, List<String> ids) {
        QueryBuilder queryBuilder = QueryBuilders.termsQuery("_id", ids);
        NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(queryBuilder).build();

        // 查询其中审核过的QA数量
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termsQuery("_id", ids))
                .must(QueryBuilders.termQuery("is_review", true));
        NativeSearchQuery reviewQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .build();
        int reviewSize = (int) esUtils.countIndex(taskId + QA_DOCUMENT_INDEX_SUFFIX, reviewQuery);

        // 删除
        return new int[]{(int) esUtils.deleteQA(taskId, query).getDeleted(), reviewSize};
    }

    /**
     * 通过集合名删除QA对
     *
     * @param taskId 任务id
     * @param fileId 文件id
     * @return QA对应数量
     */
    public int[] deleteByFileId(String taskId, String fileId) {
        QueryBuilder queryBuilder = QueryBuilders.termQuery("file_id", fileId);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder).build();

        // 查询其中审核过的QA数量
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termsQuery("file_id", fileId))
                .must(QueryBuilders.termQuery("is_review", true));
        NativeSearchQuery reviewQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .build();
        int reviewSize = (int) esUtils.countIndex(taskId + QA_DOCUMENT_INDEX_SUFFIX, reviewQuery);

        // 删除
        return new int[]{(int) esUtils.deleteQA(taskId, query).getDeleted(), reviewSize};
    }

    /**
     * 通过集合名删除QA对
     *
     * @param taskId 任务id
     * @param fileId 文件id
     * @param ids    id列表
     * @return QA对应数量
     */
    public int[] deleteByFileIdExcludeIdList(String taskId, String fileId, List<String> ids) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("file_id", fileId))
                .mustNot(QueryBuilders.termsQuery("_id", ids));
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .build();

        // 查询其中审核过的QA数量
        BoolQueryBuilder reviewQueryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("file_id", fileId))
                .mustNot(QueryBuilders.termsQuery("_id", ids))
                .must(QueryBuilders.termQuery("is_review", true));
        NativeSearchQuery reviewQuery = new NativeSearchQueryBuilder()
                .withQuery(reviewQueryBuilder)
                .build();
        int reviewSize = (int) esUtils.countIndex(taskId + QA_DOCUMENT_INDEX_SUFFIX, reviewQuery);

        // 删除
        return new int[]{(int) esUtils.deleteQA(taskId, query).getDeleted(), reviewSize};
    }

    /**
     * 更新QA对
     *
     * @param taskId   任务id
     * @param id       id
     * @param question 问题
     * @param answer   答案
     * @param isModify 是否修改
     */
    public void updateQADocument(String taskId, String id, String question, String answer, String isModify) {
        QADocument qaDocument = getQADocumentById(taskId, id);
        if (qaDocument == null) {
            throw new BusinessException(500, "审核失败,更新QA到数据库错误");
        }
        qaDocument.setQuestion(question);
        qaDocument.setAnswer(answer);
        qaDocument.setIsModify(isModify);
        esUtils.addData(taskId + QA_DOCUMENT_INDEX_SUFFIX, qaDocument);
    }

    /**
     * 更新QA审核状态
     *
     * @param taskId 任务id
     * @param id     Document的_id
     */
    public boolean updateQAReviewStatus(String taskId, String id) {
        QADocument qaDocument = getQADocumentById(taskId, id);
        if (qaDocument == null) {
            throw new BusinessException(500, "审核失败,更新QA到数据库错误");
        }
        qaDocument.setReview(true);
        esUtils.addData(taskId + QA_DOCUMENT_INDEX_SUFFIX, qaDocument);
        return true;
    }

    /**
     * 更新QA审核状态和分数
     *
     * @param taskId 任务id
     * @param id     Document的_id
     */
    public boolean updateQAReviewStatusAndScore(String taskId, String id, String score) {
        QADocument qaDocument = getQADocumentById(taskId, id);
        if (qaDocument == null) {
            throw new BusinessException(500, "审核失败,更新QA到数据库错误");
        }
        qaDocument.setReview(true);
        qaDocument.setScore(score);
        esUtils.addData(taskId + QA_DOCUMENT_INDEX_SUFFIX, qaDocument);
        return true;
    }


    public void updateQAIsAllocated(String taskId, List<QADocument> qaDocumentList,Document document) {
        for (QADocument qaDocument : qaDocumentList) {
            UpdateQuery updateQuery = UpdateQuery.builder(qaDocument.getId())
                    .withDocument(document)
                    .withRetryOnConflict(5) //冲突重试
                    .build();
            elasticsearchRestTemplate.update(updateQuery, IndexCoordinates.of(taskId + QA_DOCUMENT_INDEX_SUFFIX));
        }
    }

    /**
     * 通过id-list获取file content
     *
     * @param taskId 任务id
     * @param qaId   id列表
     * @return QA列表
     */
    public FileContentQueryResponse getFileContentsByQAId(String taskId, String qaId) {
        QADocument qaDocument = getQADocumentById(taskId, qaId);
        if (qaDocument == null) {
            throw new BusinessException(500, "QA对不存在");
        }

        QueryBuilder queryBuilder = QueryBuilders.termsQuery("_id", qaDocument.getFileContentId());
        Pageable pageable = PageRequest.of(0, 10000);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .withPageable(pageable)
                .build();
        FileContent fileContent = esUtils.searchFileContent(taskId, query);
        String fileId = qaDocument.getFileId();
        String fileName = fileInfoService.getById(fileId).getFileName();
        return FileContentQueryResponse.builder()
                .id(fileContent.getId())
                .content(fileContent.getContent())
                .fileId(fileId)
                .fileName(fileName)
                .build();
    }
}
