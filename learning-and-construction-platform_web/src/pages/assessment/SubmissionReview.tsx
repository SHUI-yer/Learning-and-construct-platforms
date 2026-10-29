import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Table, Button, Tag, Space, Card, Drawer, Tabs, Input, InputNumber, Select,
  Form, message, Row, Col, Typography, Alert, Empty, Spin,
} from 'antd';
import {
  ArrowLeftOutlined, ReloadOutlined, SafetyCertificateOutlined, StarOutlined,
} from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { useCheatLogs, useSubmission, useSubmissionPage, useTask } from '../../hooks/useAssessment';
import { gradeSubmission, setSubmissionExcellent } from '../../api/assessment';
import QuestionCard from '../../components/assessment/QuestionCard';
import {
  SUBMISSION_STATUS, SUBMISSION_STATUS_LABELS, SUBMISSION_STATUS_COLORS,
  CHEAT_TYPE_LABELS,
} from '../../constants/assessment';
import type { CheatLogVO, SubmissionVO } from '../../api/types';

const { Title } = Typography;
const { TextArea } = Input;

/** 违规类型汇总（同一提交的多次同类型违规合并计数） */
function summarizeCheats(logs: CheatLogVO[]): { type: string; label: string; count: number }[] {
  const map = new Map<string, number>();
  logs.forEach((l) => map.set(l.cheatType, (map.get(l.cheatType) ?? 0) + 1));
  return [...map.entries()].map(([type, count]) => ({
    type,
    label: CHEAT_TYPE_LABELS[type] || type,
    count,
  }));
}

/** 答题详情 Tab：逐题展示学生作答与自动判分结果 */
function AnswerTab({ submissionId }: { submissionId: number }) {
  const { data, isLoading } = useSubmission(submissionId);

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 40 }}>
        <Spin />
      </div>
    );
  }
  const answers = data?.answers ?? [];
  if (answers.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该提交没有逐题作答记录" />;
  }
  return (
    <div>
      {answers
        .slice()
        .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
        .map((a, i) => (
          <QuestionCard
            key={a.qId}
            index={i + 1}
            question={{
              qType: a.qType,
              content: a.content,
              options: a.options,
              answer: a.answer,
              analysis: a.analysis,
              score: a.fullScore ?? undefined,
            }}
            review={{
              studentAnswer: a.studentAnswer,
              isCorrect: a.isCorrect,
              score: a.score,
              fullScore: a.fullScore,
            }}
          />
        ))}
    </div>
  );
}

/** 防作弊记录 Tab：类型统计 + 明细时间线 */
function CheatTab({ submissionId }: { submissionId: number }) {
  const { data: logs, isLoading } = useCheatLogs(submissionId);
  const list = logs ?? [];
  const stats = summarizeCheats(list);

  if (!isLoading && list.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="未记录到违规行为" />;
  }
  return (
    <div>
      <Space wrap size={8} style={{ marginBottom: 16 }}>
        {stats.map((s) => (
          <Tag key={s.type} color="warning" icon={<SafetyCertificateOutlined />}>
            {s.label} × {s.count}
          </Tag>
        ))}
      </Space>
      <Table<CheatLogVO>
        rowKey="id"
        size="small"
        loading={isLoading}
        dataSource={list}
        pagination={false}
        locale={{ emptyText: '未记录到违规行为' }}
        columns={[
          {
            title: '违规类型',
            dataIndex: 'cheatTypeName',
            key: 'cheatTypeName',
            width: 140,
            render: (v: string, r: CheatLogVO) => (
              <Tag color="warning">{v || CHEAT_TYPE_LABELS[r.cheatType] || r.cheatType}</Tag>
            ),
          },
          { title: '详情', dataIndex: 'detail', key: 'detail', ellipsis: true, render: (v: string | null) => v || '-' },
          {
            title: '发生时间',
            dataIndex: 'occurredAt',
            key: 'occurredAt',
            width: 180,
            render: (v: string | null) => (v ? new Date(v).toLocaleString('zh-CN') : '-'),
          },
        ]}
      />
    </div>
  );
}

/** 批阅表单：总分 + 评语（乐观锁 version 冲突时提示刷新） */
function GradeForm({ submission, onSuccess }: { submission: SubmissionVO; onSuccess: () => void }) {
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [excellentLoading, setExcellentLoading] = useState(false);

  useEffect(() => {
    form.setFieldsValue({
      score: submission.score ?? 0,
      teacherComment: submission.teacherComment ?? '',
    });
  }, [form, submission]);

  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      await gradeSubmission({
        submissionId: submission.submissionId,
        score: values.score,
        teacherComment: values.teacherComment || null,
        version: submission.version,
      });
      message.success('批阅已保存');
      onSuccess();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setSaving(false);
    }
  }, [form, submission, onSuccess]);

  const handleExcellent = useCallback(async () => {
    setExcellentLoading(true);
    try {
      await setSubmissionExcellent({
        submissionId: submission.submissionId,
        isExcellent: submission.isExcellent === 1 ? 0 : 1,
        version: submission.version,
      });
      message.success(submission.isExcellent === 1 ? '已取消优秀范本' : '已设为优秀范本');
      onSuccess();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setExcellentLoading(false);
    }
  }, [submission, onSuccess]);

  return (
    <div style={{ padding: '0 4px' }}>
      <Form form={form} layout="vertical">
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              name="score"
              label={`总分（满分 ${submission.maxScore ?? '-'}）`}
              rules={[
                { required: true, message: '请输入总分' },
                {
                  validator: (_, v: number) =>
                    submission.maxScore != null && v > submission.maxScore
                      ? Promise.reject(new Error(`不得超过满分 ${submission.maxScore}`))
                      : Promise.resolve(),
                },
              ]}
            >
              <InputNumber min={0} max={submission.maxScore ?? undefined} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item
          name="teacherComment"
          label="评语（可选）"
          extra="可在此说明主观题给分依据，学生端成绩页可见"
        >
          <TextArea rows={3} placeholder="主观题批阅说明、改进建议等" maxLength={500} showCount />
        </Form.Item>
      </Form>
      <Space>
        <Button type="primary" loading={saving} onClick={handleSave}>
          保存批阅
        </Button>
        <Button icon={<StarOutlined />} loading={excellentLoading} onClick={handleExcellent}>
          {submission.isExcellent === 1 ? '取消优秀范本' : '设为优秀范本'}
        </Button>
      </Space>
      <Alert
        style={{ marginTop: 12 }}
        type="info"
        showIcon
        message="填空题/简答题由教师人工给分；选择题、判断题已在交卷时自动判分。"
      />
    </div>
  );
}

/** 阅卷页（教师）：任务提交列表 → 详情 Drawer（答题详情 / 批阅 / 防作弊记录） */
export default function SubmissionReview() {
  const navigate = useNavigate();
  const { taskId: taskIdStr } = useParams<{ taskId: string }>();
  const taskId = Number(taskIdStr);

  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [status, setStatus] = useState<string | undefined>();
  const [detailId, setDetailId] = useState<number | null>(null);

  const { data: task } = useTask(taskId);
  const { data, isLoading, refetch } = useSubmissionPage(taskId, pageNum, pageSize, status);
  const { data: detail, refetch: refetchDetail } = useSubmission(detailId);

  const rows = useMemo(() => data?.records ?? [], [data]);

  const columns: ColumnsType<SubmissionVO> = [
    {
      title: '学生ID',
      dataIndex: 'studentId',
      key: 'studentId',
      width: 100,
      render: (v: number) => `#${v}`,
    },
    {
      title: '提交时间',
      dataIndex: 'submittedAt',
      key: 'submittedAt',
      width: 190,
      render: (v: string | null, r: SubmissionVO) => (
        <Space size={4}>
          <span>{v ? new Date(v).toLocaleString('zh-CN') : '-'}</span>
          {r.isLate === 1 && <Tag color="orange">晚交</Tag>}
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (v: string) => (
        <Tag color={SUBMISSION_STATUS_COLORS[v] || 'default'}>
          {SUBMISSION_STATUS_LABELS[v] || v}
        </Tag>
      ),
    },
    {
      title: '得分',
      key: 'score',
      width: 120,
      render: (_: unknown, r: SubmissionVO) =>
        r.status === SUBMISSION_STATUS.GRADED ? (
          <span>
            <b style={{ color: '#1677ff' }}>{r.score ?? '-'}</b>
            <span style={{ color: '#8c8c8c' }}> / {r.maxScore ?? '-'}</span>
          </span>
        ) : (
          <span style={{ color: '#bfbfbf' }}>待批阅</span>
        ),
    },
    {
      title: '范本',
      key: 'excellent',
      width: 80,
      render: (_: unknown, r: SubmissionVO) =>
        r.isExcellent === 1 ? <Tag color="gold">优秀</Tag> : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, r: SubmissionVO) => (
        <Button type="link" size="small" onClick={() => setDetailId(r.submissionId)}>
          查看/批阅
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 12]} align="middle">
          <Col>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/assessment/tasks')}>
              返回任务列表
            </Button>
          </Col>
          <Col flex="auto">
            <Space size={12} wrap>
              <Title level={5} style={{ margin: 0 }}>
                {task ? task.title : `任务 #${taskId}`}
              </Title>
              <Tag color="blue">阅卷</Tag>
              <span style={{ color: '#8c8c8c', fontSize: 13 }}>
                满分 {task?.maxScore ?? '-'} 分
              </span>
            </Space>
          </Col>
          <Col>
            <Space>
              <Select
                value={status}
                onChange={(v) => {
                  setStatus(v);
                  setPageNum(1);
                }}
                allowClear
                placeholder="全部状态"
                style={{ width: 150 }}
                options={[
                  { value: SUBMISSION_STATUS.PENDING, label: SUBMISSION_STATUS_LABELS.pending },
                  { value: SUBMISSION_STATUS.IN_PROGRESS, label: SUBMISSION_STATUS_LABELS.in_progress },
                  { value: SUBMISSION_STATUS.GRADED, label: SUBMISSION_STATUS_LABELS.graded },
                ]}
              />
              <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
                刷新
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Card>
        <Table<SubmissionVO>
          rowKey="submissionId"
          columns={columns}
          dataSource={rows}
          loading={isLoading}
          pagination={{
            current: pageNum,
            pageSize,
            total: data?.total || 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 份提交`,
          }}
          onChange={(pagination: TablePaginationConfig) => {
            setPageNum(pagination.current || 1);
            setPageSize(pagination.pageSize || 10);
          }}
          locale={{ emptyText: '暂无提交记录' }}
        />
      </Card>

      <Drawer
        title={detail ? `提交详情 #${detail.submissionId}（学生 #${detail.studentId}）` : '提交详情'}
        open={detailId != null}
        onClose={() => setDetailId(null)}
        width={860}
        destroyOnClose
      >
        {detail && (
          <>
            <Space size={12} wrap style={{ marginBottom: 12 }}>
              <Tag color={SUBMISSION_STATUS_COLORS[detail.status] || 'default'}>
                {SUBMISSION_STATUS_LABELS[detail.status] || detail.status}
              </Tag>
              <span style={{ color: '#8c8c8c', fontSize: 13 }}>
                提交于 {detail.submittedAt ? new Date(detail.submittedAt).toLocaleString('zh-CN') : '-'}
              </span>
              {detail.isLate === 1 && <Tag color="orange">晚交</Tag>}
            </Space>

            <CheatBadge submissionId={detail.submissionId} />

            <Tabs
              defaultActiveKey="answers"
              items={[
                {
                  key: 'answers',
                  label: '答题详情',
                  children: <AnswerTab submissionId={detail.submissionId} />,
                },
                {
                  key: 'grade',
                  label: detail.status === SUBMISSION_STATUS.GRADED ? '修改批阅' : '批阅打分',
                  children: (
                    <GradeForm
                      submission={detail}
                      onSuccess={() => {
                        refetchDetail();
                        refetch();
                      }}
                    />
                  ),
                },
                {
                  key: 'cheat',
                  label: '防作弊记录',
                  children: <CheatTab submissionId={detail.submissionId} />,
                },
              ]}
            />
          </>
        )}
      </Drawer>
    </div>
  );
}

/** 详情顶部的违规提醒条（有违规才显示） */
function CheatBadge({ submissionId }: { submissionId: number }) {
  const { data: logs } = useCheatLogs(submissionId);
  const list = logs ?? [];
  if (list.length === 0) return null;
  const stats = summarizeCheats(list);
  return (
    <Alert
      type="warning"
      showIcon
      icon={<SafetyCertificateOutlined />}
      style={{ marginBottom: 12 }}
      message={
        <Space size={8} wrap>
          <span>该提交记录到 {list.length} 次违规行为</span>
          {stats.map((s) => (
            <Tag key={s.type} color="orange">
              {s.label} × {s.count}
            </Tag>
          ))}
        </Space>
      }
      description="可在「防作弊记录」标签页查看明细，请结合违规情况判断是否影响成绩。"
    />
  );
}
