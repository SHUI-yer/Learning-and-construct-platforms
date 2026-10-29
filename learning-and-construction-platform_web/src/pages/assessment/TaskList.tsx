import { useCallback, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Button, Input, Select, Tag, Space, Card, Modal, Form,
  message, Tooltip, Row, Col, Switch, InputNumber, DatePicker, Popconfirm,
} from 'antd';
import {
  PlusOutlined, SearchOutlined, ReloadOutlined, EditOutlined,
  DeleteOutlined, FileDoneOutlined,
} from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import { useTaskList } from '../../hooks/useAssessment';
import { useCourses } from '../../hooks/useCourses';
import { createTask, deleteTask, updateTask } from '../../api/assessment';
import CourseSelect from '../../components/assessment/CourseSelect';
import {
  TASK_TYPE_LABELS, TASK_TYPE_COLORS,
  PUBLISH_TYPE, PUBLISH_TYPE_LABELS,
} from '../../constants/assessment';
import type { TaskCreateParams, TaskVO } from '../../api/types';

const { Option } = Select;
const { TextArea } = Input;

/** LocalDateTime -> ISO 字符串（后端默认 Jackson ISO 反序列化） */
const fmt = (d: Dayjs | null) => (d ? d.format('YYYY-MM-DDTHH:mm:ss') : undefined);

/** 任务创建/编辑弹窗（新建任务 / 编辑任务共用） */
function TaskFormModal({
  open, task, courseId, onCancel, onSuccess,
}: {
  open: boolean;
  task: TaskVO | null;
  courseId: number;
  onCancel: () => void;
  onSuccess: () => void;
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const publishType = Form.useWatch('publishType', form);
  const allowLate = Form.useWatch('allowLate', form);
  // 课程名查找（与页面 CourseSelect 共享 React Query 缓存，不产生额外请求）
  const { data: coursesData } = useCourses(1, 50);
  const courseName = coursesData?.records.find((c) => c.courseId === courseId)?.courseName ?? `#${courseId}`;

  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const base = {
        courseId,
        title: values.title,
        taskType: values.taskType,
        tag: values.tag || undefined,
        weight: values.weight ?? 100,
        publishType: values.publishType,
        publishTime: values.publishType === PUBLISH_TYPE.SCHEDULED ? fmt(values.publishTime) : undefined,
        // 表单已校验必填，这里断言非空
        deadline: fmt(values.deadline)!,
        allowLate: values.allowLate ? 1 : 0,
        latePenalty: values.allowLate ? values.latePenalty : undefined,
        maxScore: values.maxScore ?? 100,
        allowResubmit: values.allowResubmit ? 1 : 0,
        description: values.description || undefined,
      } satisfies TaskCreateParams;
      if (task) {
        await updateTask({ ...base, taskId: task.taskId });
        message.success('任务已更新');
      } else {
        await createTask(base);
        message.success('任务创建成功');
      }
      onSuccess();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [form, task, courseId, onSuccess]);

  return (
    <Modal
      title={task ? '编辑任务' : '新建任务'}
      open={open}
      onCancel={onCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      width={640}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        style={{ marginTop: 16 }}
        initialValues={{
          publishType: PUBLISH_TYPE.IMMEDIATE,
          allowLate: false,
          allowResubmit: false,
          weight: 100,
          maxScore: 100,
          ...(task
            ? {
                ...task,
                publishTime: task.publishTime ? dayjs(task.publishTime) : undefined,
                deadline: task.deadline ? dayjs(task.deadline) : undefined,
                allowLate: task.allowLate === 1,
                allowResubmit: task.allowResubmit === 1,
              }
            : {}),
        }}
      >
        <div style={{ marginBottom: 16, color: '#8c8c8c' }}>
          所属课程：<span style={{ color: '#262626' }}>{courseName}</span>
          <span style={{ marginLeft: 12, fontSize: 12 }}>（在列表上方切换课程）</span>
        </div>
        <Form.Item name="title" label="任务标题" rules={[{ required: true, message: '请输入任务标题' }]}>
          <Input placeholder="如：第三章随堂测验" maxLength={100} />
        </Form.Item>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="taskType" label="任务类型" rules={[{ required: true, message: '请选择任务类型' }]}>
              <Select placeholder="请选择任务类型">
                {Object.entries(TASK_TYPE_LABELS).map(([value, label]) => (
                  <Option key={value} value={value}>{label}</Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="tag" label="标签">
              <Input placeholder="如：期中 / 重点" maxLength={30} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="weight" label="成绩权重（%）" tooltip="该任务在课程总评中的占比">
              <InputNumber min={0} max={100} precision={2} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="maxScore" label="满分">
              <InputNumber min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="publishType" label="发布方式" rules={[{ required: true }]}>
          <PublishTypeSelect />
        </Form.Item>
        {publishType === PUBLISH_TYPE.SCHEDULED && (
          <Form.Item
            name="publishTime"
            label="定时发布时间"
            rules={[{ required: true, message: '请选择发布时间' }]}
          >
            <DatePicker showTime format="YYYY-MM-DD HH:mm" disabledDate={(d) => d < dayjs().startOf('day')} style={{ width: '100%' }} />
          </Form.Item>
        )}
        <Form.Item name="deadline" label="截止时间" rules={[{ required: true, message: '请选择截止时间' }]}>
          <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
        </Form.Item>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="allowLate" label="允许晚交" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Col>
          {allowLate && (
            <Col span={12}>
              <Form.Item
                name="latePenalty"
                label="晚交扣分（%）"
                rules={[{ required: true, message: '请输入晚交扣分比例' }]}
              >
                <InputNumber min={0.01} max={100} precision={2} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          )}
        </Row>
        <Form.Item name="allowResubmit" label="允许重新提交" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="description" label="任务说明">
          <TextArea rows={3} placeholder="任务要求、注意事项等" maxLength={500} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

/** 发布方式下拉 */
function PublishTypeSelect() {
  return (
    <Select placeholder="请选择发布方式">
      {Object.entries(PUBLISH_TYPE_LABELS).map(([value, label]) => (
        <Option key={value} value={value}>{label}</Option>
      ))}
    </Select>
  );
}

/** 任务列表页（教师 / 管理员） */
export default function TaskList() {
  const navigate = useNavigate();
  const [courseId, setCourseId] = useState<number | undefined>();
  const [keyword, setKeyword] = useState('');
  const [taskTypeFilter, setTaskTypeFilter] = useState<string | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editTask, setEditTask] = useState<TaskVO | null>(null);

  const { data: tasks, isLoading, refetch } = useTaskList(courseId ?? null);

  // /task/list 不分页，前端过滤
  const filtered = useMemo(() => {
    const list = tasks ?? [];
    return list.filter((t) => {
      if (taskTypeFilter && t.taskType !== taskTypeFilter) return false;
      if (keyword) {
        const kw = keyword.toLowerCase();
        const hit = (t.title ?? '').toLowerCase().includes(kw) || (t.description ?? '').toLowerCase().includes(kw);
        if (!hit) return false;
      }
      return true;
    });
  }, [tasks, taskTypeFilter, keyword]);

  /** 删除任务 */
  const handleDelete = useCallback(async (taskId: number) => {
    try {
      await deleteTask(taskId);
      message.success('任务已删除');
      refetch();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  }, [refetch]);

  const columns: ColumnsType<TaskVO> = [
    {
      title: '任务标题',
      dataIndex: 'title',
      key: 'title',
      width: 220,
      ellipsis: true,
      render: (text: string) => <span style={{ fontWeight: 500 }}>{text}</span>,
    },
    {
      title: '类型',
      dataIndex: 'taskType',
      key: 'taskType',
      width: 100,
      render: (v: string) => <Tag color={TASK_TYPE_COLORS[v] || 'default'}>{TASK_TYPE_LABELS[v] || v}</Tag>,
    },
    {
      title: '标签',
      dataIndex: 'tag',
      key: 'tag',
      width: 100,
      ellipsis: true,
      render: (v: string | null) => v || '-',
    },
    {
      title: '权重',
      dataIndex: 'weight',
      key: 'weight',
      width: 80,
      render: (v: number | null) => (v != null ? `${v}%` : '-'),
    },
    {
      title: '发布',
      key: 'publish',
      width: 170,
      render: (_: unknown, record: TaskVO) => (
        <Space direction="vertical" size={0}>
          <Tag color={record.publishType === PUBLISH_TYPE.IMMEDIATE ? 'green' : 'blue'}>
            {PUBLISH_TYPE_LABELS[record.publishType] || record.publishType}
          </Tag>
          {record.publishType === PUBLISH_TYPE.SCHEDULED && record.publishTime && (
            <span style={{ fontSize: 12, color: '#8c8c8c' }}>
              {new Date(record.publishTime).toLocaleString('zh-CN')}
            </span>
          )}
        </Space>
      ),
    },
    {
      title: '截止时间',
      dataIndex: 'deadline',
      key: 'deadline',
      width: 170,
      render: (v: string | null) => {
        if (!v) return '-';
        const passed = Date.parse(v) < Date.now();
        return (
          <span style={{ color: passed ? '#ff4d4f' : undefined }}>
            {new Date(v).toLocaleString('zh-CN')}
            {passed && <Tag color="error" style={{ marginLeft: 6 }}>已截止</Tag>}
          </span>
        );
      },
    },
    {
      title: '晚交',
      key: 'late',
      width: 100,
      render: (_: unknown, record: TaskVO) =>
        record.allowLate === 1 ? (
          <Tag color="orange">扣 {record.latePenalty ?? '-'}%</Tag>
        ) : (
          <Tag>不允许</Tag>
        ),
    },
    {
      title: '满分',
      dataIndex: 'maxScore',
      key: 'maxScore',
      width: 80,
      render: (v: number | null) => v ?? '-',
    },
    {
      title: '重交',
      dataIndex: 'allowResubmit',
      key: 'allowResubmit',
      width: 80,
      render: (v: number | null) => (v === 1 ? <Tag color="green">允许</Tag> : <Tag>不允许</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_: unknown, record: TaskVO) => (
        <Space size="small" wrap>
          <Tooltip title="编辑任务">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setEditTask(record);
                setModalOpen(true);
              }}
            >
              编辑
            </Button>
          </Tooltip>
          <Tooltip title="提交与阅卷">
            <Button
              type="link"
              size="small"
              icon={<FileDoneOutlined />}
              onClick={() => navigate(`/assessment/tasks/${record.taskId}/submissions`)}
            >
              提交与阅卷
            </Button>
          </Tooltip>
          <Popconfirm
            title="确认删除该任务？"
            description="删除后不可恢复，学生提交将无法查看。"
            onConfirm={() => handleDelete(record.taskId)}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* ===== 搜索 & 筛选栏 ===== */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col flex="auto">
            <Space wrap>
              <CourseSelect
                value={courseId}
                onChange={(id) => {
                  setCourseId(id);
                  setTaskTypeFilter(undefined);
                }}
                allowOwnOnly
              />
              <Select
                placeholder="任务类型"
                value={taskTypeFilter}
                onChange={setTaskTypeFilter}
                style={{ width: 140 }}
                allowClear
              >
                {Object.entries(TASK_TYPE_LABELS).map(([value, label]) => (
                  <Option key={value} value={value}>{label}</Option>
                ))}
              </Select>
              <Input
                placeholder="搜索任务标题"
                prefix={<SearchOutlined />}
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                style={{ width: 200 }}
                allowClear
              />
            </Space>
          </Col>
          <Col>
            <Space>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                disabled={!courseId}
                onClick={() => {
                  setEditTask(null);
                  setModalOpen(true);
                }}
              >
                新建任务
              </Button>
              <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
                刷新
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* ===== 任务表格 ===== */}
      <Card>
        {!courseId ? (
          <div style={{ textAlign: 'center', padding: 60, color: '#8c8c8c' }}>请先选择课程</div>
        ) : (
          <Table<TaskVO>
            rowKey="taskId"
            columns={columns}
            dataSource={filtered}
            loading={isLoading}
            pagination={false}
            scroll={{ x: 1300 }}
          />
        )}
      </Card>

      {/* ===== 新建 / 编辑任务弹窗 ===== */}
      {modalOpen && courseId && (
        <TaskFormModal
          key={editTask?.taskId ?? 'create'}
          open={modalOpen}
          task={editTask}
          courseId={courseId}
          onCancel={() => {
            setModalOpen(false);
            setEditTask(null);
          }}
          onSuccess={() => {
            setModalOpen(false);
            setEditTask(null);
            refetch();
          }}
        />
      )}
    </div>
  );
}
