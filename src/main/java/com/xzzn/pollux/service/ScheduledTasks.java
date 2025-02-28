package com.xzzn.pollux.service;

import com.xzzn.pollux.service.impl.WebScraperImpl;
import com.xzzn.pollux.service.impl.WebScraperPlusImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    @Autowired
    private WebScraperImpl webScraperService;
    @Autowired
    private WebScraperPlusImpl webScraperPlusService;

    @Scheduled(cron = "0 0,30 22-23,0-8 * * ?")
    //@Scheduled(cron = "0 51 14 * * ?")
    public void scheduleDailyWebScraping() {
        webScraperPlusService.webScraper("daily_crawl");
    }
}

