package com.teamloci.loci.domain.feedback;

import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.global.infra.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final S3UploadService s3Uploader;

    @Transactional
    public void createFeedback(Long userId, FeedbackRequest request, MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String imageUrl = null;

        if (image != null && !image.isEmpty()) {
            imageUrl = s3Uploader.upload(image, "feedbacks");
        }

        Feedback feedback = Feedback.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(imageUrl)
                .build();

        feedbackRepository.save(feedback);
    }
}