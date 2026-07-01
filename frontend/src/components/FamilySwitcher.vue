<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { NPopover, NScrollbar, NSpin } from 'naive-ui';
import { useRouter } from 'vue-router';
import { fetchFamilyMembers, fetchMyFamilies } from '@/service/api';
defineOptions({ name: 'FamilySwitcher' });
type Selection = { type: 'self' } | { type: 'member'; member: Api.Family.Member };
const emit = defineEmits<{ (e: 'select', payload: Selection): void }>();
const router = useRouter();
const loading = ref(true);
const groups = ref<{ family: Api.Family.Family; members: Api.Family.Member[] }[]>([]);
const current = ref<Selection>({ type: 'self' });
const label = computed(() => current.value.type === 'self' ? '我的健康'
  : current.value.member.nicknameInFamily || current.value.member.username || `成员${current.value.member.id}`);
const isSelf = computed(() => current.value.type === 'self');
const memberName = (m: Api.Family.Member) => m.nicknameInFamily || m.username || `成员${m.id}`;
onMounted(async () => {
  try {
    const { data: families, error } = await fetchMyFamilies();
    if (error || !families) return;
    groups.value = await Promise.all(
      families.map(async f => {
        const { data: members } = await fetchFamilyMembers(f.id);
        return { family: f, members: (members || []) as Api.Family.Member[] };
      })
    );
  } finally { loading.value = false; }
});
function pick(s: Selection) { current.value = s; emit('select', s); }
</script>

<template>
  <NPopover trigger="click" :show-arrow="false" placement="bottom-end" :width="220" raw>
    <template #trigger>
      <button class="fs-chip" :class="{ 'fs-chip--fam': !isSelf }">
        <span class="fs-av">
          <img v-if="!isSelf && current.type === 'member' && current.member.avatar" :src="current.member.avatar" alt="" />
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </span>
        <span class="fs-lbl">{{ label }}</span>
        <svg class="fs-caret" viewBox="0 0 16 16" fill="currentColor"><path d="M4.5 6l3.5 4 3.5-4z"/></svg>
      </button>
    </template>
    <div class="fs-panel">
      <NSpin :show="loading" size="small">
        <NScrollbar style="max-height: 320px">
          <div class="fs-body">
            <button class="fs-item" :class="{ 'fs-active': isSelf }" @click="pick({ type: 'self' })">
              <svg class="fs-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              我的健康
            </button>
            <template v-for="g in groups" :key="g.family.id">
              <div class="fs-grp">{{ g.family.familyName }}</div>
              <button v-for="m in g.members" :key="m.id" class="fs-item"
                :class="{ 'fs-active': current.type === 'member' && current.member.id === m.id }"
                @click="pick({ type: 'member', member: m })">
                <img v-if="m.avatar" class="fs-mav" :src="m.avatar" alt="" />
                <span v-else class="fs-mav fs-mav-fb">{{ memberName(m).charAt(0) }}</span>
                <span class="fs-name">{{ memberName(m) }}</span>
                <span class="fs-role">{{ m.memberRole }}</span>
              </button>
            </template>
          </div>
        </NScrollbar>
      </NSpin>
      <div class="fs-footer">
        <button class="fs-manage" @click="router.push('/family')">管理家庭</button>
      </div>
    </div>
  </NPopover>
</template>

<style scoped>
.fs-chip{display:inline-flex;align-items:center;gap:6px;padding:4px 10px 4px 4px;height:32px;border-radius:var(--radius-full);border:1px solid var(--sport-border);background:var(--sport-bg-surface);color:var(--sport-text-primary);cursor:pointer;font:600 var(--text-sm) var(--font-family);transition:.2s;line-height:1}
.fs-chip:hover{background:var(--sport-bg-elevated);border-color:var(--chart-sky)}
.fs-chip--fam{border-color:var(--chart-sky);background:rgba(56,189,248,.08)}
.fs-av{width:24px;height:24px;border-radius:var(--radius-full);overflow:hidden;display:flex;align-items:center;justify-content:center;background:var(--sport-bg-elevated);color:var(--sport-text-secondary);flex-shrink:0}
.fs-av img{width:100%;height:100%;object-fit:cover}
.fs-av svg{width:14px;height:14px}
.fs-lbl{max-width:100px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.fs-caret{width:16px;height:16px;color:var(--sport-text-tertiary);flex-shrink:0}
.fs-panel{background:var(--sport-bg-surface);border:1px solid var(--sport-border);border-radius:var(--radius-base);box-shadow:var(--shadow-lg);overflow:hidden}
.fs-body{padding:6px}
.fs-grp{font:600 var(--text-xs) var(--font-family);color:var(--sport-text-tertiary);padding:8px 8px 4px;text-transform:uppercase;letter-spacing:.04em}
.fs-item{display:flex;align-items:center;gap:8px;width:100%;padding:8px;border:none;border-radius:var(--radius-sm);background:0 0;color:var(--sport-text-primary);cursor:pointer;font:inherit;font-size:var(--text-base);transition:.15s;text-align:left}
.fs-item:hover{background:var(--sport-bg-elevated)}
.fs-active{background:var(--sport-primary-subtle);color:var(--sport-primary)}
.fs-ico{width:16px;height:16px;flex-shrink:0}
.fs-mav{width:22px;height:22px;border-radius:var(--radius-full);object-fit:cover;flex-shrink:0}
.fs-mav-fb{display:flex;align-items:center;justify-content:center;background:var(--sport-bg-elevated);color:var(--sport-text-secondary);font:700 var(--text-xs) var(--font-family)}
.fs-name{flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.fs-role{font-size:var(--text-xs);color:var(--sport-text-tertiary);flex-shrink:0}
.fs-footer{border-top:1px solid var(--sport-border-subtle);padding:6px}
.fs-manage{display:block;width:100%;padding:8px;border:none;border-radius:var(--radius-sm);background:0 0;color:var(--chart-sky);cursor:pointer;font:600 var(--text-sm) var(--font-family);text-align:center;transition:.15s}
.fs-manage:hover{background:rgba(56,189,248,.1)}
</style>
