package com.ssafy.s14p21a506.mockpost.service;

import com.ssafy.s14p21a506.auth.AuthenticatedUser;
import com.ssafy.s14p21a506.mockpost.dto.MockPostCreateRequest;
import com.ssafy.s14p21a506.mockpost.dto.MockPostResponse;
import com.ssafy.s14p21a506.mockpost.dto.MockPostUpdateRequest;
import java.util.List;

public interface MockPostService {

    MockPostResponse create(AuthenticatedUser currentUser, MockPostCreateRequest request);

    List<MockPostResponse> list();

    MockPostResponse get(long mockPostId);

    MockPostResponse update(AuthenticatedUser currentUser, long mockPostId, MockPostUpdateRequest request);

    void delete(AuthenticatedUser currentUser, long mockPostId);
}
