import { useAuth } from '../hooks/useAuth';
import { ROLE_LABELS, type RoleCode } from '../constants/roles';
import {
  UserOutlined,
  MailOutlined,
  PhoneOutlined,
  TeamOutlined,
  ClockCircleOutlined,
  EditOutlined,
} from '@ant-design/icons';
import { Card, Avatar, Descriptions, Tag, Button, Space, Typography, Divider } from 'antd';

const { Title, Text } = Typography;

export default function Profile() {
  const { user, roleCode, realName, username } = useAuth();

  const roleLabel = roleCode ? ROLE_LABELS[roleCode as RoleCode] : '未知角色';

  /** 角色标签颜色 */
  const roleColorMap: Record<string, string> = {
    super_admin: 'red',
    dept_admin: 'orange',
    teacher: 'blue',
    assistant: 'cyan',
    student: 'green',
  };

  return (
    <div style={{ maxWidth: 900, margin: '0 auto' }}>
      <Title level={4} style={{ marginBottom: 24 }}>
        个人中心
      </Title>

      {/* ===== 用户信息卡片 ===== */}
      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 32 }}>
          {/* 头像 */}
          <Avatar
            size={96}
            icon={<UserOutlined />}
            style={{
              backgroundColor: '#1677ff',
              flexShrink: 0,
              fontSize: 36,
            }}
          >
            {realName ? realName.charAt(0) : undefined}
          </Avatar>

          {/* 基本信息 */}
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <Title level={4} style={{ margin: 0 }}>
                {realName || username}
              </Title>
              <Tag color={roleColorMap[roleCode || ''] || 'default'}>{roleLabel}</Tag>
            </div>
            <Space direction="vertical" size={4}>
              <Text type="secondary">
                <UserOutlined style={{ marginRight: 8 }} />
                账号：{username}
              </Text>
              <Text type="secondary">
                <TeamOutlined style={{ marginRight: 8 }} />
                角色等级：{user?.roleLevel ?? '-'}
              </Text>
            </Space>
          </div>

          {/* 编辑按钮（预留） */}
          <Button icon={<EditOutlined />} disabled>
            编辑资料
          </Button>
        </div>
      </Card>

      {/* ===== 详细资料 ===== */}
      <Card title="详细资料" style={{ marginBottom: 24 }}>
        <Descriptions column={2} labelStyle={{ fontWeight: 500 }}>
          <Descriptions.Item label="用户ID">{user?.userId ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="用户名">{username || '-'}</Descriptions.Item>
          <Descriptions.Item label="姓名">{realName || '-'}</Descriptions.Item>
          <Descriptions.Item label="角色">
            <Tag color={roleColorMap[roleCode || ''] || 'default'}>{roleLabel}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="邮箱">
            <MailOutlined style={{ marginRight: 8 }} />
            暂未设置
          </Descriptions.Item>
          <Descriptions.Item label="手机号">
            <PhoneOutlined style={{ marginRight: 8 }} />
            暂未设置
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* ===== 安全设置（预留） ===== */}
      <Card title="安全设置">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Text strong>登录密码</Text>
            <br />
            <Text type="secondary">定期修改密码有助于保护账户安全</Text>
          </div>
          <Button disabled>修改密码</Button>
        </div>
        <Divider />
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Text strong>登录设备</Text>
            <br />
            <Text type="secondary">
              <ClockCircleOutlined style={{ marginRight: 8 }} />
              当前设备 · Windows
            </Text>
          </div>
          <Button disabled>管理设备</Button>
        </div>
      </Card>

      {/* 提示 */}
      <div style={{ marginTop: 16, textAlign: 'center' }}>
        <Text type="secondary" style={{ fontSize: 12 }}>
          个人中心部分功能待后端接口就绪后开放
        </Text>
      </div>
    </div>
  );
}
