package com.ssafy.s14p21a506.mockpost.api;

import com.ssafy.s14p21a506.mockpost.dto.MockPostCreateRequest;
import com.ssafy.s14p21a506.mockpost.dto.MockPostResponse;
import com.ssafy.s14p21a506.mockpost.dto.MockPostUpdateRequest;
import com.ssafy.s14p21a506.mockpost.service.MockPostService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/mock-posts")
public class MockPostApi implements MockPostApiDoc {

    private final MockPostService mockPostService;

    @Override
    @PostMapping
    public ResponseEntity<MockPostResponse> create(@Valid @RequestBody MockPostCreateRequest request) {
        MockPostResponse created = mockPostService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<MockPostResponse>> list() {
        return ResponseEntity.ok(mockPostService.list());
    }

    @Override
    @GetMapping("/{mockPostId}")
    public ResponseEntity<MockPostResponse> get(@PathVariable long mockPostId) {
        return ResponseEntity.ok(mockPostService.get(mockPostId));
    }

    @Override
    @PutMapping("/{mockPostId}")
    public ResponseEntity<MockPostResponse> update(
            @PathVariable long mockPostId,
            @Valid @RequestBody MockPostUpdateRequest request
    ) {
        return ResponseEntity.ok(mockPostService.update(mockPostId, request));
    }

    @Override
    @DeleteMapping("/{mockPostId}")
    public ResponseEntity<Void> delete(@PathVariable long mockPostId) {
        mockPostService.delete(mockPostId);
        return ResponseEntity.noContent().build();
    }
}
