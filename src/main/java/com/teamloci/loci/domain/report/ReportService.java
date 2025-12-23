package com.teamloci.loci.domain.report;

import com.teamloci.loci.domain.post.entity.PostStatus;
import com.teamloci.loci.domain.post.repository.PostCommentRepository;
import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.domain.post.entity.PostComment;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;

    private static final int BLIND_THRESHOLD = 3;

    @Transactional
    public void createReport(Long reporterId, ReportDto.CreateRequest request) {
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, request.getTargetType(), request.getTargetId())) {
            throw new CustomException(ErrorCode.ALREADY_REPORTED);
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .description(request.getDescription())
                .build();

        reportRepository.save(report);

        checkAndBlindContent(request.getTargetType(), request.getTargetId());
    }

    private void checkAndBlindContent(ReportTarget type, Long targetId) {
        long count = reportRepository.countByTargetTypeAndTargetIdAndStatus(type, targetId, ReportStatus.PENDING);

        if (count >= BLIND_THRESHOLD) {
            log.info("🚨 [자동 블라인드] {} ID: {} (누적 신고: {}회)", type, targetId, count);

            if (type == ReportTarget.POST) {
                postRepository.findById(targetId)
                        .ifPresent(post -> post.changeStatus(PostStatus.BLIND));
            } else if (type == ReportTarget.COMMENT) {
                commentRepository.findById(targetId)
                        .ifPresent(PostComment::blind);
            }
        }
    }
}