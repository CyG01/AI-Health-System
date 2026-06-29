package com.example.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.CommunityLike;
import com.example.entity.CommunityPost;
import com.example.entity.SysUser;
import com.example.mapper.*;
import com.example.vo.CommunityPostVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityServiceImplTest {

    @Mock private CommunityPostMapper postMapper;
    @Mock private CommunityCommentMapper commentMapper;
    @Mock private CommunityLikeMapper likeMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private ExerciseRecordMapper exerciseRecordMapper;

    private CommunityServiceImpl communityService;

    @BeforeEach
    void setUp() {
        communityService = new CommunityServiceImpl(postMapper, commentMapper, likeMapper,
                userMapper, exerciseRecordMapper);
    }

    // ==================== getRanking("calories") ====================

    @Test
    @DisplayName("getCaloriesRanking returns correct aggregated results with user info")
    void shouldReturnCaloriesRankingWithUserInfo() {
        List<Map<String, Object>> rankingData = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("userId", 1L);
        row1.put("totalCalories", 5000);
        rankingData.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("userId", 2L);
        row2.put("totalCalories", 3000);
        rankingData.add(row2);

        when(exerciseRecordMapper.selectCaloriesRanking(10)).thenReturn(rankingData);

        SysUser user1 = new SysUser();
        user1.setId(1L);
        user1.setNickname("Alice");
        user1.setAvatar("avatar1.png");

        SysUser user2 = new SysUser();
        user2.setId(2L);
        user2.setNickname("Bob");
        user2.setAvatar("avatar2.png");

        when(userMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(user1, user2));

        List<Map<String, Object>> result = communityService.getRanking("calories", 10);

        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).get("nickname"));
        assertEquals(5000, result.get(0).get("calories"));
        assertEquals("Bob", result.get(1).get("nickname"));
        assertEquals(3000, result.get(1).get("calories"));
    }

    @Test
    @DisplayName("getCaloriesRanking with empty records returns empty list")
    void shouldReturnEmptyListWhenNoRecords() {
        when(exerciseRecordMapper.selectCaloriesRanking(10)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = communityService.getRanking("calories", 10);

        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectBatchIds(anyCollection());
    }

    @Test
    @DisplayName("getCaloriesRanking handles missing user gracefully")
    void shouldHandleMissingUserInRanking() {
        List<Map<String, Object>> rankingData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("userId", 999L);
        row.put("totalCalories", 1000);
        rankingData.add(row);

        when(exerciseRecordMapper.selectCaloriesRanking(10)).thenReturn(rankingData);
        when(userMapper.selectBatchIds(List.of(999L))).thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = communityService.getRanking("calories", 10);

        assertEquals(1, result.size());
        assertEquals("未知", result.get(0).get("nickname"));
    }

    // ==================== getPostList ====================

    @Test
    @DisplayName("getPostList batch-loads likes correctly in one query")
    void shouldBatchLoadLikesForPostList() {
        // Setup posts
        CommunityPost post1 = buildPost(1L, "Post 1");
        CommunityPost post2 = buildPost(2L, "Post 2");

        Page<CommunityPost> page = new Page<>(1, 10);
        page.setRecords(List.of(post1, post2));
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);

        // User liked post1 only
        CommunityLike like = new CommunityLike();
        like.setPostId(1L);
        like.setUserId(10L);
        when(likeMapper.selectList(any())).thenReturn(List.of(like));

        List<CommunityPostVO> result = communityService.getPostList(10L, 1, 10);

        assertEquals(2, result.size());
        assertTrue(result.get(0).getIsLiked());   // post1 is liked
        assertFalse(result.get(1).getIsLiked());   // post2 is not liked

        // Verify only ONE batch query was made (not N+1)
        verify(likeMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("getPostList with null userId returns all posts with isLiked=false")
    void shouldReturnPostsNotLikedWhenUserIdNull() {
        CommunityPost post = buildPost(1L, "Post 1");
        Page<CommunityPost> page = new Page<>(1, 10);
        page.setRecords(List.of(post));
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);

        List<CommunityPostVO> result = communityService.getPostList(null, 1, 10);

        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsLiked());
        verify(likeMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("getPostList with empty page returns empty list")
    void shouldReturnEmptyListWhenNoPosts() {
        Page<CommunityPost> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);

        List<CommunityPostVO> result = communityService.getPostList(10L, 1, 10);

        assertTrue(result.isEmpty());
        verify(likeMapper, never()).selectList(any());
    }

    // ==================== helpers ====================

    private CommunityPost buildPost(Long id, String content) {
        CommunityPost post = new CommunityPost();
        post.setId(id);
        post.setUserId(100L);
        post.setUserNickname("TestUser");
        post.setUserAvatar("avatar.png");
        post.setContent(content);
        post.setLikeCount(5);
        post.setCommentCount(2);
        post.setStatus(1);
        post.setCreateTime(LocalDateTime.now().minusMinutes(30));
        return post;
    }
}
