import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Button, Tag, Space, Card, Typography, Tooltip, Row, Col, Alert,
} from 'antd';
import { PlayCircleOutlined, ReloadOutlined, ProfileOutlined } from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { useMySubmissionStatuses, useTaskList } from '../../hooks/useAssessment';
import { useAuth } from '../../hooks/useAuth';
import CourseSelect from '../../components/assessment/CourseSelect';
import {
  SUBMISSION_STATUS_LABELS, SUBMISSION_STATUS_COLORS,
  TASK_TYPE, TASK_TYPE_LABELS, TASK_TYPE_COLORS, PUBLISH_TYPE,
} from '../../constants/assessment';
import type { SubmissionVO, TaskVO } from '../../api/types';

const { Title } = Typography;

/** 学生视角的任务状态：后端没有「我的提交」端点，从任务提交列表里按 studentId 取本人记录 */
interface MyTaskState {
  submission: SubmissionVO | null;
  label: string;
  color: string;
  /** 已截止（不可再作答，除非允许重交且未截止） */
  expired: boolean;
}

function resolveState(task: TaskVO, submissions: SubmissionVO[], userId: number | null): MyTaskState {
  const own = submissions.find((s) => s.studentId === userId) ?? null;
  const expired = task.deadline != null && Date.parse(task.deadline) < Date.now();

  if (own) {
    return {
      submission: own,
      label: SUBMISSION_STATUS_LABELS[own.status] || own.status,
      color: SUBMISSION_STATUS_COLORS[own.status] || 'default',
      expired,
    };
  }
  if (expired) {
    return { submission: null, label: '已截止', color: 'default', expired };
  }
  const notPublished =
    task.publishType !== PUBLISH_TYPE.IMMEDIATE &&
    (task.publishTime == null || Date.parse(task.publishTime) > Date.now());
  return notPublished
    ? { submission: null, label: '未发布', color: 'default', expired }
    : { submission: null, label: '未开始', color: 'blue', expired };
}

/** 我的任务与考核（学生）：仅展示可在线作答的测验任务 */
export default function TaskList() {
  const navigate = useNavigate();
  const { userId } = useAuth();
  const [courseId, setCourseId] = useState<number | undefined>();
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const { data: tasks, isLoading, refetch } = useTaskList(courseId ?? null);
  const quizTasks = useMemo(
    () => (tasks ?? []).filter((t) => t.taskType === TASK_TYPE.QUIZ),
    [tasks]
  );
  const { byTask } = useMySubmissionStatuses(quizTasks, userId);

  // 状态由本地计算（含截止判断），分页放在前端做
  const rows = useMemo(
    () =>
      quizTasks.map((t) => {
        const state = resolveState(t, byTask[t.taskId] ?? [], userId);
        return { task: t, state };
      }),
    [quizTasks, byTask, userId]
  );
  const pageRows = useMemo(
    () => rows.slice((pageNum - 1) * pageSize, pageNum * pageSize),
    [rows, pageNum, pageSize]
  );

  const columns: ColumnsType<(typeof rows)[number]> = [
    {
      title: '任务标题',
      key: 'title',
      width: 260,
      ellipsis: { showTitle: false },
      render: (_: unknown, { task }: (typeof rows)[number]) => (
        <Tooltip title={task.description || task.title} placement="topLeft">
          <span style={{ fontWeight: 500 }}>{task.title}</span>
        </Tooltip>
      ),
    },
    {
      title: '类型',
      key: 'taskType',
      width: 90,
      render: (_: unknown, { task }: (typeof rows)[number]) => (
        <Tag color={TASK_TYPE_COLORS[task.taskType] || 'default'}>
          {TASK_TYPE_LABELS[task.taskType] || task.taskType}
        </Tag>
      ),
    },
    {
      title: '截止时间',
      key: 'deadline',
      width: 190,
      render: (_: unknown, { task, state }: (typeof rows)[number]) => (
        <Space size={4}>
          <span>{task.deadline ? new Date(task.deadline).toLocaleString('zh-CN') : '-'}</span>
          {state.expired && <Tag color="default">已截止</Tag>}
        </Space>
      ),
    },
    {
      title: '满分',
      key: 'maxScore',
      width: 80,
      render: (_: unknown, { task }: (typeof rows)[number]) => task.maxScore ?? '-',
    },
    {
      title: '状态',
      key: 'status',
      width: 100,
      render: (_: unknown, { state }: (typeof rows)[number]) => (
        <Tag color={state.color}>{state.label}</Tag>
      ),
    },
    {
      title: '得分',
      key: 'score',
      width: 100,
      render: (_: unknown, { state }: (typeof rows)[number]) =>
        state.submission?.status === 'graded' ? (
          <b style={{ color: '#1677ff' }}>{state.submission.score ?? '-'}</b>
        ) : (
          <span style={{ color: '#bfbfbf' }}>-</span>
        ),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: unknown, { task, state }: (typeof rows)[number]) => {
        const sub = state.submission;
        // 已批阅：先看结果；允许重交且未截止时可重考
        if (sub?.status === 'graded') {
          return (
            <Space size={4}>
              <Button
                type="link"
                size="small"
                icon={<ProfileOutlined />}
                onClick={() => navigate(`/my-assessment/exam/${task.taskId}`)}
              >
                查看结果
              </Button>
              {task.allowResubmit === 1 && !state.expired && (
                <Button
                  type="link"
                  size="small"
                  onClick={() => navigate(`/my-assessment/exam/${task.taskId}?retake=1`)}
                >
                  重新考试
                </Button>
              )}
            </Space>
          );
        }
        // 作答中：继续作答
        if (sub?.status === 'in_progress') {
          return (
            <Button
              type="link"
              size="small"
              icon={<PlayCircleOutlined />}
              onClick={() => navigate(`/my-assessment/exam/${task.taskId}`)}
            >
              继续作答
            </Button>
          );
        }
        // 已交卷待批阅 或 未作答
        if (state.expired) {
          return sub ? (
            <Button
              type="link"
              size="small"
              onClick={() => navigate(`/my-assessment/exam/${task.taskId}`)}
            >
              查看提交
            </Button>
          ) : (
            <span style={{ color: '#bfbfbf', fontSize: 13 }}>已截止</span>
          );
        }
        return (
          <Button
            type="primary"
            size="small"
            icon={<PlayCircleOutlined />}
            onClick={() => navigate(`/my-assessment/exam/${task.taskId}`)}
          >
            {sub ? '查看提交' : '开始考试'}
          </Button>
        );
      },
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>我的任务与考核</Title>

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 12]} align="middle">
          <Col>
            <CourseSelect
              value={courseId}
              onChange={(id) => {
                setCourseId(id);
                setPageNum(1);
              }}
              studentMode
              width={280}
              placeholder="请选择课程"
            />
          </Col>
          <Col flex="auto">
            <span style={{ color: '#8c8c8c', fontSize: 13 }}>
              这里只列出支持在线作答的「测验」任务；作业/签到/实验报告等任务请按教师要求提交。
            </span>
          </Col>
          <Col>
            <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
              刷新
            </Button>
          </Col>
        </Row>
      </Card>

      {!courseId && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="请先选择课程，再查看该课程下的考核任务。"
        />
      )}

      <Card>
        <Table<(typeof rows)[number]>
          rowKey={({ task }) => task.taskId}
          columns={columns}
          dataSource={pageRows}
          loading={isLoading}
          pagination={{
            current: pageNum,
            pageSize,
            total: rows.length,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 个测验任务`,
          }}
          onChange={(pagination: TablePaginationConfig) => {
            setPageNum(pagination.current || 1);
            setPageSize(pagination.pageSize || 10);
          }}
          scroll={{ x: 1100 }}
        />
      </Card>
    </div>
  );
}
