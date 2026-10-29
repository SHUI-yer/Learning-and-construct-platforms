import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Card, Tag, Typography, Row, Col, Space, Button, Empty, Statistic,
} from 'antd';
import { ReloadOutlined, TrophyOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  useGradeSummaryPage, useMySubmissionStatuses, useTaskList,
} from '../../hooks/useAssessment';
import { useAuth } from '../../hooks/useAuth';
import CourseSelect from '../../components/assessment/CourseSelect';
import {
  SUBMISSION_STATUS_LABELS, SUBMISSION_STATUS_COLORS,
  TASK_TYPE, TASK_TYPE_LABELS, TASK_TYPE_COLORS,
} from '../../constants/assessment';
import type { TaskVO } from '../../api/types';

const { Title } = Typography;

/** 我的成绩（学生）：课程最终成绩 + 各测验任务得分 */
export default function Grades() {
  const navigate = useNavigate();
  const { userId } = useAuth();
  const [courseId, setCourseId] = useState<number | undefined>();

  // 汇总表是课程维度全量，接口不支持按学生过滤，这里取大页后前端筛本人
  const { data: summaryPage, isLoading: summaryLoading, refetch: refetchSummary } =
    useGradeSummaryPage(courseId ?? null, 1, 500);
  const { data: tasks, refetch: refetchTasks } = useTaskList(courseId ?? null);

  const quizTasks = useMemo(
    () => (tasks ?? []).filter((t) => t.taskType === TASK_TYPE.QUIZ),
    [tasks]
  );
  const { byTask } = useMySubmissionStatuses(quizTasks, userId);

  const mySummary = useMemo(
    () => (summaryPage?.records ?? []).find((s) => s.studentId === userId) ?? null,
    [summaryPage, userId]
  );

  const taskColumns: ColumnsType<TaskVO> = [
    {
      title: '任务标题',
      dataIndex: 'title',
      key: 'title',
      width: 260,
      ellipsis: { showTitle: false },
    },
    {
      title: '类型',
      dataIndex: 'taskType',
      key: 'taskType',
      width: 90,
      render: (v: string) => (
        <Tag color={TASK_TYPE_COLORS[v] || 'default'}>{TASK_TYPE_LABELS[v] || v}</Tag>
      ),
    },
    {
      title: '权重',
      dataIndex: 'weight',
      key: 'weight',
      width: 90,
      render: (v: number | null) => (v != null ? `${v}%` : '-'),
    },
    {
      title: '状态',
      key: 'status',
      width: 110,
      render: (_: unknown, task: TaskVO) => {
        const own = (byTask[task.taskId] ?? []).find((s) => s.studentId === userId);
        const status = own?.status ?? 'not_started';
        return (
          <Tag color={SUBMISSION_STATUS_COLORS[status] || 'default'}>
            {SUBMISSION_STATUS_LABELS[status] || (own ? status : '未开始')}
          </Tag>
        );
      },
    },
    {
      title: '得分',
      key: 'score',
      width: 120,
      render: (_: unknown, task: TaskVO) => {
        const own = (byTask[task.taskId] ?? []).find((s) => s.studentId === userId);
        if (!own || own.status !== 'graded') return <span style={{ color: '#bfbfbf' }}>-</span>;
        return (
          <span>
            <b style={{ color: '#1677ff' }}>{own.score ?? '-'}</b>
            <span style={{ color: '#8c8c8c' }}> / {own.maxScore ?? task.maxScore ?? '-'}</span>
          </span>
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, task: TaskVO) => {
        const own = (byTask[task.taskId] ?? []).find((s) => s.studentId === userId);
        if (!own) {
          return <span style={{ color: '#bfbfbf', fontSize: 13 }}>未参加</span>;
        }
        return (
          <Button
            type="link"
            size="small"
            onClick={() => navigate(`/my-assessment/exam/${task.taskId}`)}
          >
            查看详情
          </Button>
        );
      },
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>我的成绩</Title>

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 12]} align="middle">
          <Col>
            <CourseSelect
              value={courseId}
              onChange={setCourseId}
              studentMode
              width={280}
              placeholder="请选择课程"
            />
          </Col>
          <Col flex="auto">
            <span style={{ color: '#8c8c8c', fontSize: 13 }}>
              课程最终成绩由教师完成「成绩汇总并确认」后生成；未确认前可能仍有变动。
            </span>
          </Col>
          <Col>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                refetchSummary();
                refetchTasks();
              }}
            >
              刷新
            </Button>
          </Col>
        </Row>
      </Card>

      {!courseId ? (
        <Card>
          <Empty description="请先选择课程查看成绩" />
        </Card>
      ) : (
        <>
          <Card style={{ marginBottom: 16 }}>
            {mySummary ? (
              <Row gutter={24}>
                <Col>
                  <Statistic
                    title="课程最终成绩"
                    value={mySummary.finalScore ?? 0}
                    precision={2}
                    suffix="分"
                    valueStyle={{ color: '#1677ff', fontWeight: 700 }}
                  />
                </Col>
                <Col>
                  <Statistic
                    title="课程排名"
                    value={mySummary.rankInCourse ?? '-'}
                    prefix={<TrophyOutlined />}
                    valueStyle={{ color: mySummary.rankInCourse ? '#faad14' : '#bfbfbf' }}
                  />
                </Col>
                <Col flex="auto">
                  <div style={{ paddingTop: 26 }}>
                    {mySummary.confirmedAt ? (
                      <Tag color="success">
                        已确认 · {new Date(mySummary.confirmedAt).toLocaleString('zh-CN')}
                      </Tag>
                    ) : (
                      <Tag color="processing">待教师确认</Tag>
                    )}
                  </div>
                </Col>
              </Row>
            ) : (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无课程最终成绩（教师尚未汇总，或你还没有任何成绩记录）"
              />
            )}
          </Card>

          <Card title="各任务得分">
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Table<TaskVO>
                rowKey="taskId"
                columns={taskColumns}
                dataSource={quizTasks}
                loading={summaryLoading}
                pagination={false}
                scroll={{ x: 800 }}
                locale={{ emptyText: '该课程暂无测验任务' }}
              />
              <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                对已批阅的成绩有异议，可前往「我的申诉」发起申诉。
              </span>
            </Space>
          </Card>
        </>
      )}
    </div>
  );
}
