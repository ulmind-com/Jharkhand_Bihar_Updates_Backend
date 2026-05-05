package com.soumyajit.jharkhand_project.service;

import com.soumyajit.jharkhand_project.dto.*;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final StateNewsRepository stateNewsRepository;
    private final EventRepository eventRepository;
    private final JobRepository jobRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;
    private final ImageUploadService imageUploadService;
    private final PropertyRepository propertyRepository;
    private final StateReelRepository stateReelRepository;

    public UserProfileDto getUserProfile(User user) {
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserProfileDto profile = modelMapper.map(currentUser, UserProfileDto.class);
        log.info("Retrieved profile for user: {}", user.getEmail());
        return profile;
    }

    public UserProfileDto updateUserProfile(User user, UpdateUserProfileRequest request) {
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        currentUser.setFirstName(request.getFirstName());
        currentUser.setLastName(request.getLastName());
        currentUser.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(currentUser);

        UserProfileDto updatedProfile = modelMapper.map(savedUser, UserProfileDto.class);
        log.info("Updated profile for user: {} - New name: {} {}",
                user.getEmail(), request.getFirstName(), request.getLastName());

        return updatedProfile;
    }

    @Transactional
    public void updateUserProfileImage(User user, MultipartFile file) {
        String imageUrl = imageUploadService.uploadFile(file);
        user.setProfileImageUrl(imageUrl);
        User updatedUser = userRepository.save(user);
    }

    public UserStatsDto getUserStats(User user) {
        long stateNewsCount = stateNewsRepository.countByAuthor(user);
        long eventsCount = eventRepository.countByAuthor(user);
        long jobsCount = jobRepository.countByAuthor(user);
        long communityPostsCount = communityPostRepository.countByAuthor(user);
        long commentsCount = commentRepository.countByAuthor(user);
        long propertiesCount = propertyRepository.countByAuthor(user);  // ✅ Already here

        UserStatsDto stats = UserStatsDto.builder()
                .totalStateNews(stateNewsCount)
                .totalEvents(eventsCount)
                .totalJobs(jobsCount)
                .totalCommunityPosts(communityPostsCount)
                .totalComments(commentsCount)
                .totalProperties(propertiesCount)  // ✅ ADDED - This was missing!
                .build();

        log.info("Retrieved stats for user: {} - Total content: {}",
                user.getEmail(), stats.getTotalContent());

        return stats;
    }

    public UserContentDto getUserContent(User user) {
        // Get all user's content sorted by creation date (newest first)
        var stateNewsList = stateNewsRepository.findByAuthorOrderByCreatedAtDesc(user);
        var eventsList = eventRepository.findByAuthorOrderByCreatedAtDesc(user);
        var jobsList = jobRepository.findByAuthorOrderByCreatedAtDesc(user);
        var communityPostsList = communityPostRepository.findByAuthorOrderByCreatedAtDesc(user);
        var propertiesList = propertyRepository.findByAuthorOrderByCreatedAtDesc(user);  // ✅ ADDED - Fetch properties

        UserContentDto content = UserContentDto.builder()
                .stateNews(stateNewsList.stream()
                        .map(news -> modelMapper.map(news, StateNewsDto.class))
                        .collect(Collectors.toList()))
                .events(eventsList.stream()
                        .map(event -> modelMapper.map(event, EventDto.class))
                        .collect(Collectors.toList()))
                .jobs(jobsList.stream()
                        .map(job -> modelMapper.map(job, JobDto.class))
                        .collect(Collectors.toList()))
                .communityPosts(communityPostsList.stream()
                        .map(post -> modelMapper.map(post, CommunityPostDto.class))
                        .collect(Collectors.toList()))
                .properties(propertiesList.stream()  // ✅ ADDED - Map properties to DTO
                        .map(property -> modelMapper.map(property, PropertyDto.class))
                        .collect(Collectors.toList()))
                .totalElements(stateNewsList.size() + eventsList.size() +
                        jobsList.size() + communityPostsList.size() + propertiesList.size())  // ✅ ADDED - Include properties count
                .build();

        log.info("Retrieved all content for user: {} - Total items: {}",
                user.getEmail(), content.getTotalElements());
        return content;
    }

    public org.springframework.data.domain.Page<?> getUserPostsByCategory(User user, String category, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        return switch (category) {
            case "stateNews" -> stateNewsRepository.findByAuthorOrderByCreatedAtDesc(user, pageable)
                    .map(news -> modelMapper.map(news, StateNewsDto.class));
            case "events" -> eventRepository.findByAuthorOrderByCreatedAtDesc(user, pageable)
                    .map(event -> modelMapper.map(event, EventDto.class));
            case "jobs" -> jobRepository.findByAuthorOrderByCreatedAtDesc(user, pageable)
                    .map(job -> modelMapper.map(job, JobDto.class));
            case "community" -> communityPostRepository.findByAuthorOrderByCreatedAtDesc(user, pageable)
                    .map(post -> modelMapper.map(post, CommunityPostDto.class));
            case "properties" -> propertyRepository.findByAuthorOrderByCreatedAtDesc(user, pageable)
                    .map(property -> modelMapper.map(property, PropertyDto.class));
            case "reels" -> stateReelRepository.findByAuthorOrderByCreatedAtDesc(user, pageable)
                    .map(reel -> modelMapper.map(reel, com.soumyajit.jharkhand_project.dto.ReelItemDto.class));
            default -> throw new IllegalArgumentException("Invalid category: " + category);
        };
    }

    @Transactional
    public void updateOnesignalPlayerId(User user, String playerId) {
        user.setOnesignalPlayerId(playerId);
        userRepository.save(user);
    }
}
