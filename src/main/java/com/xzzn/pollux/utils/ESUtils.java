package com.xzzn.pollux.utils;

import com.xzzn.pollux.entity.es.FileContent;
import com.xzzn.pollux.entity.es.QADocument;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.util.ScrollState;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchScrollHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component("esUtils")
@Slf4j
public class ESUtils {

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    private static final String QA_DOCUMENT_INDEX_SUFFIX = "_qa_document";
    private static final String FILE_CONTENT_INDEX_SUFFIX = "_file_content";

    /**
     * 判断索引是否存在
     *
     * @param indexName 索引名称
     * @return 存在返回true，否则返回false
     */
    public boolean existIndex(String indexName) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        return elasticsearchRestTemplate.indexOps(indexCoordinates).exists();
    }

    /**
     * 创建索引
     *
     * @param indexName 索引名称
     * @param clazz     索引对应的类
     */
    public void createIndex(String indexName, Class<?> clazz) {
        if (existIndex(indexName)) {
            return;
        }
        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        Document mapping = elasticsearchRestTemplate.indexOps(indexCoordinates).createMapping(clazz);
        elasticsearchRestTemplate.indexOps(indexCoordinates).create();
        elasticsearchRestTemplate.indexOps(indexCoordinates).putMapping(mapping);
    }

    /**
     * 删除索引
     * @param indexName 索引名称
     */
    public void deleteIndex(String indexName) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        elasticsearchRestTemplate.indexOps(indexCoordinates).delete();
    }

    /**
     * 添加数据到索引
     *
     * @param indexName 索引名称
     * @param data      数据
     * @param <T>       数据类型
     * @return 返回添加的数据
     */
    public <T> T addData(String indexName, T data) {
        createIndex(indexName, data.getClass());
        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        return elasticsearchRestTemplate.save(data, indexCoordinates);
    }

    /**
     * 获取一个index中的所有数据
     *
     * @param indexName 索引名称
     * @param clazz     索引对应的类
     * @return 返回更新的数据
     */
    public List<?> getAllDataUseQuery(String indexName, Class<?> clazz) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        // todo 修改为scroll查询
        Pageable pageable = PageRequest.of(0, 10000);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .withPageable(pageable)
                .build();
        SearchHits<?> searchHits = elasticsearchRestTemplate.search(query, clazz, indexCoordinates);
        List<?> list = searchHits.get().map(SearchHit::getContent).collect(Collectors.toList());
        return list;
    }

    public <T> List<T> getAllData(String indexName, Class<T> clazz) {
        List<T> allData = new ArrayList<>();
        try {
            IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
            Query query = new NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.matchAllQuery())
                    .withPageable(PageRequest.of(0, 1000)) // 每次获取1000条数据
                    .build();

            long scrollTimeout = 60000L;

            SearchScrollHits<T> searchHits = elasticsearchRestTemplate.searchScrollStart(scrollTimeout, query, clazz, indexCoordinates);

            while (searchHits.hasSearchHits()) {
                searchHits.forEach(searchHit -> allData.add(searchHit.getContent()));
                searchHits = elasticsearchRestTemplate.searchScrollContinue(searchHits.getScrollId(), scrollTimeout, clazz, indexCoordinates);
            }

            elasticsearchRestTemplate.searchScrollClear(Collections.singletonList(searchHits.getScrollId()));

            log.info("Fetched all data from index {}: {}", indexName, allData.size());
        } catch (Exception e) {
            log.error("Error fetching all data from index: {}", indexName, e);
        }

        return allData;
    }

    /**
     * 通过id获取数据
     *
     * @param indexName 索引名称
     * @param id        id
     * @param clazz     索引对应的类
     * @param <T>       数据类型
     * @return 返回更新的数据
     */
    public <T> T getDataById(String indexName, String id, Class<T> clazz) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        return elasticsearchRestTemplate.get(id, clazz, indexCoordinates);
    }

    /**
     * 计算某个索引的数据量
     *
     * @param indexName 索引名称
     * @return 返回索引的数据量
     */
    public long countIndex(String indexName, NativeSearchQuery query) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        return elasticsearchRestTemplate.count(query, indexCoordinates);
    }

    /**
     * 辅助service进行条件查询
     *
     * @param taskId 任务id
     * @param query  查询条件
     * @return 返回查询结果
     */
    public List<QADocument> searchQA(String taskId, NativeSearchQuery query) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(taskId + QA_DOCUMENT_INDEX_SUFFIX);
        SearchHits<QADocument> searchHits = elasticsearchRestTemplate.search(query, QADocument.class, indexCoordinates);
        return searchHits.get().map(SearchHit::getContent).collect(Collectors.toList());
    }

    /**
     * 删除QA
     *
     * @param taskId 任务id
     * @param query  查询条件
     */
    public ByQueryResponse deleteQA(String taskId, NativeSearchQuery query) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(taskId + QA_DOCUMENT_INDEX_SUFFIX);
        return elasticsearchRestTemplate.delete(query, QADocument.class, indexCoordinates);
    }

    /**
     * 辅助service进行条件查询
     *
     * @param taskId 任务id
     * @param query  查询条件
     * @return 返回查询结果
     */
    public FileContent searchFileContent(String taskId, NativeSearchQuery query) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(taskId + FILE_CONTENT_INDEX_SUFFIX);
        SearchHits<FileContent> searchHits = elasticsearchRestTemplate.search(query, FileContent.class, indexCoordinates);
        return searchHits.get().map(SearchHit::getContent).collect(Collectors.toList()).get(0);
    }

}
