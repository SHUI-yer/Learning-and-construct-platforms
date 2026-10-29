import { ROLES, type RoleCode } from '../constants/roles';

/**
 * 菜单项定义 —— 每条菜单可声明「哪些角色可见」
 * roles 为空数组或不填表示所有角色可见
 */
export interface MenuItem {
  key: string;
  label: string;
  path: string;
  icon?: string;
  /** 允许访问的角色列表；空 = 所有角色 */
  roles?: RoleCode[];
  children?: MenuItem[];
}

/**
 * 完整菜单配置（按顺序渲染）
 *
 * 规则：
 *   - super_admin / dept_admin 可以看到所有菜单
 *   - teacher / assistant 可以看到课程管理、教学工具、个人中心
 *   - student 只能看到我的课程、学习中心、个人中心
 */
export const MENU_CONFIG: MenuItem[] = [
  {
    key: 'home',
    label: '首页',
    path: '/',
    icon: 'HomeOutlined',
  },
  {
    key: 'courses',
    label: '课程管理',
    path: '/courses',
    icon: 'BookOutlined',
    roles: [ROLES.SUPER_ADMIN, ROLES.DEPT_ADMIN, ROLES.TEACHER, ROLES.ASSISTANT],
  },
  {
    key: 'my-courses',
    label: '我的课程',
    path: '/my-courses',
    icon: 'ReadOutlined',
    roles: [ROLES.STUDENT],
  },
  {
    key: 'learning',
    label: '学习中心',
    path: '/learning',
    icon: 'PlayCircleOutlined',
    roles: [ROLES.STUDENT],
  },
  {
    key: 'assessment',
    label: '任务与考核',
    path: '/assessment',
    icon: 'FormOutlined',
    roles: [ROLES.SUPER_ADMIN, ROLES.DEPT_ADMIN, ROLES.TEACHER, ROLES.ASSISTANT],
    children: [
      {
        key: 'assessment-tasks',
        label: '任务与发布',
        path: '/assessment/tasks',
        icon: 'FormOutlined',
      },
      {
        key: 'assessment-questions',
        label: '题库管理',
        path: '/assessment/questions',
        icon: 'EditOutlined',
      },
      {
        key: 'assessment-papers',
        label: '试卷管理',
        path: '/assessment/papers',
        icon: 'BookOutlined',
      },
      {
        key: 'assessment-grades',
        label: '成绩管理',
        path: '/assessment/grades',
        icon: 'ReadOutlined',
      },
      {
        key: 'assessment-appeals',
        label: '申诉处理',
        path: '/assessment/appeals',
        icon: 'ToolOutlined',
      },
    ],
  },
  {
    key: 'my-assessment',
    label: '任务与考核',
    path: '/my-assessment/tasks',
    icon: 'FormOutlined',
    roles: [ROLES.STUDENT],
  },
  {
    key: 'tools',
    label: '教学工具',
    path: '/tools',
    icon: 'ToolOutlined',
    roles: [ROLES.SUPER_ADMIN, ROLES.DEPT_ADMIN, ROLES.TEACHER, ROLES.ASSISTANT],
    children: [
      {
        key: 'notebook',
        label: '智能笔记',
        path: '/tools/notebook',
        icon: 'EditOutlined',
      },
      {
        key: 'mindmap',
        label: '思维导图',
        path: '/tools/mindmap',
        icon: 'DeploymentUnitOutlined',
      },
    ],
  },
  {
    key: 'admin',
    label: '系统管理',
    path: '/admin',
    icon: 'SettingOutlined',
    roles: [ROLES.SUPER_ADMIN, ROLES.DEPT_ADMIN],
    children: [
      {
        key: 'user-manage',
        label: '用户管理',
        path: '/admin/users',
        icon: 'UserOutlined',
      },
      {
        key: 'role-manage',
        label: '角色管理',
        path: '/admin/roles',
        icon: 'TeamOutlined',
      },
    ],
  },
  {
    key: 'profile',
    label: '个人中心',
    path: '/profile',
    icon: 'UserOutlined',
  },
];

/**
 * 根据角色过滤菜单
 */
export function filterMenuByRole(menus: MenuItem[], roleCode: RoleCode | null): MenuItem[] {
  if (!roleCode) return [];
  return menus
    .filter((item) => {
      // roles 未定义或为空数组 → 所有角色可见
      if (!item.roles || item.roles.length === 0) return true;
      return item.roles.includes(roleCode);
    })
    .map((item) => {
      if (item.children) {
        return { ...item, children: filterMenuByRole(item.children, roleCode) };
      }
      return item;
    })
    // 过滤后如果子菜单全部被移除，且父菜单本身也有角色限制，则隐藏父菜单
    .filter((item) => {
      if (item.children && item.children.length === 0) {
        // 有 children 定义但过滤后为空 → 隐藏
        return false;
      }
      return true;
    });
}

/**
 * 额外路由权限 —— 用于不在菜单中但需要权限控制的路由
 * 支持前缀匹配：'/courses' 会匹配 /courses、/courses/123 等
 * 未列出的路由默认所有角色可访问（白名单模式）
 */
export const ROUTE_PERMISSIONS: { path: string; roles: RoleCode[] }[] = [
  // 课程详情：所有角色可访问（学生看自己的课程，教师管理自己的课程）
  // 此处无需额外配置，因为 /courses 已对所有角色开放
  // 如果将来需要限制，可在此添加

  // 学生考核区：菜单项只登记了 /my-assessment/tasks，
  // 考试页 /my-assessment/exam/:taskId、成绩页、申诉页不在其前缀下，需在此显式放行
  { path: '/my-assessment', roles: [ROLES.STUDENT] },
];

/**
 * 检查某个角色是否可以访问指定路径
 * 支持精确匹配和前缀匹配（/courses 匹配 /courses/123）
 */
export function canAccessPath(path: string, roleCode: RoleCode | null): boolean {
  if (!roleCode) return false;

  // 先检查额外路由权限配置
  for (const perm of ROUTE_PERMISSIONS) {
    if (path === perm.path || path.startsWith(perm.path + '/')) {
      return perm.roles.includes(roleCode);
    }
  }

  // 再检查菜单配置（支持前缀匹配）
  const check = (items: MenuItem[]): boolean => {
    for (const item of items) {
      // 父菜单角色不符 → 其子菜单同样不可访问（子菜单通常不重复声明 roles，
      // 直接下钻会让学生凭 URL 打开教师端页面）
      if (item.roles && item.roles.length > 0 && !item.roles.includes(roleCode)) {
        continue;
      }
      // 精确匹配 或 前缀匹配（如 /courses 匹配 /courses/123）
      const pathMatch = item.path === path || (item.path !== '/' && path.startsWith(item.path + '/'));
      if (pathMatch) return true;
      if (item.children && check(item.children)) return true;
    }
    return false;
  };

  return check(MENU_CONFIG);
}

/**
 * 检查当前角色是否拥有管理员权限（super_admin 或 dept_admin）
 */
export function isAdmin(roleCode: RoleCode | null): boolean {
  return roleCode === ROLES.SUPER_ADMIN || roleCode === ROLES.DEPT_ADMIN;
}

/**
 * 检查当前角色是否为教师或以上（含管理员、教师）
 */
export function isTeacherOrAbove(roleCode: RoleCode | null): boolean {
  if (!roleCode) return false;
  return [ROLES.SUPER_ADMIN, ROLES.DEPT_ADMIN, ROLES.TEACHER, ROLES.ASSISTANT].includes(roleCode as any);
}
