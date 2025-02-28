//package com.xzzn.pollux;
//
//import com.xzzn.pollux.entity.es.QADocument;
//import com.xzzn.pollux.service.ESService;
//import com.xzzn.pollux.utils.ESUtils;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
//import org.springframework.data.elasticsearch.core.document.Document;
//import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
//import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
//
//import javax.annotation.Resource;
//import java.util.AbstractList;
//import java.util.Arrays;
//import java.util.List;
//
//@SpringBootTest
//public class ESDaoTest {
//
//    @Resource
//    private ESService esService;
//
//    @Resource
//    private ESUtils esUtils;
//
//    @Resource
//    private ElasticsearchRestTemplate elasticsearchRestTemplate;
//
//    @Test
//    void testCreateIndex() {
//        if(!elasticsearchRestTemplate.indexOps(QADocument.class).exists()) {
//            elasticsearchRestTemplate.indexOps(QADocument.class).create();
//        }
//        IndexCoordinates indexCoordinates = IndexCoordinates.of("test_qa");
//        Document mapping = elasticsearchRestTemplate.indexOps(indexCoordinates).createMapping(QADocument.class);
//        elasticsearchRestTemplate.indexOps(indexCoordinates).create();
//        elasticsearchRestTemplate.indexOps(indexCoordinates).putMapping(mapping);
//    }
//
//    @Test
//    void testAddData(){
//        QADocument qaDocument = QADocument.builder()
//                .id("56")
//                .question("test question")
//                .answer("qaPairWithHighLightIdx.getAnswer()")
//                .highlightIdxList(new AbstractList<QADocument.HighlightIdx>() {
//                    @Override
//                    public QADocument.HighlightIdx get(int index) {
//                        return null;
//                    }
//
//                    @Override
//                    public int size() {
//                        return 0;
//                    }
//                })
//                .fileContentId("fileContentId")
//                .fileId("fileId")
//                .isAllocated(false)
//                .isReview(true)
//                .isModify("false")
//                .build();
//        esUtils.addData("f52f967d3f1d92b4d2e3074a50fd861e_qa_document", qaDocument);
//    }
//
//    @Test
//    void testSearchData() {
//        List<QADocument> allQADocument = esService.getAllQADocument("f52f967d3f1d92b4d2e3074a50fd861e");
//        System.out.println();
//    }
//
//    @Test
//    void testDeleteIndex(){
//        esUtils.deleteIndex("7503c053536576b9b9f69d6a699f2da7_qa_document");
//    }
//
//
//
//    @Test
//    void testGetDataById(){
//        QADocument qaDocument = esUtils.getDataById("6980b4cf3417a53cbfb7c5a30078c5d1_qa_document", "fsPQzowBdxtzGTqASsTh", QADocument.class);
//        System.out.println(qaDocument);
//    }
//
//    @Test
//    void testCount() {
//        System.out.println(esUtils.countIndex("f52f967d3f1d92b4d2e3074a50fd861e_qa_document", new NativeSearchQueryBuilder().build()));
//    }
//
//    @Test
//    void testGetQAByIds(){
//        List<QADocument> qaDocumentByIds = esService.getQADocumentByIds("f52f967d3f1d92b4d2e3074a50fd861e", Arrays.asList("i8NCz4wBdxtzGTqAAMTO", "jcNCz4wBdxtzGTqAAsRT"));System.out.println();
//    }
//
//    @Test
//    void testGetQAByFileId(){
//        List<QADocument> qa = esService.getQAByFile("f52f967d3f1d92b4d2e3074a50fd861e", "589d3382625ef2e3db9926c3f4c77c4c");
//        System.out.println(qa);
//    }
//
//    @Test
//    void testGetQAByFileExcludeId(){
//        List<QADocument> qa = esService.getQAByFileExcludeInfoList("f52f967d3f1d92b4d2e3074a50fd861e", "589d3382625ef2e3db9926c3f4c77c4c", Arrays.asList("i8NCz4wBdxtzGTqAAMTO", "jcNCz4wBdxtzGTqAAsRT"));
//        System.out.println(qa);
//    }
//
//    @Test
//    void testDeleteByIds(){
//        int[] ints = esService.deleteByIds("f52f967d3f1d92b4d2e3074a50fd861e", Arrays.asList("i8NCz4wBdxtzGTqAAMTO", "jcNCz4wBdxtzGTqAAsRT", "55", "56"));
//        System.out.println(ints);
//    }
//
//    @Test
//    void testUpdateQA(){
//        esService.updateQADocument("f52f967d3f1d92b4d2e3074a50fd861e", "nsNCz4wBdxtzGTqANMR7", "test question update", "test answer update", "true");
//    }
//}
