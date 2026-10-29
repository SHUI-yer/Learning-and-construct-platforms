import { useAuth } from '../hooks/useAuth';
import { ROLE_LABELS, type RoleCode } from '../constants/roles';
import { useNavigate } from 'react-router-dom';
import { Card, Typography, Space, Tag, Avatar, Button, Row, Col, Statistic } from 'antd';
import {
  UserOutlined,
  BookOutlined,
  ClockCircleOutlined,
  TrophyOutlined,
} from '@ant-design/icons';

const { Title, Text } = Typography;

export default function Home() {
  const { user, realName, roleCode } = useAuth();
  const navigate = useNavigate();

  const roleLabel = roleCode ? ROLE_LABELS[roleCode as RoleCode] : '';

  const roleColorMap: Record<string, string> = {
    super_admin: 'red',
    dept_admin: 'orange',
    teacher: 'blue',
    assistant: 'cyan',
    student: 'green',
  };

  return (
    <div>
      {/* ===== 欢迎区域 ===== */}
      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
          <Avatar
            size={56}
            icon={<UserOutlined />}
            style={{ backgroundColor: '#1677ff', fontSize: 24, flexShrink: 0 }}
          >
            {realName ? realName.charAt(0) : undefined}
          </Avatar>
          <div>
            <Title level={4} style={{ margin: 0 }}>
              你好，{realName || user?.username || '同学'}
            </Title>
            <Space style={{ marginTop: 4 }}>
              <Tag color={roleColorMap[roleCode || ''] || 'default'}>{roleLabel}</Tag>
              <Text type="secondary">欢迎来到学习与建设平台</Text>
            </Space>
          </div>
        </div>
      </Card>

      {/* ===== 统计卡片（占位，待后端数据） ===== */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="我的课程"
              value={0}
              prefix={<BookOutlined />}
              suffix="门"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="学习时长"
              value={0}
              prefix={<ClockCircleOutlined />}
              suffix="小时"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="完成任务"
              value={0}
              prefix={<TrophyOutlined />}
              suffix="个"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="连续学习"
              value={0}
              prefix={<ClockCircleOutlined />}
              suffix="天"
            />
          </Card>
        </Col>
      </Row>

      {/* ===== 快捷入口 ===== */}
      <Card title="快捷入口">
        <Space wrap>
          <Button type="primary" onClick={() => navigate('/profile')}>
            个人中心
          </Button>
        </Space>
      </Card>
    </div>
  );
}
