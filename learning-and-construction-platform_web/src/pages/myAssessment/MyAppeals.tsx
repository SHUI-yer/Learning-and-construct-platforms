import { useCallback, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Table, Button, Tag, Space, Card, Modal, Input, message, Row, Col, Empty,
  Typography, Tooltip,
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { useMyAppealPage, useTaskList } from '../../hooks/useAssessment';
import {
  createGradeAppeal, getGradeIdBySubmission, getSubmissionPage,
} from '../../api/assessment';
import { useAuth } from '../../hooks/useAuth';
import CourseSelect from '../../components/assessment/CourseSelect';
import {
  APPEAL_STATUS_LABELS, APPEAL_STATUS_COLORS,
  TASK_TYPE,
} from '../../constants/assessment';
import type { GradeAppealVO, SubmissionVO } from '../../api/types';

const { Title } = Typography;
const { TextArea } = Input;

/** 新建申诉弹窗：选课程 → 选任务 → 选本人已批阅提交 → gradeId 校验 → 填写理由 */
function CreateAppealModal({
  open, onCancel, onSuccess,
}: {
  open: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}) {
  const { userId } = useAuth();
  const [courseId, setCourseId] = useState<number | undefined>();
  const [taskId, setTaskId] = useState<number | undefined>();
  const [appealTarget, setAppealTarget] = useState<{ submissionId: number; gradeId: number } | null>(null);
  const [appealReason, setAppealReason] = useState('');
  const [checking, setChecking] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const { data: tasks } = useTaskList(courseId ?? null);
  const quizTasks = (tasks ?? []).filter((t) => t.taskType === TASK_TYPE.QUIZ);

  // 选择任务后拉取本人已批阅提交（只保留已批阅的，未批阅无法申诉）
  const { data: subs, isLoading: subsLoading } = useQuery({
    queryKey: ['myGradedSubmissions', taskId, userId],
    queryFn: () => getSubmissionPage(taskId!, 1, 100),
    enabled: !!taskId && !!userId,
  });
  const gradedSubs = (subs?.records ?? []).filter(
    (s) => s.studentId === userId && s.status === 'graded'
  );

  /** 点击申诉：查询成绩记录ID（无则不可申诉） */
  const handleAppealClick = useCallback(async (sub: SubmissionVO) => {
    setChecking(true);
    try {
      const gradeId = await getGradeIdBySubmission(sub.submissionId);
      if (gradeId == null) {
        message.warning('该成绩暂不支持申诉（需教师批阅确认后生成成绩记录）');
        setAppealTarget(null);
      } else {
        setAppealTarget({ submissionId: sub.submissionId, gradeId });
      }
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setChecking(false);
    }
  }, []);

  /** 提交申诉 */
  const handleSubmit = useCallback(async () => {
    if (!appealTarget) return;
    const reason = appealReason.trim();
    if (!reason) {
      message.warning('请填写申诉理由');
      return;
    }
    setSubmitting(true);
    try {
      await createGradeAppeal({ gradeId: appealTarget.gradeId, appealReason: reason });
      message.success('申诉已提交，等待教师处理');
      onSuccess();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setSubmitting(false);
    }
  }, [appealTarget, appealReason, onSuccess]);

  const subColumns: ColumnsType<SubmissionVO> = [
    {
      title: '提交时间',
      dataIndex: 'submittedAt',
      key: 'submittedAt',
      width: 180,
      render: (v: string | null) => (v ? new Date(v).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '得分',
      key: 'score',
      width: 110,
      render: (_: unknown, record: SubmissionVO) => (
        <b style={{ color: '#1677ff' }}>{record.score ?? '-'}</b>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, record: SubmissionVO) => (
        <Button
          type="link"
          size="small"
          loading={checking}
          onClick={() => handleAppealClick(record)}
        >
          申诉
        </Button>
      ),
    },
  ];

  return (
    <Modal
      title="新建申诉"
      open={open}
      onCancel={onCancel}
      footer={
        appealTarget
          ? [
              <Button key="back" onClick={() => setAppealTarget(null)}>
                返回选择
              </Button>,
              <Button key="submit" type="primary" loading={submitting} onClick={handleSubmit}>
                提交申诉
              </Button>,
            ]
          : [
              <Button key="close" onClick={onCancel}>
                取消
              </Button>,
            ]
      }
      width={720}
      destroyOnClose
    >
      <div style={{ marginTop: 8 }}>
        {!appealTarget ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <div>
              <div style={{ marginBottom: 6, color: '#595959' }}>1. 选择课程</div>
              <CourseSelect
                value={courseId}
                onChange={(id) => {
                  setCourseId(id);
                  setTaskId(undefined);
                }}
                studentMode
                width={280}
                placeholder="请选择课程"
              />
            </div>
            <div>
              <div style={{ marginBottom: 6, color: '#595959' }}>2. 选择测验任务（仅展示已批阅的提交）</div>
              <Row gutter={[16, 8]}>
                {quizTasks.length === 0 ? (
                  <Col span={24}>
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该课程暂无测验任务" />
                  </Col>
                ) : (
                  quizTasks.map((t) => (
                    <Col span={12} key={t.taskId}>
                      <Card
                        size="small"
                        hoverable
                        onClick={() => {
                          setTaskId(t.taskId);
                          setAppealTarget(null);
                        }}
                        style={{
                          borderColor: taskId === t.taskId ? '#1677ff' : undefined,
                          background: taskId === t.taskId ? '#f0f7ff' : undefined,
                        }}
                      >
                        <span style={{ fontSize: 13 }}>{t.title}</span>
                      </Card>
                    </Col>
                  ))
                )}
              </Row>
            </div>
            {taskId && (
              <div>
                <div style={{ marginBottom: 6, color: '#595959' }}>3. 选择要申诉的提交</div>
                <Table<SubmissionVO>
                  rowKey="submissionId"
                  size="small"
                  columns={subColumns}
                  dataSource={gradedSubs}
                  loading={subsLoading}
                  pagination={false}
                  locale={{ emptyText: '未找到已批阅的提交（需教师批阅后才能申诉）' }}
                />
              </div>
            )}
          </Space>
        ) : (
          <div>
            <AlertInfo gradeId={appealTarget.gradeId} />
            <TextArea
              rows={4}
              placeholder="请说明申诉理由（如：对某题判分有异议、分数统计有误等）"
              value={appealReason}
              onChange={(e) => setAppealReason(e.target.value)}
              maxLength={500}
              showCount
            />
          </div>
        )}
      </div>
    </Modal>
  );
}

/** 申诉对象提示 */
function AlertInfo({ gradeId }: { gradeId: number }) {
  return (
    <div style={{ marginBottom: 12, padding: '8px 12px', background: '#f0f7ff', borderRadius: 6, fontSize: 13 }}>
      已选定成绩记录（#{gradeId}），提交后由任课教师复核处理。
    </div>
  );
}

/** 我的申诉页（学生）：申诉列表 + 新建申诉 */
export default function MyAppeals() {
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [createOpen, setCreateOpen] = useState(false);

  const { data, isLoading, refetch } = useMyAppealPage(pageNum, pageSize);

  const columns: ColumnsType<GradeAppealVO> = [
    {
      title: '任务',
      dataIndex: 'taskTitle',
      key: 'taskTitle',
      width: 200,
      ellipsis: { showTitle: false },
      render: (text: string | null) => (
        <Tooltip title={text ?? ''} placement="topLeft">
          <span>{text ?? '-'}</span>
        </Tooltip>
      ),
    },
    {
      title: '原分数',
      dataIndex: 'currentScore',
      key: 'currentScore',
      width: 90,
      render: (v: number | null) => <b>{v ?? '-'}</b>,
    },
    {
      title: '申诉理由',
      dataIndex: 'appealReason',
      key: 'appealReason',
      width: 240,
      ellipsis: { showTitle: false },
      render: (text: string) => (
        <Tooltip title={text} placement="topLeft">
          <span>{text}</span>
        </Tooltip>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (v: string) => (
        <Tag color={APPEAL_STATUS_COLORS[v] || 'default'}>{APPEAL_STATUS_LABELS[v] || v}</Tag>
      ),
    },
    {
      title: '新分数',
      dataIndex: 'newScore',
      key: 'newScore',
      width: 90,
      render: (v: number | null) => (v != null ? <span style={{ color: '#52c41a' }}>{v}</span> : '-'),
    },
    {
      title: '教师回复',
      dataIndex: 'teacherResponse',
      key: 'teacherResponse',
      width: 200,
      ellipsis: { showTitle: false },
      render: (text: string | null) => (
        text ? (
          <Tooltip title={text} placement="topLeft">
            <span style={{ color: '#595959' }}>{text}</span>
          </Tooltip>
        ) : '-'
      ),
    },
    {
      title: '申诉时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (v: string | null) => (v ? new Date(v).toLocaleString('zh-CN') : '-'),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>我的申诉</Title>

      <Card style={{ marginBottom: 16 }}>
        <Row align="middle">
          <Col flex="auto">
            <span style={{ color: '#8c8c8c', fontSize: 13 }}>
              对教师批阅后的成绩有异议时，可发起申诉；每位成绩仅可有一条待处理申诉。
            </span>
          </Col>
          <Col>
            <Space>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
                新建申诉
              </Button>
              <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
                刷新
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Card>
        <Table<GradeAppealVO>
          rowKey="appealId"
          columns={columns}
          dataSource={data?.records || []}
          loading={isLoading}
          pagination={{
            current: pageNum,
            pageSize,
            total: data?.total || 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条申诉`,
          }}
          onChange={(pagination: TablePaginationConfig) => {
            setPageNum(pagination.current || 1);
            setPageSize(pagination.pageSize || 10);
          }}
          scroll={{ x: 1100 }}
        />
      </Card>

      {createOpen && (
        <CreateAppealModal
          open={createOpen}
          onCancel={() => setCreateOpen(false)}
          onSuccess={() => {
            setCreateOpen(false);
            refetch();
          }}
        />
      )}
    </div>
  );
}
