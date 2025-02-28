//package com.xzzn.pollux;
//
//import com.github.dockerjava.api.model.Image;
//import com.xzzn.pollux.service.DockerService;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import javax.annotation.Resource;
//
//@SpringBootTest
//@Slf4j
//class DockerServiceTest {
//
//    @Resource
//    private DockerService dockerService;
//
//    @Test
//    void testGetImageList(){
//        for (Image image : dockerService.imageList()) {
//            log.info(image.toString());
//        }
//    }
//}
