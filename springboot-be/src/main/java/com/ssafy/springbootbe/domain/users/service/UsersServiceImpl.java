package com.ssafy.springbootbe.domain.users.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.domain.common.dto.DevPositionInfo;
import com.ssafy.springbootbe.domain.common.dto.TechStackInfo;
import com.ssafy.springbootbe.domain.users.dto.request.UserProfileUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileUpdateResponse;
import com.ssafy.springbootbe.domain.users.exception.UserNotFoundException;
import com.ssafy.springbootbe.persistence.position.entity.DevPosition;
import com.ssafy.springbootbe.persistence.position.repository.DevPositionRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.techstack.repository.TechStackRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserCurriculumCategory;
import com.ssafy.springbootbe.persistence.user.entity.UserDesiredPosition;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserCurriculumCategoryRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserDesiredPositionRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {

    private static final long PROFILE_CACHE_TTL_HOURS = 128L;

    private final UserRepository userRepository;
    private final UserDesiredPositionRepository userDesiredPositionRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final UserCurriculumCategoryRepository userCurriculumCategoryRepository;
    private final DevPositionRepository devPositionRepository;
    private final TechStackRepository techStackRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse findProfile(Long userId) {
        String cacheKey = buildProfileCacheKey(userId);
        String cachedValue = redisService.get(cacheKey);

        if (cachedValue != null) {
            UserProfileResponse cached = deserializeProfile(cachedValue);
            if (cached != null) {
                return cached;
            }
        }

        UserProfileResponse response = buildProfileResponse(userId);
        cacheProfile(cacheKey, response);
        return response;
    }

    @Override
    @Transactional
    public UserProfileUpdateResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = findUserByIdOrThrow(userId);
        user.updateProfile(request.getPosition(), request.getConsiderPersonalSchedule());

        if (request.getDesiredPositionIds() != null) {
            userDesiredPositionRepository.deleteByUserUserId(userId);
            List<DevPosition> devPositions = devPositionRepository.findAllById(request.getDesiredPositionIds());
            List<UserDesiredPosition> newDesiredPositions = devPositions.stream()
                    .map(dp -> UserDesiredPosition.builder().user(user).devPosition(dp).build())
                    .toList();
            userDesiredPositionRepository.saveAll(newDesiredPositions);
        }

        if (request.getTechStackIds() != null) {
            userTechStackRepository.deleteByUserUserId(userId);
            List<TechStack> techStacks = techStackRepository.findAllById(request.getTechStackIds());
            List<UserTechStack> newTechStacks = techStacks.stream()
                    .map(ts -> UserTechStack.builder().user(user).techStack(ts).build())
                    .toList();
            userTechStackRepository.saveAll(newTechStacks);
        }

        if (request.getCurriculumCategories() != null) {
            userCurriculumCategoryRepository.deleteByUserUserId(userId);
            List<UserCurriculumCategory> newCategories = request.getCurriculumCategories().stream()
                    .map(cat -> UserCurriculumCategory.builder().user(user).category(cat).build())
                    .toList();
            userCurriculumCategoryRepository.saveAll(newCategories);
        }

        redisService.delete(buildProfileCacheKey(userId));

        List<DevPositionInfo> desiredPositions = userDesiredPositionRepository.findByUserUserId(userId).stream()
                .map(udp -> DevPositionInfo.from(udp.getDevPosition()))
                .toList();
        List<TechStackInfo> techStacks = userTechStackRepository.findByUserUserId(userId).stream()
                .map(uts -> TechStackInfo.from(uts.getTechStack()))
                .toList();
        List<CurriculumCategory> curriculumCategories = userCurriculumCategoryRepository.findByUserUserId(userId).stream()
                .map(UserCurriculumCategory::getCategory)
                .toList();

        log.info("유저 프로필 수정 완료. userId={}", userId);

        return UserProfileUpdateResponse.builder()
                .position(user.getPosition())
                .desiredPositions(desiredPositions)
                .techStacks(techStacks)
                .curriculumCategories(curriculumCategories)
                .considerPersonalSchedule(user.getCalendarSyncEnabled())
                .build();
    }

    protected User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private UserProfileResponse buildProfileResponse(Long userId) {
        User user = findUserByIdOrThrow(userId);
        List<DevPositionInfo> desiredPositions = userDesiredPositionRepository.findByUserUserId(userId).stream()
                .map(udp -> DevPositionInfo.from(udp.getDevPosition()))
                .toList();
        List<TechStackInfo> techStacks = userTechStackRepository.findByUserUserId(userId).stream()
                .map(uts -> TechStackInfo.from(uts.getTechStack()))
                .toList();
        List<CurriculumCategory> curriculumCategories = userCurriculumCategoryRepository.findByUserUserId(userId).stream()
                .map(UserCurriculumCategory::getCategory)
                .toList();
        return UserProfileResponse.of(user, desiredPositions, techStacks, curriculumCategories);
    }

    private void cacheProfile(String cacheKey, UserProfileResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisService.save(cacheKey, json, PROFILE_CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (JacksonException e) {
            log.warn("유저 프로필 캐시 직렬화 실패. key={}", cacheKey, e);
        }
    }

    private UserProfileResponse deserializeProfile(String json) {
        try {
            return objectMapper.readValue(json, UserProfileResponse.class);
        } catch (JacksonException e) {
            log.warn("유저 프로필 캐시 역직렬화 실패", e);
            return null;
        }
    }

    private String buildProfileCacheKey(Long userId) {
        return "user:profile:" + userId;
    }
}
