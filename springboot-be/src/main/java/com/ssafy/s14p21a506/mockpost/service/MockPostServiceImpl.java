package com.ssafy.s14p21a506.mockpost.service;

import com.ssafy.s14p21a506.auth.AuthenticatedUser;
import com.ssafy.s14p21a506.exception.BaseException;
import com.ssafy.s14p21a506.exception.ErrorCode;
import com.ssafy.s14p21a506.mockpost.dto.MockPostCreateRequest;
import com.ssafy.s14p21a506.mockpost.dto.MockPostResponse;
import com.ssafy.s14p21a506.mockpost.dto.MockPostUpdateRequest;
import com.ssafy.s14p21a506.mockpost.entity.MockPost;
import com.ssafy.s14p21a506.mockpost.repository.MockPostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MockPostServiceImpl implements MockPostService {

    private final MockPostRepository mockPostRepository;

    @Override
    @Transactional
    public MockPostResponse create(AuthenticatedUser currentUser, MockPostCreateRequest request) {
        MockPost post = MockPost.createBuilder()
                .authorSub(currentUser.subject())
                .authorName(currentUser.username())
                .title(request.title())
                .content(request.content())
                .build();

        return MockPostResponse.from(mockPostRepository.save(post));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MockPostResponse> list() {
        return mockPostRepository.findAll().stream()
                .map(MockPostResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MockPostResponse get(long mockPostId) {
        MockPost post = mockPostRepository.findById(mockPostId)
                .orElseThrow(() -> new BaseException(ErrorCode.MOCK_POST_NOT_FOUND));

        return MockPostResponse.from(post);
    }

    @Override
    @Transactional
    public MockPostResponse update(AuthenticatedUser currentUser, long mockPostId, MockPostUpdateRequest request) {
        MockPost post = mockPostRepository.findById(mockPostId)
                .orElseThrow(() -> new BaseException(ErrorCode.MOCK_POST_NOT_FOUND));

        validateOwnership(currentUser, post);
        post.update(request.title(), request.content());
        return MockPostResponse.from(post);
    }

    @Override
    @Transactional
    public void delete(AuthenticatedUser currentUser, long mockPostId) {
        MockPost post = mockPostRepository.findById(mockPostId)
                .orElseThrow(() -> new BaseException(ErrorCode.MOCK_POST_NOT_FOUND));

        validateOwnership(currentUser, post);
        mockPostRepository.delete(post);
    }

    private static void validateOwnership(AuthenticatedUser currentUser, MockPost post) {
        if (!post.isOwnedBy(currentUser.subject())) {
            throw new BaseException(ErrorCode.FORBIDDEN);
        }
    }
}
