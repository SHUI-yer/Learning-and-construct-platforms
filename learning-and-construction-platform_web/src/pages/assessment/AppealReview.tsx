import { useCallback, useState } from 'react';
import {
  Table, Button, Tag, Card, Modal, Input, InputNumber, Select,
  message, Row, Col, Typography, Tooltip, Descriptions, Empty, Form,
} from 'antd';
import { ReloadOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { useAppealPage } from '../../hooks/useAssessment';
import { reviewGradeAppeal } from '../../api/assessment';
import CourseSelect from '../../components/assessment/CourseSelect';
import {
  APPEAL_STATUS, APPEAL_STATUS_LABELS, APPEAL_STATUS_COLORS,
} from '../../constants/assessment';
import type { GradeAppealVO } from '../../api/types';

const { Title, Paragraph } = Typography;
const { TextArea } = Input;

/** 复核弹窗：通过需给新分数，驳回需说明理由 */
function ReviewModal({
  appeal, onCancel, onSuccess,
}: {
  appeal: GradeAppealVO | null;
  onCancel: () => void;
  onSuccess: () => void;
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleReview = useCallback(
    async (status: string) => {
      if (!appeal) return;
      try {
        const values = await form.validateFields();
        // 通过必须给新分数、驳回必须给理由，否则学生收到无信息的处理结果
        if (status === APPEAL_STATUS.APPROVED && values.newScore == null) {
          message.warning('通过申诉请填写复核后的新分数');
          return;
        }
        if (status === APPEAL_STATUS.REJECTED && !values.teacherResponse?.trim()) {
          message.warning('驳回申诉请填写理由，学生端可见');
          return;
        }
        setLoading(true);
        await reviewGradeAppeal({
          appealId: appeal.appealId,
          status,
          newScore: status === APPEAL_STATUS.APPROVED ? values.newScore : null,
          teacherResponse: values.teacherResponse?.trim() || null,
          version: appeal.version,
        });
        message.success(status === APPEAL_STATUS.APPROVED ? '申诉已通过，成绩已更新' : '申诉已驳回');
        onSuccess();
      } catch (err) {
        if (err instanceof Error) message.error(err.message);
      } finally {
        setLoading(false);
      }
    },
    [appeal, form, onSuccess]
  );

  return (
    <Modal
      title="处理申诉"
      open={appeal != null}
      onCancel={onCancel}
      width={640}
      destroyOnClose
      footer={
        appeal?.status === APPEAL_STATUS.PENDING
          ? [
              <Button key="reject" danger icon={<CloseOutlined />} loading={loading} onClick={() => handleReview(APPEAL_STATUS.REJECTED)}>
                驳回申诉
              </Button>,
              <Button key="approve" type="primary" icon={<CheckOutlined />} loading={loading} onClick={() => handleReview(APPEAL_STATUS.APPROVED)}>
                通过并改分
              </Button>,
            ]
          : [
              <Button key="close" onClick={onCancel}>
                关闭
              </Button>,
            ]
      }
    >
      {appeal && (
        <>
          <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
            <Descriptions.Item label="任务" span={2}>
              {appeal.taskTitle ?? `#${appeal.taskId ?? '-'}`}
            </Descriptions.Item>
            <Descriptions.Item label="学生">
              {appeal.studentId != null ? `#${appeal.studentId}` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="当前分数">
              <b style={{ color: '#1677ff' }}>{appeal.currentScore ?? '-'}</b>
            </Descriptions.Item>
            <Descriptions.Item label="状态" span={2}>
              <Tag color={APPEAL_STATUS_COLORS[appeal.status] || 'default'}>
                {APPEAL_STATUS_LABELS[appeal.status] || appeal.status}
              </Tag>
              {appeal.resolvedAt && (
                <span style={{ marginLeft: 8, color: '#8c8c8c', fontSize: 12 }}>
                  处理于 {new Date(appeal.resolvedAt).toLocaleString('zh-CN')}
                </span>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="申诉理由" span={2}>
              <Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                {appeal.appealReason || '-'}
              </Paragraph>
            </Descriptions.Item>
          </Descriptions>

          {appeal.status === APPEAL_STATUS.PENDING ? (
            <Form form={form} layout="vertical">
              <Form.Item name="newScore" label="新分数（点击「通过并改分」时必填）">
                <InputNumber min={0} style={{ width: 200 }} placeholder="复核后应得的分数" />
              </Form.Item>
              <Form.Item name="teacherResponse" label="教师回复（驳回时必填，学生可见）">
                <TextArea rows={3} maxLength={500} showCount placeholder="说明判分依据或改分理由" />
              </Form.Item>
            </Form>
          ) : (
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="复核结果">
                {appeal.newScore != null ? `调整为 ${appeal.newScore} 分` : '维持原分数'}
              </Descriptions.Item>
              <Descriptions.Item label="教师回复">{appeal.teacherResponse || '-'}</Descriptions.Item>
            </Descriptions>
          )}
        </>
      )}
    </Modal>
  );
}

/** 申诉处理（教师）：按课程筛选 → 复核通过/驳回 */
export default function AppealReview() {
  const [courseId, setCourseId] = useState<number | undefined>();
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [status, setStatus] = useState<string | undefined>(APPEAL_STATUS.PENDING);
  const [current, setCurrent] = useState<GradeAppealVO | null>(null);

  const { data, isLoading, refetch } = useAppealPage(courseId ?? null, pageNum, pageSize, status);
  const rows = data?.records ?? [];

  const columns: ColumnsType<GradeAppealVO> = [
    {
      title: '任务',
      dataIndex: 'taskTitle',
      key: 'taskTitle',
      width: 220,
      ellipsis: { showTitle: false },
      render: (v: string | null, r: GradeAppealVO) => (
        <Tooltip title={v ?? ''} placement="topLeft">
          <span>{v ?? `#${r.taskId ?? '-'}`}</span>
        </Tooltip>
      ),
    },
    {
      title: '学生',
      dataIndex: 'studentId',
      key: 'studentId',
      width: 100,
      render: (v: number | null) => (v != null ? `#${v}` : '-'),
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
      width: 260,
      ellipsis: { showTitle: false },
      render: (v: string | null) => (
        <Tooltip title={v ?? ''} placement="topLeft">
          <span>{v ?? '-'}</span>
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
      title: '申诉时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (v: string | null) => (v ? new Date(v).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, r: GradeAppealVO) => (
        <Button type="link" size="small" onClick={() => setCurrent(r)}>
          {r.status === APPEAL_STATUS.PENDING ? '处理' : '查看'}
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>申诉处理</Title>

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 12]} align="middle">
          <Col>
            <CourseSelect
              value={courseId}
              onChange={(id) => {
                setCourseId(id);
                setPageNum(1);
              }}
              width={280}
              placeholder="请选择课程"
            />
          </Col>
          <Col>
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
                { value: APPEAL_STATUS.PENDING, label: APPEAL_STATUS_LABELS.pending },
                { value: APPEAL_STATUS.APPROVED, label: APPEAL_STATUS_LABELS.approved },
                { value: APPEAL_STATUS.REJECTED, label: APPEAL_STATUS_LABELS.rejected },
              ]}
            />
          </Col>
          <Col flex="auto">
            <span style={{ color: '#8c8c8c', fontSize: 13 }}>
              通过申诉需填写新分数，将直接更新该学生的成绩记录；驳回请说明理由，学生端可见。
            </span>
          </Col>
          <Col>
            <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
              刷新
            </Button>
          </Col>
        </Row>
      </Card>

      {!courseId ? (
        <Card>
          <Empty description="请先选择课程" />
        </Card>
      ) : (
        <Card>
          <Table<GradeAppealVO>
            rowKey="appealId"
            columns={columns}
            dataSource={rows}
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
            locale={{ emptyText: '暂无申诉记录' }}
            scroll={{ x: 1200 }}
          />
        </Card>
      )}

      <ReviewModal
        appeal={current}
        onCancel={() => setCurrent(null)}
        onSuccess={() => {
          setCurrent(null);
          refetch();
        }}
      />
    </div>
  );
}
