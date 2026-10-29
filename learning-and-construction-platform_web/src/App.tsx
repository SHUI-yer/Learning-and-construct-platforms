import { useEffect } from 'react';
import AppRouter from './router';
import { queryClient } from './utils/queryClient';
import { QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from './store/authStore';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';

function App() {
  const restore = useAuthStore((s) => s.restore);
  const initialized = useAuthStore((s) => s.initialized);

  // 应用启动时立即恢复认证状态
  useEffect(() => {
    restore();
  }, [restore]);

  // 等待认证状态恢复完成后再渲染路由，避免闪烁
  if (!initialized) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        background: '#f5f5f5',
      }}>
        <span style={{ color: '#999', fontSize: 16 }}>正在初始化...</span>
      </div>
    );
  }

  return (
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={queryClient}>
        <AppRouter />
      </QueryClientProvider>
    </ConfigProvider>
  );
}

export default App;
