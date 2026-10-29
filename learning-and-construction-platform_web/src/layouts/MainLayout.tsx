import { useState, useEffect, useMemo } from 'react';
import { Outlet, useNavigate, useLocation, Navigate } from 'react-router-dom';
import { Suspense } from 'react';
import { Layout, Menu, Dropdown, Avatar, Space, ConfigProvider } from 'antd';
import type { MenuProps } from 'antd';
import {
  HomeOutlined,
  BookOutlined,
  ReadOutlined,
  PlayCircleOutlined,
  FormOutlined,
  ToolOutlined,
  EditOutlined,
  DeploymentUnitOutlined,
  SettingOutlined,
  UserOutlined,
  TeamOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import zhCN from 'antd/locale/zh_CN';
import { useAuth } from '../hooks/useAuth';
import { useAuthStore } from '../store/authStore';
import { MENU_CONFIG, filterMenuByRole, canAccessPath } from '../utils/permission';
import type { MenuItem } from '../utils/permission';
import type { RoleCode } from '../constants/roles';
import { ROLE_LABELS } from '../constants/roles';

const { Header, Sider, Content } = Layout;

/** 图标名称 → 组件映射 */
const ICON_MAP: Record<string, React.ReactNode> = {
  HomeOutlined: <HomeOutlined />,
  BookOutlined: <BookOutlined />,
  ReadOutlined: <ReadOutlined />,
  PlayCircleOutlined: <PlayCircleOutlined />,
  FormOutlined: <FormOutlined />,
  ToolOutlined: <ToolOutlined />,
  EditOutlined: <EditOutlined />,
  DeploymentUnitOutlined: <DeploymentUnitOutlined />,
  SettingOutlined: <SettingOutlined />,
  UserOutlined: <UserOutlined />,
  TeamOutlined: <TeamOutlined />,
};

/** 将菜单配置转为 Ant Design Menu 的 items 格式 */
function buildMenuItems(menus: MenuItem[]): MenuProps['items'] {
  return menus.map((item) => {
    if (item.children && item.children.length > 0) {
      return {
        key: item.path,
        icon: item.icon ? ICON_MAP[item.icon] : null,
        label: item.label,
        children: buildMenuItems(item.children),
      };
    }
    return {
      key: item.path,
      icon: item.icon ? ICON_MAP[item.icon] : null,
      label: item.label,
    };
  });
}

/** 根据当前路径找到应该展开的父菜单 key */
function getOpenKeys(pathname: string): string[] {
  const openKeys: string[] = [];
  for (const item of MENU_CONFIG) {
    if (item.children) {
      for (const child of item.children) {
        if (pathname.startsWith(child.path)) {
          openKeys.push(item.path);
        }
      }
    }
  }
  return openKeys;
}

export default function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, isAuthenticated, initialized, logout, roleCode } = useAuth();
  const restore = useAuthStore((s) => s.restore);
  const [collapsed, setCollapsed] = useState(false);
  const [openKeys, setOpenKeys] = useState<string[]>([]);

  // 应用启动时从 localStorage 恢复认证状态
  useEffect(() => {
    if (!initialized) {
      restore();
    }
  }, [initialized, restore]);

  // 路径变化时自动展开对应父菜单
  useEffect(() => {
    const keys = getOpenKeys(location.pathname);
    setOpenKeys((prev) => {
      const merged = new Set([...prev, ...keys]);
      return [...merged];
    });
  }, [location.pathname]);

  // 根据角色过滤菜单
  const menuItems = useMemo(
    () => buildMenuItems(filterMenuByRole(MENU_CONFIG, roleCode as RoleCode | null)),
    [roleCode],
  );

  // 未初始化时显示加载中
  if (!initialized) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
        <span style={{ color: '#999', fontSize: 16 }}>加载中...</span>
      </div>
    );
  }

  // 未认证 → 跳转登录页
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // 权限检查：当前角色不能访问当前路径 → 重定向到首页
  if (!canAccessPath(location.pathname, roleCode as RoleCode | null)) {
    // 首页对所有角色开放，避免死循环
    if (location.pathname !== '/') {
      return <Navigate to="/" replace />;
    }
  }

  // 用户下拉菜单
  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/profile'),
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      danger: true,
      onClick: () => {
        logout();
        navigate('/login', { replace: true });
      },
    },
  ];

  const roleLabel = roleCode ? ROLE_LABELS[roleCode as RoleCode] : '';

  return (
    <ConfigProvider locale={zhCN}>
      <Layout style={{ minHeight: '100vh' }}>
        {/* ===== 侧边栏 ===== */}
        <Sider
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          trigger={null}
          width={220}
          style={{
            overflow: 'auto',
            height: '100vh',
            position: 'fixed',
            left: 0,
            top: 0,
            bottom: 0,
            background: '#001529',
          }}
        >
          {/* Logo 区域 */}
          <div
            style={{
              height: 64,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              borderBottom: '1px solid rgba(255,255,255,0.08)',
            }}
          >
            <h1
              style={{
                color: '#fff',
                fontSize: collapsed ? 16 : 18,
                fontWeight: 700,
                margin: 0,
                whiteSpace: 'nowrap',
                letterSpacing: '0.05em',
                transition: 'font-size 0.2s',
              }}
            >
              {collapsed ? '智学' : '智学汇平台'}
            </h1>
          </div>

          {/* 导航菜单 */}
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[location.pathname]}
            openKeys={openKeys}
            onOpenChange={setOpenKeys}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            style={{ borderRight: 0, marginTop: 8 }}
          />
        </Sider>

        {/* ===== 右侧区域 ===== */}
        <Layout style={{ marginLeft: collapsed ? 80 : 220, transition: 'margin-left 0.2s' }}>
          {/* 顶部栏 */}
          <Header
            style={{
              padding: '0 24px',
              background: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
              position: 'sticky',
              top: 0,
              zIndex: 10,
            }}
          >
            {/* 左侧折叠按钮 */}
            <div
              onClick={() => setCollapsed(!collapsed)}
              style={{ fontSize: 18, cursor: 'pointer', color: '#595959', lineHeight: 1 }}
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </div>

            {/* 右侧用户信息 */}
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" arrow>
              <Space style={{ cursor: 'pointer' }}>
                <Avatar
                  size={32}
                  icon={<UserOutlined />}
                  style={{ backgroundColor: '#1677ff' }}
                />
                <div style={{ lineHeight: 1.3 }}>
                  <div style={{ fontSize: 14, fontWeight: 500, color: '#262626' }}>
                    {user?.realName || user?.username || '用户'}
                  </div>
                  <div style={{ fontSize: 12, color: '#8c8c8c' }}>{roleLabel}</div>
                </div>
              </Space>
            </Dropdown>
          </Header>

          {/* 内容区域 */}
          <Content
            style={{
              margin: 24,
              minHeight: 'calc(100vh - 64px - 48px)',
            }}
          >
            <Suspense
              fallback={
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 80 }}>
                  <span style={{ color: '#999' }}>页面加载中...</span>
                </div>
              }
            >
              <Outlet />
            </Suspense>
          </Content>
        </Layout>
      </Layout>
    </ConfigProvider>
  );
}
