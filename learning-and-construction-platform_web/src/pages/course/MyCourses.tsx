import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card, Row, Col, Tag, Input, Empty, Spin, Typography, Space,
} from 'antd';
import {
  SearchOutlined, BookOutlined,
} from '@ant-design/icons';
import { useCourses } from '../../hooks/useCourses';
import { COURSE_STATUS_LABELS, COURSE_STATUS_COLORS } from '../../constants/course';
import type { CourseVO } from '../../api/types';

const { Text, Title } = Typography;
const { Meta } = Card;

/** 占位封面图 —— 根据课程名生成渐变色背景 */
function CourseCover({ name }: { name: string }) {
  // 根据名字 hash 生成稳定颜色
  const colors = [
    ['#667eea', '#764ba2'],
    ['#f093fb', '#f5576c'],
    ['#4facfe', '#00f2fe'],
    ['#43e97b', '#38f9d7'],
    ['#fa709a', '#fee140'],
    ['#a18cd1', '#fbc2eb'],
    ['#fccb90', '#d57eeb'],
    ['#e0c3fc', '#8ec5fc'],
  ];
  const hash = name.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0);
  const [from, to] = colors[hash % colors.length];

  return (
    <div
      style={{
        height: 140,
        background: `linear-gradient(135deg, ${from}, ${to})`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: '8px 8px 0 0',
      }}
    >
      <BookOutlined style={{ fontSize: 40, color: 'rgba(255,255,255,0.8)' }} />
    </div>
  );
}

export default function MyCourses() {
  const navigate = useNavigate();
  const [searchInput, setSearchInput] = useState('');

  // 学生端查看课程列表（公开课程）
  const { data, isLoading } = useCourses(1, 50, { status: 2 }); // status=2 已发布

  // 前端搜索过滤
  const filteredCourses = data?.records?.filter((c: CourseVO) =>
    !searchInput || c.courseName.toLowerCase().includes(searchInput.toLowerCase())
  ) || [];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>我的课程</Title>

      {/* 搜索栏 */}
      <div style={{ marginBottom: 20 }}>
        <Input
          placeholder="搜索课程名称"
          prefix={<SearchOutlined />}
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          style={{ maxWidth: 360 }}
          allowClear
        />
      </div>

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: 80 }}>
          <Spin size="large" />
        </div>
      ) : filteredCourses.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={searchInput ? '没有找到匹配的课程' : '暂无已发布的课程'}
        />
      ) : (
        <Row gutter={[16, 16]}>
          {filteredCourses.map((course: CourseVO) => (
            <Col xs={24} sm={12} lg={8} xl={6} key={course.courseId}>
              <Card
                hoverable
                cover={<CourseCover name={course.courseName} />}
                onClick={() => navigate(`/courses/${course.courseId}`)}
                styles={{ body: { padding: 16 } }}
              >
                <Meta
                  title={
                    <Space>
                      <Text ellipsis style={{ maxWidth: 160, fontSize: 15, fontWeight: 500 }}>
                        {course.courseName}
                      </Text>
                    </Space>
                  }
                  description={
                    <div>
                      <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>
                        {course.courseCode || '无编号'}
                        {course.semester && ` · ${course.semester}`}
                      </Text>
                      <Space>
                        <Tag color={COURSE_STATUS_COLORS[course.status ?? 0] || 'default'} style={{ fontSize: 11 }}>
                          {COURSE_STATUS_LABELS[course.status ?? 0] || '未知'}
                        </Tag>
                        {course.isPublic === 1 && (
                          <Tag color="green" style={{ fontSize: 11 }}>公开</Tag>
                        )}
                      </Space>
                      {course.description && (
                        <Text
                          type="secondary"
                          ellipsis={{ tooltip: course.description }}
                          style={{ display: 'block', marginTop: 8, fontSize: 12 }}
                        >
                          {course.description}
                        </Text>
                      )}
                    </div>
                  }
                />
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  );
}
