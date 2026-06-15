<template>
  <div class="community-page">
    <div class="page-header">
      <h2>{{ $t('community.title') || '健康社区' }}</h2>
      <NButton type="primary" @click="showPostDialog = true">
        <template #icon><NIcon><CreateOutline /></NIcon></template>
        {{ $t('community.publish') || '发布动态' }}
      </NButton>
    </div>

    <NGrid :x-gap="20" :y-gap="0" :cols="24">
      <!-- 左侧：动态流 -->
      <NGi :span="16">
        <div class="post-feed" :class="{ 'is-loading': loading }">
          <div v-if="posts.length === 0 && !loading" class="empty-state">
            <NEmpty :description="$t('community.empty') || '暂无动态，快来发布第一条吧'" />
          </div>

          <div v-for="post in posts" :key="post.id" class="post-card glass-card">
            <!-- 帖子头部 -->
            <div class="post-header">
              <div class="post-user">
                <NAvatar :size="40" :src="post.userAvatar" round>
                  {{ (post.userNickname || 'U')[0].toUpperCase() }}
                </NAvatar>
                <div class="user-meta">
                  <span class="username">{{ post.userNickname }}</span>
                  <span class="time">{{ post.timeAgo }}</span>
                </div>
              </div>
              <NDropdown
                v-if="post.userId === currentUserId"
                :options="postDropdownOptions"
                @select="(key: string) => handlePostAction(key, post)"
                trigger="click"
              >
                <NButton text>
                  <template #icon><NIcon><EllipsisHorizontal /></NIcon></template>
                </NButton>
              </NDropdown>
            </div>

            <!-- 运动标签 -->
            <div v-if="post.exerciseType || post.exerciseDuration || post.caloriesBurned" class="post-exercise">
              <NTag v-if="post.exerciseType" type="success" size="small" :bordered="true">{{ post.exerciseType }}</NTag>
              <span v-if="post.exerciseDuration">{{ post.exerciseDuration }}分钟</span>
              <span v-if="post.caloriesBurned" class="calories">{{ post.caloriesBurned }} kcal</span>
            </div>

            <!-- 内容 -->
            <div class="post-content">{{ post.content }}</div>

            <!-- 操作栏 -->
            <div class="post-actions">
              <NButton
                text
                :type="post.isLiked ? 'primary' : 'default'"
                @click="handleLike(post)"
              >
                <template #icon>
                  <NIcon><component :is="post.isLiked ? Star : StarOutline" /></NIcon>
                </template>
                {{ post.likeCount || 0 }}
              </NButton>
              <NButton text @click="openComments(post)">
                <template #icon><NIcon><ChatbubbleEllipsesOutline /></NIcon></template>
                {{ post.commentCount || 0 }}
              </NButton>
            </div>
          </div>

          <!-- 加载更多 -->
          <div class="load-more" v-if="hasMore">
            <NButton :loading="loading" @click="loadMore">加载更多</NButton>
          </div>
        </div>
      </NGi>

      <!-- 右侧：排行榜 -->
      <NGi :span="8">
        <div class="ranking-card glass-card">
          <h3 class="ranking-title">{{ $t('community.ranking') || '热量消耗榜' }}</h3>
          <div class="ranking-list">
            <div
              v-for="(item, idx) in ranking"
              :key="item.userId"
              class="ranking-item"
            >
              <span class="rank-num" :class="'rank-' + (idx + 1)">
                {{ idx + 1 }}
              </span>
              <NAvatar :size="32" :src="item.avatar" round>
                {{ (item.nickname || 'U')[0].toUpperCase() }}
              </NAvatar>
              <span class="rank-nickname">{{ item.nickname }}</span>
              <span class="rank-value">{{ item.calories }} kcal</span>
            </div>
          </div>
          <NEmpty v-if="ranking.length === 0" description="暂无排行数据" />
        </div>
      </NGi>
    </NGrid>

    <!-- 发布动态弹窗 -->
    <NModal
      v-model:show="showPostDialog"
      preset="card"
      :title="$t('community.publishPost') || '发布运动动态'"
      style="width: 500px; max-width: 90vw"
      :mask-closable="true"
      @after-leave="resetPostForm"
    >
      <NForm :model="postForm" label-placement="left" label-width="80px">
        <NFormItem label="内容">
          <NInput
            v-model:value="postForm.content"
            type="textarea"
            :rows="4"
            placeholder="分享你的运动成果..."
            :maxlength="500"
            show-count
          />
        </NFormItem>
        <NFormItem label="运动类型">
          <NSelect
            v-model:value="postForm.exerciseType"
            :options="exerciseTypeOptions"
            placeholder="选填"
            clearable
          />
        </NFormItem>
        <NGrid :x-gap="16" :cols="2">
          <NGi>
            <NFormItem label="时长(分钟)">
              <NInputNumber v-model:value="postForm.exerciseDuration" :min="0" :max="600" style="width:100%" />
            </NFormItem>
          </NGi>
          <NGi>
            <NFormItem label="消耗(kcal)">
              <NInputNumber v-model:value="postForm.caloriesBurned" :min="0" :max="9999" style="width:100%" />
            </NFormItem>
          </NGi>
        </NGrid>
      </NForm>
      <template #footer>
        <div style="display:flex;justify-content:flex-end;gap:10px">
          <NButton @click="showPostDialog = false">取消</NButton>
          <NButton type="primary" :loading="posting" @click="handlePublish">发布</NButton>
        </div>
      </template>
    </NModal>

    <!-- 评论弹窗 -->
    <NModal
      v-model:show="showCommentDialog"
      preset="card"
      title="评论"
      style="width: 480px; max-width: 90vw"
      :mask-closable="true"
    >
      <div class="comment-list">
        <div v-if="currentComments.length === 0" class="no-comments">
          <NEmpty description="暂无评论，来抢沙发吧" />
        </div>
        <div v-for="c in currentComments" :key="c.id" class="comment-item">
          <NAvatar :size="28" :src="c.userAvatar" round>{{ (c.userNickname || 'U')[0].toUpperCase() }}</NAvatar>
          <div class="comment-body">
            <div class="comment-user">
              <span class="comment-nickname">{{ c.userNickname }}</span>
              <span class="comment-time">{{ c.timeAgo }}</span>
            </div>
            <div class="comment-content">{{ c.content }}</div>
          </div>
          <NButton
            v-if="c.userId === currentUserId"
            text
            size="small"
            type="error"
            @click="handleDeleteComment(c)"
          >
            <template #icon><NIcon><TrashOutline /></NIcon></template>
          </NButton>
        </div>
      </div>
      <div class="comment-input">
        <NInput
          v-model:value="commentText"
          placeholder="发表评论..."
          :maxlength="200"
          @keyup.enter="handleComment"
        >
          <template #suffix>
            <NButton size="small" :loading="commenting" @click="handleComment" text>发送</NButton>
          </template>
        </NInput>
      </div>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  NButton, NIcon, NAvatar, NTag, NEmpty, NModal, NForm, NFormItem,
  NInput, NInputNumber, NSelect, NGrid, NGi, NDropdown,
  useMessage, useDialog
} from 'naive-ui'
import {
  CreateOutline, EllipsisHorizontal, Star, StarOutline,
  ChatbubbleEllipsesOutline, TrashOutline
} from '@vicons/ionicons5'
import { useAuthStore } from '@/store/modules/auth'
import {
  fetchCreatePost, fetchDeletePost, fetchGetPostList, fetchToggleLike,
  fetchCreateComment, fetchDeleteComment, fetchGetComments, fetchGetRanking
} from '@/service/api'

interface Post {
  id: number | string
  userId: number | string
  userNickname: string
  userAvatar: string
  timeAgo: string
  content: string
  exerciseType: string
  exerciseDuration: number | null
  caloriesBurned: number | null
  isLiked: boolean
  likeCount: number
  commentCount: number
}

interface RankingItem {
  userId: number | string
  nickname: string
  avatar: string
  calories: number
}

interface Comment {
  id: number | string
  userId: number | string
  userNickname: string
  userAvatar: string
  timeAgo: string
  content: string
}

defineOptions({ name: 'CommunityFeed' })

const message = useMessage()
const dialog = useDialog()

const userStore = useAuthStore()
const loading = ref(false)
const posting = ref(false)
const commenting = ref(false)
const posts = ref<Post[]>([])
const ranking = ref<RankingItem[]>([])
const page = ref(1)
const hasMore = ref(true)

const showPostDialog = ref(false)
const showCommentDialog = ref(false)
const currentPost = ref<Post | null>(null)
const currentComments = ref<Comment[]>([])
const commentText = ref('')

const currentUserId = userStore.userInfo?.id

const postForm = reactive<{
  content: string
  exerciseType: string | null
  exerciseDuration: number | null
  caloriesBurned: number | null
}>({
  content: '',
  exerciseType: null,
  exerciseDuration: null,
  caloriesBurned: null
})

const exerciseTypeOptions = [
  { label: '跑步', value: '跑步' },
  { label: '健身', value: '健身' },
  { label: '游泳', value: '游泳' },
  { label: '骑行', value: '骑行' },
  { label: '瑜伽', value: '瑜伽' },
  { label: '篮球', value: '篮球' },
  { label: '跳绳', value: '跳绳' },
  { label: '其他', value: '其他' }
]

const postDropdownOptions = [
  { label: '删除', key: 'delete', props: { style: 'color: var(--color-danger, #e88080)' } }
]

function resetPostForm() {
  postForm.content = ''
  postForm.exerciseType = null
  postForm.exerciseDuration = null
  postForm.caloriesBurned = null
}

async function loadPosts(reset = false) {
  if (reset) {
    page.value = 1
    hasMore.value = true
  }
  loading.value = true
  try {
    const { data } = await fetchGetPostList(page.value, 10)
    const list: Post[] = (data as any) || []
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
    const { data } = await fetchGetRanking('calories', 20)
    ranking.value = (data as any) || []
  } catch { /* ignore */ }
}

async function handlePublish() {
  if (!postForm.content.trim()) {
    message.warning('请输入内容')
    return
  }
  posting.value = true
  try {
    await fetchCreatePost({ ...postForm } as any)
    message.success('发布成功')
    showPostDialog.value = false
    resetPostForm()
    await loadPosts(true)
  } finally {
    posting.value = false
  }
}

async function handleLike(post: Post) {
  try {
    const { data } = await fetchToggleLike(post.id as number)
    post.isLiked = (data as any).isLiked
    post.likeCount = (data as any).likeCount
  } catch { /* ignore */ }
}

function handlePostAction(cmd: string, post: Post) {
  if (cmd === 'delete') {
    dialog.warning({
      title: '确认',
      content: '确定删除这条动态吗？',
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        await fetchDeletePost(post.id as number)
        message.success('已删除')
        posts.value = posts.value.filter(p => p.id !== post.id)
      }
    })
  }
}

async function openComments(post: Post) {
  currentPost.value = post
  showCommentDialog.value = true
  commentText.value = ''
  try {
    const { data } = await fetchGetComments(post.id as number)
    currentComments.value = (data as any) || []
  } catch { /* ignore */ }
}

async function handleComment() {
  if (!commentText.value.trim() || !currentPost.value) return
  commenting.value = true
  try {
    await fetchCreateComment({ postId: currentPost.value.id as number, content: commentText.value })
    commentText.value = ''
    message.success('评论成功')
    // 刷新评论
    const { data } = await fetchGetComments(currentPost.value.id as number)
    currentComments.value = (data as any) || []
    currentPost.value.commentCount = (currentPost.value.commentCount || 0) + 1
  } finally {
    commenting.value = false
  }
}

async function handleDeleteComment(comment: Comment) {
  dialog.warning({
    title: '确认',
    content: '删除此评论？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await fetchDeleteComment(comment.id as number)
      currentComments.value = currentComments.value.filter(c => c.id !== comment.id)
      if (currentPost.value) {
        currentPost.value.commentCount = Math.max(0, (currentPost.value.commentCount || 1) - 1)
      }
      message.success('已删除')
    }
  })
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

.post-feed {
  position: relative;
  &.is-loading { opacity: 0.7; pointer-events: none; }
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
