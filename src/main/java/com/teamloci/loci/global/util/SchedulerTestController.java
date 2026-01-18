package com.teamloci.loci.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@RestController
@RequestMapping("/test/scheduler")
@RequiredArgsConstructor
public class SchedulerTestController {

    private final PostScheduler postScheduler;

    @PostMapping("/run")
    public String runSchedulerManually() {
        postScheduler.archiveExpiredPosts();
        return "스케줄러가 수동으로 실행되었습니다. 로그를 확인하세요.";
    }
}