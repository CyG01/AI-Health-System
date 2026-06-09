<template>
  <div class="community-page">
    <div class="page-header">
      <h2>健康社区</h2>
      <el-button type="primary" @click="showPostDialog = true">
        <el-icon><Edit /></el-icon> 发布动态
      </el-button>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：动态流 -->
      <el-col :span="16">
        <div v-loading="loading" class="post-feed">
          <div v-if="posts.length === 0 && !loading" class="empty-state">
            <el-empty description="暂无动态，快来发布第一条吧" :image-size="100" />
          </div>

          <div v-for="post in posts" :key="post.id" class="post-card glass-card">
            <!-- 帖子头部 -->
            <div class="post-header">
              <div class="post-user">
                <el-avatar :size="40" :src="post.userAvatar">
                  {{ (post.userNickname || 'U')[0].toUpperCase() }}
                </el-avatar>
                <div class="user-meta">
                  <span class="username">{{ post.userNickname }}</span>
                  <span class="time">{{ post.timeAgo }}</span>
                </div>
              </div>
              <el-dropdown v-if="post.userId === currentUserId" trigger="click" @command="(cmd) => handlePostAction(cmd, post)">
                <el-button text :icon="'MoreFilled'" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="delete" style="color: #ff4d4f">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <!-- 运动标签 -->
            <div v-if="post.exerciseType || post.exerciseDuration || post.caloriesBurned" class="post-exercise">
              <el-tag v-if="post.exerciseType" type="success" size="small" effect="plain">{{ post.exerciseType }}</el-tag>
              <span v-if="post.exerciseDuration">{{ post.exerciseDuration }}分钟</span>
              <span v-if="post.caloriesBurned" class="calories">{{ post.caloriesBurned }} kcal</span>
            </div>

            <!-- 内容 -->
            <div class="post-content">{{ post.content }}</div>

            <!-- 操作栏 -->
            <div class="post-actions">
              <el-button
                text
                :type="post.isLiked ? 'primary' : 'default'"
                :icon="post.isLiked ? 'StarFilled' : 'Star'"
                @click="handleLike(post)"
              >
                {{ post.likeCount || 0 }}
              </el-button>
              <el-button text :icon="'ChatDotRound'" @click="openComments(post)">
                {{ post.commentCount || 0 }}
              </el-button>
            </div>
          </div>

          <!-- 加载更多 -->
          <div class="load-more" v-if="hasMore">
            <el-button :loading="loading" @click="loadMore">加载更多</el-button>
          </div>
        </div>
      </el-col>

      <!-- 右侧：排行榜 -->
      <el-col :span="8">
        <div class="ranking-card glass-card">
          <h3 class="ranking-title">热量消耗榜</h3>
          <div class="ranking-list">
            <div
              v-for="(item, idx) in ranking"
              :key="item.userId"
              class="ranking-item"
            >
              <span class="rank-num" :class="'rank-' + (idx + 1)">
                {{ idx + 1 }}
              </span>
              <el-avatar :size="32" :src="item.avatar">
                {{ (item.nickname || 'U')[0].toUpperCase() }}
              </el-avatar>
              <span class="rank-nickname">{{ item.nickname }}</span>
              <span class="rank-value">{{ item.calories }} kcal</span>
            </div>
          </div>
          <el-empty v-if="ranking.length === 0" description="暂无排行数据" :image-size="60" />
        </div>
      </el-col>
    </el-row>

    <!-- 发布动态弹窗 -->
    <el-dialog v-model="showPostDialog" title="发布运动动态" width="500px" destroy-on-close>
      <el-form :model="postForm" label-width="80px">
        <el-form-item label="内容">
          <el-input
            v-model="postForm.content"
            type="textarea"
            :rows="4"
            placeholder="分享你的运动成果..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="运动类型">
          <el-select v-model="postForm.exerciseType" placeholder="选填" clearable style="width:100%">
            <el-option label="跑步" value="跑步" />
            <el-option label="健身" value="健身" />
            <el-option label="游泳" value="游泳" />
            <el-option label="骑行" value="骑行" />
            <el-option label="瑜伽" value="瑜伽" />
            <el-option label="篮球" value="篮球" />
            <el-option label="跳绳" value="跳绳" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="时长(分钟)">
              <el-input-number v-model="postForm.exerciseDuration" :min="0" :max="600" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="消耗(kcal)">
              <el-input-number v-model="postForm.caloriesBurned" :min="0" :max="9999" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showPostDialog = false">取消</el-button>
        <el-button type="primary" :loading="posting" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>

    <!-- 评论弹窗 -->
    <el-dialog v-model="showCommentDialog" title="评论" width="480px" destroy-on-close>
      <div class="comment-list">
        <div v-if="currentComments.length === 0" class="no-comments">
          <el-empty description="暂无评论，来抢沙发吧" :image-size="60" />
        </div>
        <div v-for="c in currentComments" :key="c.id" class="comment-item">
          <el-avatar :size="28" :src="c.userAvatar">{{ (c.userNickname || 'U')[0].toUpperCase() }}</el-avatar>
          <div class="comment-body">
            <div class="comment-user">
              <span class="comment-nickname">{{ c.userNickname }}</span>
              <span class="comment-time">{{ c.timeAgo }}</span>
            </div>
            <div class="comment-content">{{ c.content }}</div>
          </div>
          <el-button
            v-if="c.userId === currentUserId"
            text
            size="small"
            type="danger"
            :icon="'Delete'"
            @click="handleDeleteComment(c)"
          />
        </div>
      </div>
      <div class="comment-input">
        <el-input
          v-model="commentText"
          placeholder="发表评论..."
          maxlength="200"
          @keyup.enter="handleComment"
        >
          <template #append>
            <el-button :loading="commenting" @click="handleComment">发送</el-button>
          </template>
        </el-input>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  createPost, deletePost, getPostList, toggleLike,
  createComment, deleteComment, getComments, getRanking
} from '@/api/community'

const userStore = useUserStore()
const loading = ref(false)
const posting = ref(false)
const commenting = ref(false)
const posts = ref([])
const ranking = ref([])
const page = ref(1)
const hasMore = ref(true)

const showPostDialog = ref(false)
const showCommentDialog = ref(false)
const currentPost = ref(null)
const currentComments = ref([])
const commentText = ref('')

const currentUserId = userStore.userInfo?.id

const postForm = reactive({
  content: '',
  exerciseType: '',
  exerciseDuration: null,
  caloriesBurned: null
})

async function loadPosts(reset = false) {
  if (reset) {
    page.value = 1
    hasMore.value = true
  }
  loading.value = true
  try {
    const res = await getPostList(page.value, 10)
    const list = res.data || []
    if (reset) {
      posts.value = list
    } else {
      posts.value.push(...list)
    }
    hasMore.value = list.length === 10
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value++
  loadPosts()
}

async function loadRanking() {
  try {
    const res = await getRanking('calories', 20)
    ranking.value = res.data || []
  } catch { /* ignore */ }
}

async function handlePublish() {
  if (!postForm.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  posting.value = true
  try {
    await createPost({ ...postForm })
    ElMessage.success('发布成功')
    showPostDialog.value = false
    postForm.content = ''
    postForm.exerciseType = ''
    postForm.exerciseDuration = null
    postForm.caloriesBurned = null
    await loadPosts(true)
  } finally {
    posting.value = false
  }
}

async function handleLike(post) {
  try {
    const res = await toggleLike(post.id)
    post.isLiked = res.data.isLiked
    post.likeCount = res.data.likeCount
  } catch { /* ignore */ }
}

function handlePostAction(cmd, post) {
  if (cmd === 'delete') {
    ElMessageBox.confirm('确定删除这条动态吗？', '确认', { type: 'warning' })
      .then(async () => {
        await deletePost(post.id)
        ElMessage.success('已删除')
        posts.value = posts.value.filter(p => p.id !== post.id)
      }).catch(() => {})
  }
}

async function openComments(post) {
  currentPost.value = post
  showCommentDialog.value = true
  commentText.value = ''
  try {
    const res = await getComments(post.id)
    currentComments.value = res.data || []
  } catch { /* ignore */ }
}

async function handleComment() {
  if (!commentText.value.trim()) return
  commenting.value = true
  try {
    await createComment({ postId: currentPost.value.id, content: commentText.value })
    commentText.value = ''
    ElMessage.success('评论成功')
    // 刷新评论
    const res = await getComments(currentPost.value.id)
    currentComments.value = res.data || []
    currentPost.value.commentCount = (currentPost.value.commentCount || 0) + 1
  } finally {
    commenting.value = false
  }
}

async function handleDeleteComment(comment) {
  ElMessageBox.confirm('删除此评论？', '确认', { type: 'warning' })
    .then(async () => {
      await deleteComment(comment.id)
      currentComments.value = currentComments.value.filter(c => c.id !== comment.id)
      currentPost.value.commentCount = Math.max(0, (currentPost.value.commentCount || 1) - 1)
      ElMessage.success('已删除')
    }).catch(() => {})
}

onMounted(() => {
  loadPosts(true)
  loadRanking()
})
</script>

<style scoped lang="scss">
.community-page { padding: 0 4px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h2 { margin: 0; font-size: 22px; color: var(--text-primary); }
}

.post-card {
  padding: 16px 20px;
  margin-bottom: 14px;

  .post-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 10px;
  }

  .post-user {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .user-meta {
    display: flex;
    flex-direction: column;
    .username { font-weight: 600; font-size: 14px; color: var(--text-primary); }
    .time { font-size: 12px; color: var(--text-secondary); }
  }

  .post-exercise {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 10px;
    font-size: 13px;
    color: var(--text-secondary);
    .calories { color: #fa8c16; font-weight: 500; }
  }

  .post-content {
    font-size: 15px;
    line-height: 1.6;
    color: var(--text-primary);
    margin-bottom: 14px;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .post-actions {
    display: flex;
    gap: 8px;
    padding-top: 10px;
    border-top: 1px solid rgba(48, 54, 61, 0.3);
  }
}

.load-more {
  text-align: center;
  padding: 20px 0;
}

.ranking-card {
  padding: 16px 20px;
  position: sticky;
  top: 16px;

  .ranking-title {
    margin: 0 0 16px;
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .ranking-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
  &:hover { background: rgba(88, 166, 255, 0.05); }

  .rank-num {
    width: 24px;
    text-align: center;
    font-weight: 700;
    font-size: 14px;
    color: var(--text-secondary);
    &.rank-1 { color: #f5a623; font-size: 16px; }
    &.rank-2 { color: #a0a0a0; }
    &.rank-3 { color: #cd7f32; }
  }

  .rank-nickname {
    flex: 1;
    font-size: 14px;
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .rank-value {
    font-size: 13px;
    font-weight: 600;
    color: #fa8c16;
  }
}

.comment-list {
  max-height: 320px;
  overflow-y: auto;
  margin-bottom: 14px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(48, 54, 61, 0.2);

  .comment-body {
    flex: 1;
    min-width: 0;
  }

  .comment-user {
    display: flex;
    gap: 8px;
    margin-bottom: 4px;
    .comment-nickname { font-weight: 600; font-size: 13px; color: var(--text-primary); }
    .comment-time { font-size: 11px; color: var(--text-secondary); }
  }

  .comment-content {
    font-size: 14px;
    color: var(--text-primary);
    word-break: break-word;
  }
}

.comment-input { padding-top: 8px; }
</style>