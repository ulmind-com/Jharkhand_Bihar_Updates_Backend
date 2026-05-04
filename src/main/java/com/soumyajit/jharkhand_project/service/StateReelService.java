package com.soumyajit.jharkhand_project.service;

import com.soumyajit.jharkhand_project.dto.CreateReelRequest;
import com.soumyajit.jharkhand_project.dto.ReelItemDto;
import com.soumyajit.jharkhand_project.entity.State;
import com.soumyajit.jharkhand_project.entity.StateReel;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.repository.StateReelRepository;
import com.soumyajit.jharkhand_project.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Service for admin-uploaded reels. Handles video upload to Cloudinary
 * and CRUD operations on the state_reels table.
 * Fully isolated — does not interact with StateNewsService at all.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StateReelService {

    private final StateReelRepository stateReelRepository;
    private final StateRepository stateRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Admin uploads a reel (video file + title + state).
     */
    public ReelItemDto createReel(CreateReelRequest request, MultipartFile videoFile, User author) {
        // 1. Find the state
        State state = stateRepository.findByName(request.getStateName())
                .orElseThrow(() -> new RuntimeException("State not found: " + request.getStateName()));

        // 2. Upload video to Cloudinary (resource_type=auto handles videos)
        CloudinaryService.CloudinaryUploadResult uploadResult = cloudinaryService.uploadImageWithPublicId(videoFile);

        // 3. Generate thumbnail URL from Cloudinary video
        // Cloudinary auto-generates video thumbnails: replace extension with .jpg
        String thumbnailUrl = uploadResult.getUrl()
                .replaceAll("\\.[^.]+$", ".jpg");

        // 4. Save to DB
        StateReel reel = StateReel.builder()
                .title(request.getTitle())
                .videoUrl(uploadResult.getUrl())
                .thumbnailUrl(thumbnailUrl)
                .cloudinaryPublicId(uploadResult.getPublicId())
                .state(state)
                .author(author)
                .published(true)
                .build();

        reel = stateReelRepository.save(reel);
        log.info("Created reel ID={} for state={} by author={}", reel.getId(), state.getName(), author.getUsername());

        return toDto(reel);
    }

    /**
     * Get DB reels for a state (paginated).
     */
    public Page<ReelItemDto> getReelsByState(String stateName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StateReel> reelPage = stateReelRepository.findByStateNameAndPublishedTrue(stateName, pageable);
        return reelPage.map(this::toDto);
    }

    /**
     * Delete a reel (admin only).
     */
    public void deleteReel(Long id, User user) {
        StateReel reel = stateReelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reel not found: " + id));

        // Delete from Cloudinary
        if (reel.getCloudinaryPublicId() != null) {
            cloudinaryService.deleteImage(reel.getCloudinaryPublicId());
        }

        stateReelRepository.delete(reel);
        log.info("Deleted reel ID={}", id);
    }

    private ReelItemDto toDto(StateReel reel) {
        return ReelItemDto.builder()
                .id(String.valueOf(reel.getId()))
                .title(reel.getTitle())
                .videoUrl(reel.getVideoUrl())
                .thumbnailUrl(reel.getThumbnailUrl())
                .author(reel.getAuthor().getUsername())
                .sourceName("Jharkhand Bihar Updates")
                .publishedAt(reel.getCreatedAt())
                .stateName(reel.getState().getName())
                .type("NATIVE_MP4")
                .build();
    }
}
