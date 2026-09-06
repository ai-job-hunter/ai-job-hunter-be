# AI Job Hunter - 前端规范

> 版本：V1.0
> 日期：2026-09-02
> 状态：草稿
> 负责人：待定

## [变更记录]
| 日期 | 版本 | 变更摘要 | 负责人 |
|---|---|---|-----|
| 2026-09-02 | v1.0 | 初始版本（Vue 3 + Nuxt 3 + shadcn-vue） | - |

---

# 一、技术选型

## 1.1 核心框架

| 技术 | 版本 | 说明 |
|-----|------|------|
| Nuxt | 3.x | Vue 3 全栈框架 |
| Vue | 3.4+ | 渐进式前端框架 |
| TypeScript | 5.0+ | 类型安全 |
| shadcn-vue | latest | 组件库（个人品牌风格） |
| Tailwind CSS | 3.0+ | CSS 框架 |
| Pinia | 2.1+ | 状态管理 |
| VueUse | 10.0+ | Vue 组合式工具集 |

## 1.2 辅助工具

| 技术 | 用途 |
|-----|------|
| Lucide Vue Next | 图标库 |
| dayjs | 日期处理 |

## 1.3 shadcn-vue vs Naive UI

| 特性 | shadcn-vue | Naive UI |
|-----|------------|----------|
| 定制性 | 极高，可直接修改源码 | 有限，通过主题配置 |
| 包大小 | 按需引入，体积小 | 整体引入，体积大 |
| 样式控制 | Tailwind CSS，完全掌控 | Less/CSS，封装较好 |
| 学习曲线 | 需要了解 Tailwind | 上手快 |
| 适用场景 | 个人品牌、高度定制 | 企业级快速开发 |

> **选择理由**：AI Job Hunter 是个人品牌项目，shadcn-vue 的高度可定制性更适合打造独特的视觉效果。

---

# 二、项目结构

```
ai-job-hunter-fe/
├── assets/
│   └── css/
│       └── main.css              # 全局样式 + Tailwind
├── components/
│   ├── common/                   # 通用组件
│   ├── layout/                   # 布局组件
│   └── ui/                      # shadcn-vue 组件
├── composables/                  # 组合式函数
│   ├── useAuth.ts              # 认证
│   ├── useApi.ts               # API 调用
│   └── useConfirm.ts           # 确认对话框
├── layouts/                     # 页面布局
│   ├── default.vue              # 主布局（侧边栏 + 顶栏）
│   └── auth.vue                # 认证布局（居中卡片）
├── middleware/                  # 中间件
│   └── auth.ts                 # 路由守卫
├── pages/                       # 页面
│   ├── index.vue               # 仪表盘
│   ├── login.vue               # 登录
│   ├── register.vue            # 注册
│   ├── resume/                 # 简历相关
│   ├── job/                    # 岗位相关
│   ├── conversation/           # 沟通相关
│   ├── application/            # 投递相关
│   ├── strategy/               # 策略相关
│   ├── stats/                  # 统计相关
│   └── settings/               # 设置相关
├── server/                      # 服务端（Nuxt）
│   └── api/                    # 服务端 API 路由
├── stores/                     # Pinia 状态
│   └── user.ts
├── types/                      # TypeScript 类型
│   └── index.ts
├── utils/                      # 工具函数
│   └── format.ts
├── app.vue                      # 根组件
├── nuxt.config.ts
├── tailwind.config.ts
├── package.json
└── tsconfig.json
```

---

# 三、页面设计

## 3.1 页面清单

| 页面 | 路由 | 说明 |
|-----|------|------|
| 登录 | `/login` | 用户登录 |
| 注册 | `/register` | 用户注册 |
| **首页仪表盘** | `/` | 今日概览、快速入口 |
| **简历管理** | `/resume` | 简历列表 |
| 简历详情 | `/resume/:id` | 简历详情和分析 |
| 简历上传 | `/resume/upload` | 上传新简历 |
| 简历分析 | `/resume/:id/analysis` | 分析报告 |
| **岗位管理** | `/job` | 岗位列表 |
| 岗位详情 | `/job/:id` | 岗位详情和 JD 分析 |
| **沟通管理** | `/conversation` | 对话列表 |
| 聊天详情 | `/conversation/:id` | 聊天界面 |
| **投递管理** | `/application` | 投递列表 |
| 投递看板 | `/application/kanban` | Kanban 视图 |
| 投递详情 | `/application/:id` | 投递详情和时间线 |
| **策略设置** | `/strategy` | 求职策略 |
| 话术模板 | `/strategy/templates` | 打招呼模板管理 |
| 黑名单 | `/strategy/blacklist` | 黑名单管理 |
| **数据统计** | `/stats` | 统计页面 |
| 周报 | `/stats/weekly-report` | 周报页面 |
| **系统设置** | `/settings` | 用户设置 |
| BOSS 连接 | `/settings/boss` | BOSS 账号连接 |

## 3.2 页面布局

### 3.2.1 主布局结构

```
┌────────────────────────────────────────────────────────────┐
│                        Header                               │
│  Logo    导航菜单                        用户信息  设置       │
├──────────┬─────────────────────────────────────────────────┤
│          │                                                   │
│          │                    Main                          │
│ Sidebar  │              (页面内容区域)                       │
│          │                                                   │
│          │                                                   │
│          │                                                   │
├──────────┴─────────────────────────────────────────────────┤
│                        Footer                               │
│  版权信息                        技术支持                    │
└────────────────────────────────────────────────────────────┘
```

### 3.2.2 侧边栏菜单

```
📊 Dashboard
   └── 今日概览

📄 简历管理
   ├── 简历列表
   └── 上传简历

💼 岗位管理
   └── 岗位列表

💬 沟通管理
   └── 对话列表

📤 投递管理
   ├── 投递列表
   └── 投递看板

⚙️ 策略设置
   ├── 求职偏好
   ├── 话术模板
   └── 黑名单

📈 数据统计
   └── 统计报表

🔧 系统设置
   └── BOSS 连接
```

---

# 四、核心页面设计

## 4.1 首页仪表盘

### 4.1.1 布局结构

```
┌─────────────────────────────────────────────────────────────┐
│  👋 欢迎回来，张三！                              2026-09-02 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ 今日打招呼 │ │ 今日回复  │ │ 今日投递  │ │ 待处理   │      │
│  │    15    │ │    8     │ │    5     │ │    3    │      │
│  │  ↑ 比昨日 │ │  ↑ 比昨日 │ │  ↑ 比昨日 │ │ 约面 2  │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  📈 求职漏斗                                          ││
│  │  打招呼 150 → 回复 60 → 投递 40 → 面试 10 → Offer 2   ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
│  ┌────────────────────────┐ ┌────────────────────────────┐│
│  │  💬 待回复对话 (5)       │ │  📋 近期投递 (3)          ││
│  │  ┌────────────────────┐ │ │  ┌────────────────────────┐││
│  │  │ XX公司 HR 李xx     │ │ │  │ XX科技 - Java工程师   │││
│  │  │ "您方便说下期望..."  │ │ │  │ 状态：约面中 | 匹配85%│││
│  │  └────────────────────┘ │ │  └────────────────────────┘││
│  │  ┌────────────────────┐ │ │  ┌────────────────────────┐││
│  │  │ YY公司 HR 王xx     │ │ │  │ YY集团 - 后端开发     │││
│  │  │ "下周三方便面试吗..." │ │ │  │ 状态：已投递 | 匹配78%│││
│  │  └────────────────────┘ │ │  └────────────────────────┘││
│  └────────────────────────┘ └────────────────────────────┘│
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  ⚡ 快捷操作                                          ││
│  │  [上传简历] [设置求职偏好] [查看统计] [生成周报]        ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 4.1.2 组件说明

| 组件 | 说明 |
|-----|------|
| Card | 卡片容器（shadcn Card） |
| Button | 按钮（shadcn Button） |
| StatCard | 自定义统计卡片，显示数字和趋势 |

## 4.2 简历管理页面

### 4.2.1 布局结构

```
┌─────────────────────────────────────────────────────────────┐
│  📄 简历管理                              [+ 上传简历]      │
├─────────────────────────────────────────────────────────────┤
│  [全部] [技术] [管理] [自定义]    🔍 搜索简历              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌───────────────────┐ ┌───────────────────┐               │
│  │  📄 技术简历V2     │ │  📄 技术简历V1     │               │
│  │  默认简历          │ │                   │               │
│  │  匹配度最高的岗位： │ │  匹配度最高的岗位： │               │
│  │  Java工程师 85%   │ │  后端开发 78%     │               │
│  │                   │ │                   │               │
│  │  ✅ 已分析        │ │  ✅ 已分析        │               │
│  │  [查看] [分析] [优化]│ │  [查看] [分析]     │               │
│  └───────────────────┘ └───────────────────┘               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2.2 组件使用

```vue
<script setup lang="ts">
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>简历标题</CardTitle>
      <CardDescription>简历描述</CardDescription>
    </CardHeader>
    <CardContent>
      <p>简历内容...</p>
      <Button>操作</Button>
    </CardContent>
  </Card>
</template>
```

## 4.3 岗位管理页面

### 4.3.1 布局结构

```
┌─────────────────────────────────────────────────────────────┐
│  💼 岗位管理                                    [搜索岗位]  │
├─────────────────────────────────────────────────────────────┤
│  状态: [全部▼] [新发现] [已分析] [已打招呼] [已投递]        │
│  城市: [全部▼] [北京] [上海] [深圳]                        │
│  匹配度: [全部▼] [90%+] [80%+] [70%+] [60%+]              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ ⭐ Java高级工程师                    公司：XX科技       ││
│  │ 📍 北京·朝阳  💰 30k-50k·14薪  📊 匹配度：85%        ││
│  │ 📋 关键词：Java / Spring / 微服务 / MySQL             ││
│  │                                                       ││
│  │ 状态：[已打招呼]  HR：李HR  💬 3条消息                ││
│  │                                                       ││
│  │                    [查看详情] [重新分析] [打招呼]       ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 4.4 沟通管理页面

### 4.4.1 布局结构

```
┌─────────────────────────────────────────────────────────────┐
│  💬 沟通管理                              [筛选：全部▼]      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌────────────────────┬────────────────────────────────────┐
│  │      对话列表        │           聊天详情                 │
│  │                    │                                    │
│  │ ┌────────────────┐ │  ┌─ XX科技 - Java工程师 ──────────┐│
│  │ │ 李HR    刚刚   │ │  │                                  ││
│  │ │ [未读]        │ │  │         👤 我                    ││
│  │ │ "您方便说下..." │ │  │  您好！看到贵司在招聘...         ││
│  │ └────────────────┘ │  │                        09:00  ││
│  │ ┌────────────────┐ │  │                                  ││
│  │ │ 王HR    2小时前 │ │  │         👤 李HR                  ││
│  │ │ "下周三方便..." │ │  │  您方便说下期望薪资吗？           ││
│  │ └────────────────┘ │  │                        10:00  ││
│  │                    │  │                                  ││
│  │                    │  │  ┌────────────────────────────┐ ││
│  │                    │  │  │ 💡 AI 推荐回复              │ ││
│  │                    │  │  │                            │ ││
│  │                    │  │  │ ① 我的期望薪资是30-35k...  │ ││
│  │                    │  │  │                           │ ││
│  │                    │  │  │ ② 我目前在职，希望涨幅20%..│ ││
│  │                    │  │  │                           │ ││
│  │                    │  │  │         [发送] [复制] [编辑]│ ││
│  │                    │  │  └────────────────────────────┘ ││
│  └────────────────────┴────────────────────────────────────┘
└─────────────────────────────────────────────────────────────┘
```

## 4.5 投递看板页面

### 4.5.1 Kanban 布局

```
┌─────────────────────────────────────────────────────────────┐
│  📤 投递看板                          [列表视图] [看板视图]   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────┐ │
│  │ 已投递   │ │ 已查看   │ │ HR沟通中 │ │  面试中  │ │Offer│ │
│  │   (8)    │ │   (5)    │ │   (3)    │ │   (2)   │ │ (1) │ │
│  ├──────────┤ ├──────────┤ ├──────────┤ ├──────────┤ ├────┤ │
│  │          │ │          │ │          │ │          │ │    │ │
│  │ ┌──────┐ │ │ ┌──────┐ │ │ ┌──────┐ │ │ ┌──────┐ │ │┌──┐│ │
│  │ │XX科技│ │ │ │YY集团│ │ │ │ZZ公司│ │ │ │AA公司│ │ ││BB││ │
│  │ │Java │ │ │ │后端 │ │ │ │架构 │ │ │ │Java │ │ ││公司││ │
│  │ │85%  │ │ │ │78%  │ │ │ │72%  │ │ │ │90%  │ │ │└──┘│ │
│  │ │投递1天│ │ │ │查看2天│ │ │ │沟通3天│ │ │ │约面1天│ │ │Offer│ │
│  │ └──────┘ │ │ └──────┘ │ │ └──────┘ │ │ └──────┘ │ │已接收│ │
│  │          │ │          │ │          │ │          │ │    │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

# 五、组件规范

## 5.1 shadcn-vue 组件清单

### 基础组件

| 组件 | 用途 |
|-----|------|
| Button | 按钮 |
| Card | 卡片 |
| Input | 输入框 |
| Label | 标签 |
| Dialog | 对话框 |
| DropdownMenu | 下拉菜单 |
| Select | 选择器 |
| Textarea | 多行文本框 |
| Avatar | 头像 |
| Badge | 徽章 |
| Separator | 分隔线 |
| Sheet | 侧边抽屉 |
| Skeleton | 加载骨架 |
| Toast | 提示 |
| Tooltip | 工具提示 |

### 表单组件

| 组件 | 用途 |
|-----|------|
| Form | 表单（配合 Input、Select 等使用） |
| Checkbox | 复选框 |
| RadioGroup | 单选组 |
| Switch | 开关 |
| Slider | 滑块 |

## 5.2 添加 shadcn-vue 组件

```bash
# 添加 Button
npx shadcn-vue@latest add button

# 添加 Card
npx shadcn-vue@latest add card

# 添加 Dialog
npx shadcn-vue@latest add dialog
```

## 5.3 组件使用示例

```vue
<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
</script>

<template>
  <div class="space-y-4">
    <Card>
      <CardHeader>
        <CardTitle>卡片标题</CardTitle>
      </CardHeader>
      <CardContent class="space-y-4">
        <div class="space-y-2">
          <Label for="email">邮箱</Label>
          <Input id="email" type="email" placeholder="your@email.com" />
        </div>
        <Button>提交</Button>
      </CardContent>
    </Card>
  </div>
</template>
```

## 5.4 自定义组件规范

```vue
<script setup lang="ts">
interface Props {
  title: string
  value: number | string
  trend?: number
  icon?: string
}

const props = withDefaults(defineProps<Props>(), {
  trend: 0
})
</script>

<template>
  <Card>
    <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
      <CardTitle class="text-sm font-medium">{{ title }}</CardTitle>
      <component :is="icon" class="h-4 w-4 text-muted-foreground" />
    </CardHeader>
    <CardContent>
      <div class="text-2xl font-bold">{{ value }}</div>
      <p v-if="trend !== 0" class="text-xs" :class="trend > 0 ? 'text-green-500' : 'text-red-500'">
        {{ trend > 0 ? '+' : '' }}{{ trend }}% 较昨日
      </p>
    </CardContent>
  </Card>
</template>
```

---

# 六、状态管理

## 6.1 Pinia Store 结构

```typescript
// stores/user.ts
import { defineStore } from 'pinia'
import type { User } from '~/types'

export const useUserStore = defineStore('user', () => {
  // State
  const user = ref<User | null>(null)
  const token = ref<string>('')
  const bossConnected = ref(false)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => user.value?.username || '')

  // Actions
  async function login(email: string, password: string) {
    // 登录逻辑
  }

  async function logout() {
    token.value = ''
    user.value = null
    navigateTo('/login')
  }

  return {
    user,
    token,
    bossConnected,
    isLoggedIn,
    username,
    login,
    logout
  }
})
```

## 6.2 Store 列表

| Store | 说明 |
|-------|------|
| `useUserStore` | 用户信息、登录状态 |
| `useResumeStore` | 简历列表、当前简历 |
| `useJobStore` | 岗位列表、筛选条件 |
| `useConversationStore` | 对话列表、当前对话 |
| `useApplicationStore` | 投递记录 |

---

# 七、API 调用

## 7.1 composables/useApi.ts

```typescript
// composables/useApi.ts
export const useApi = () => {
  const config = useRuntimeConfig()

  const request = <T>(url: string, options?: RequestInit): Promise<T> => {
    const token = useUserStore().token

    return $fetch<T>(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options?.headers
      }
    })
  }

  return {
    get: <T>(url: string, params?: Record<string, any>) =>
      request<T>(url, { method: 'GET', params }),
    post: <T>(url: string, data?: any) =>
      request<T>(url, { method: 'POST', body: data }),
    put: <T>(url: string, data?: any) =>
      request<T>(url, { method: 'PUT', body: data }),
    delete: <T>(url: string) =>
      request<T>(url, { method: 'DELETE' })
  }
}
```

## 7.2 使用示例

```typescript
// 在组件中使用
const api = useApi()

// GET 请求
const { data } = await useAsyncData('jobs', () => 
  api.get<PageResult<Job>>('/jobs', { page: 1, pageSize: 20 })
)

// POST 请求
const response = await api.post<ApiResponse<Resume>>('/resumes/upload', formData)
```

---

# 八、样式规范

## 8.1 Tailwind CSS 变量

```css
/* assets/css/main.css */
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    --primary: 221.2 83.2% 53.3%;
    /* ... 更多 CSS 变量 */
  }
}
```

## 8.2 常用样式类

| 类 | 用途 |
|---|------|
| `space-y-4` | 垂直间距 |
| `grid gap-4` | 网格间距 |
| `flex items-center` | 居中对齐 |
| `text-sm text-muted-foreground` | 次要文本 |
| `rounded-lg` | 圆角 |
| `shadow-sm` | 阴影 |

## 8.3 响应式断点

| 断点 | 类前缀 | 屏幕宽度 |
|-----|-------|---------|
| xs | - | < 576px |
| sm | `sm:` | ≥ 576px |
| md | `md:` | ≥ 768px |
| lg | `lg:` | ≥ 992px |
| xl | `xl:` | ≥ 1200px |

---

# 九、路由规范

## 9.1 路由配置

```typescript
// pages 目录自动生成路由
// pages/index.vue → /
// pages/login.vue → /login
// pages/resume/index.vue → /resume
```

## 9.2 路由守卫

```typescript
// middleware/auth.ts
export default defineNuxtRouteMiddleware((to) => {
  const { isLoggedIn } = useAuth()

  const requiresAuth = ['/', '/resume', '/job', '/conversation', '/application']

  if (requiresAuth.some(path => to.path === path) && !isLoggedIn.value) {
    return navigateTo(`/login?redirect=${to.fullPath}`)
  }
})
```

---

# 十、图标使用

使用 Lucide Vue Next：

```vue
<script setup lang="ts">
import { 
  LayoutDashboard, 
  FileText, 
  Briefcase, 
  MessageSquare, 
  Send,
  Settings,
  BarChart3,
  LogOut,
  Upload,
  Search,
  Bell
} from 'lucide-vue-next'
</script>

<template>
  <Button>
    <Upload class="mr-2 h-4 w-4" />
    上传
  </Button>
</template>
```

---

# 十一、环境变量

## 11.1 环境变量配置

```env
# .env
NUXT_PUBLIC_API_BASE=http://localhost:8080/api/v1
```

## 11.2 使用

```typescript
const config = useRuntimeConfig()
const apiBase = config.public.apiBase
```

---

# 十二、性能优化

## 12.1 路由懒加载

Nuxt 自动进行代码分割，无需额外配置。

## 12.2 图片优化

```vue
<template>
  <NuxtImg 
    src="/images/logo.png" 
    width="200" 
    height="200"
    format="webp"
  />
</template>
```

## 12.3 组件按需引入

shadcn-vue 组件按需引入，自动优化包大小。

---

# 十三、shadcn-vue 组件路径

```
components/
├── ui/                    # shadcn-vue 组件
│   ├── button/
│   │   ├── Button.vue
│   │   └── index.ts
│   ├── card/
│   │   ├── Card.vue
│   │   ├── CardHeader.vue
│   │   ├── CardTitle.vue
│   │   ├── CardContent.vue
│   │   └── index.ts
│   ├── input/
│   │   └── ...
│   └── ...
```

---

# 十四、开发规范

## 14.1 命名规范

| 类型 | 规范 | 示例 |
|-----|------|------|
| 页面组件 | PascalCase | `DashboardPage.vue` |
| 布局组件 | PascalCase | `AppSidebar.vue` |
| 通用组件 | PascalCase | `StatCard.vue` |
| composables | camelCase | `useAuth.ts` |
| stores | camelCase | `user.ts` |
| 类型 | PascalCase | `UserInfo.ts` |

## 14.2 代码格式

- 使用 TypeScript
- 使用 `<script setup lang="ts">`
- 使用 Composition API
- 使用 Tailwind CSS 代替 SCSS

## 14.3 提交规范

```
feat: 新功能
fix: 修复
docs: 文档
refactor: 重构
test: 测试
chore: 任务
```
