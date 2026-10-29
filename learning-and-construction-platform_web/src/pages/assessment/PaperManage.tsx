import { useCallback, useEffect, useState } from 'react';
import {
  Table, Button, Input, Select, Tag, Space, Card, Modal, Form,
  message, Row, Col, Tabs, Drawer, InputNumber, Switch, DatePicker, Popconfirm,
  Tooltip, Empty,
} from 'antd';
import {
  PlusOutlined, SearchOutlined, ReloadOutlined, EditOutlined,
  DeleteOutlined, EyeOutlined, SendOutlined, PlusCircleOutlined, MinusCircleOutlined,
} from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { usePaper, usePaperPage, useQuestionPage } from '../../hooks/useAssessment';
import {
  autoGeneratePaper, createPaper, deletePaper, getPaper,
  publishPaper, updatePaper,
} from '../../api/assessment';
import CourseSelect from '../../components/assessment/CourseSelect';
import QuestionCard from '../../components/assessment/QuestionCard';
import {
  QUESTION_TYPE_LABELS, QUESTION_TYPE_COLORS, QUESTION_TYPE,
  DIFFICULTY_LABELS, TASK_TYPE_LABELS, TASK_TYPE,
  PUBLISH_TYPE, PUBLISH_TYPE_LABELS,
} from '../../constants/assessment';
import type {
  ExamPaperVO, PaperPublishParams, QuestionVO, PaperAutoGenerateRule,
} from '../../api/types';

const { Option } = Select;

/** LocalDateTime -> ISO 字符串（后端默认 Jackson ISO 反序列化） */
const fmt = (d: Dayjs | null) => (d ? d.format('YYYY-MM-DDTHH:mm:ss') : undefined);

/** 手动选题草稿 */
interface DraftItem {
  qId: number;
  content: string;
  qType: string;
  difficulty: number | null;
  score: number;
}

/** 智能组卷规则（对齐后端 PaperAutoRuleDTO） */
type AutoRule = PaperAutoGenerateRule;

/** 新建 / 编辑试卷弹窗（新建支持手动选题 + 智能组卷两种模式） */
function PaperFormModal({
  open, courseId, editPaper, onCancel, onSuccess,
}: {
  open: boolean;
  courseId: number;
  editPaper: ExamPaperVO | null;
  onCancel: () => void;
  onSuccess: () => void;
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [mode, setMode] = useState<'manual' | 'auto'>('manual');
  const [drafts, setDrafts] = useState<DraftItem[]>([]);
  const [rules, setRules] = useState<AutoRule[]>([
    { qType: QUESTION_TYPE.SINGLE_CHOICE, difficulty: 2, count: 5, score: 5 },
  ]);

  // 题库搜索（手动选题）
  const [searchQuery, setSearchQuery] = useState<{ qType?: string; difficulty?: number; keyword?: string }>({});
  const [searchInput, setSearchInput] = useState('');
  const [searchPageNum, setSearchPageNum] = useState(1);
  const bankQuery = useQuestionPage(editPaper ? null : courseId, searchPageNum, 5, searchQuery);

  /** 打开编辑：从试卷详情初始化草稿 */
  const initEdit = useCallback(async () => {
    if (!editPaper) return;
    try {
      const paper = await getPaper(editPaper.paperId);
      form.setFieldsValue({
        title: paper.title,
        duration: paper.duration ?? undefined,
        shuffleQuestions: paper.shuffleQuestions === 1,
      });
      setDrafts(
        (paper.questions ?? []).map((q) => ({
          qId: q.qId,
          content: q.content,
          qType: q.qType,
          difficulty: q.difficulty ?? null,
          score: Number(q.score ?? 0),
        }))
      );
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  }, [editPaper, form]);

  // 弹窗打开且为编辑时初始化（拉取试卷详情回填草稿）
  useEffect(() => {
    if (open && editPaper) void initEdit();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  /** 提交：手动 -> create/update；智能 -> autoGenerate */
  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const common = {
        title: values.title,
        duration: values.duration ?? undefined,
        shuffleQuestions: values.shuffleQuestions ? 1 : 0,
      };
      if (mode === 'manual') {
        if (drafts.length === 0) {
          message.warning('请至少选择一道题目');
          return;
        }
        const items = drafts.map((d) => ({ qId: d.qId, score: d.score }));
        if (editPaper) {
          // 后端更新试卷强制校验乐观锁版本号
          await updatePaper({
            paperId: editPaper.paperId,
            courseId,
            version: editPaper.version,
            ...common,
            items,
          });
          message.success('试卷已更新');
        } else {
          await createPaper({ courseId, ...common, items });
          message.success('试卷生成成功');
        }
      } else {
        const validRules = rules.filter((r) => r.count > 0);
        if (validRules.length === 0) {
          message.warning('请至少配置一条组卷规则');
          return;
        }
        await autoGeneratePaper({ courseId, ...common, rules: validRules });
        message.success('智能组卷成功');
      }
      onSuccess();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [form, mode, drafts, rules, editPaper, courseId, onSuccess]);

  /** 添加所选题目到草稿 */
  const addSelected = useCallback((rows: QuestionVO[]) => {
    setDrafts((prev) => {
      const existing = new Set(prev.map((d) => d.qId));
      const added = rows
        .filter((q) => !existing.has(q.qId))
        .map((q) => ({
          qId: q.qId,
          content: q.content,
          qType: q.qType,
          difficulty: q.difficulty ?? null,
          score: 5,
        }));
      return [...prev, ...added];
    });
  }, []);

  /** 更新草稿题目分值 */
  const updateDraftScore = useCallback((qId: number, score: number) => {
    setDrafts((prev) => prev.map((d) => (d.qId === qId ? { ...d, score } : d)));
  }, []);

  /** 更新组卷规则 */
  const updateRule = useCallback((index: number, patch: Partial<AutoRule>) => {
    setRules((prev) => prev.map((r, i) => (i === index ? { ...r, ...patch } : r)));
  }, []);

  const selectedRowKeys = drafts.map((d) => d.qId);

  const questionColumns: ColumnsType<QuestionVO> = [
    {
      title: '题干',
      dataIndex: 'content',
      key: 'content',
      ellipsis: { showTitle: false },
      render: (text: string) => (
        <Tooltip title={text} placement="topLeft">
          <span>{text}</span>
        </Tooltip>
      ),
    },
    {
      title: '题型',
      dataIndex: 'qType',
      key: 'qType',
      width: 90,
      render: (v: string) => <Tag color={QUESTION_TYPE_COLORS[v] || 'default'}>{QUESTION_TYPE_LABELS[v] || v}</Tag>,
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      key: 'difficulty',
      width: 70,
      render: (v: number | null) => (v ? DIFFICULTY_LABELS[v] : '-'),
    },
  ];

  const manualTab = (
    <Row gutter={16}>
      <Col span={10}>
        <div style={{ fontWeight: 500, marginBottom: 8 }}>已选题目（{drafts.length}）</div>
        <div style={{ maxHeight: 360, overflow: 'auto' }}>
          {drafts.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="尚未选择题目" />
          ) : (
            drafts.map((d, i) => (
              <Card key={d.qId} size="small" style={{ marginBottom: 8 }}>
                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                  <Space size={8} wrap>
                    <span style={{ fontWeight: 500 }}>{i + 1}.</span>
                    <Tag color={QUESTION_TYPE_COLORS[d.qType] || 'default'}>{QUESTION_TYPE_LABELS[d.qType] || d.qType}</Tag>
                    <span style={{ fontSize: 12, color: '#8c8c8c' }}>分值</span>
                    <InputNumber
                      size="small"
                      min={0.5}
                      max={100}
                      value={d.score}
                      onChange={(v) => updateDraftScore(d.qId, Number(v ?? 0))}
                      style={{ width: 80 }}
                    />
                    <Button
                      type="link"
                      size="small"
                      danger
                      onClick={() => setDrafts((prev) => prev.filter((x) => x.qId !== d.qId))}
                    >
                      移除
                    </Button>
                  </Space>
                  <span style={{ fontSize: 13, color: '#595959' }}>{d.content}</span>
                </Space>
              </Card>
            ))
          )}
        </div>
      </Col>
      <Col span={14}>
        <div style={{ fontWeight: 500, marginBottom: 8 }}>题库选题</div>
        <Space wrap style={{ marginBottom: 12 }}>
          <Select
            placeholder="题型"
            value={searchQuery.qType}
            onChange={(v) => {
              setSearchPageNum(1);
              setSearchQuery((prev) => ({ ...prev, qType: v }));
            }}
            style={{ width: 120 }}
            allowClear
          >
            {Object.entries(QUESTION_TYPE_LABELS).map(([value, label]) => (
              <Option key={value} value={value}>{label}</Option>
            ))}
          </Select>
          <Select
            placeholder="难度"
            value={searchQuery.difficulty}
            onChange={(v) => {
              setSearchPageNum(1);
              setSearchQuery((prev) => ({ ...prev, difficulty: v }));
            }}
            style={{ width: 100 }}
            allowClear
          >
            {[1, 2, 3].map((d) => (
              <Option key={d} value={d}>{DIFFICULTY_LABELS[d]}</Option>
            ))}
          </Select>
          <Input
            placeholder="搜索关键词"
            prefix={<SearchOutlined />}
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onPressEnter={() => {
              setSearchPageNum(1);
              setSearchQuery((prev) => ({ ...prev, keyword: searchInput || undefined }));
            }}
            style={{ width: 160 }}
            allowClear
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={() => {
              setSearchPageNum(1);
              setSearchQuery((prev) => ({ ...prev, keyword: searchInput || undefined }));
            }}
          >
            搜索
          </Button>
        </Space>
        <Table<QuestionVO>
          rowKey="qId"
          size="small"
          columns={questionColumns}
          dataSource={bankQuery.data?.records || []}
          loading={bankQuery.isLoading}
          rowSelection={{
            selectedRowKeys,
            preserveSelectedRowKeys: true,
            onSelect: (record, selected) => {
              if (selected) addSelected([record]);
              else setDrafts((prev) => prev.filter((d) => d.qId !== record.qId));
            },
            onSelectAll: (selected, _rows, changeRows) => {
              if (selected) addSelected(changeRows as QuestionVO[]);
              else {
                const qIds = new Set((changeRows as QuestionVO[]).map((r) => r.qId));
                setDrafts((prev) => prev.filter((d) => !qIds.has(d.qId)));
              }
            },
          }}
          pagination={{
            current: searchPageNum,
            pageSize: 5,
            total: bankQuery.data?.total || 0,
            size: 'small',
            showTotal: (total) => `共 ${total} 道题`,
            onChange: (p) => setSearchPageNum(p),
          }}
        />
      </Col>
    </Row>
  );

  const autoTab = (
    <div>
      <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 12 }}>
        按题型 + 难度 + 数量 + 分值自动从题库随机抽题组卷
      </div>
      {rules.map((r, i) => (
        <Space key={i} wrap style={{ marginBottom: 8 }}>
          <Select
            value={r.qType}
            onChange={(v) => updateRule(i, { qType: v })}
            style={{ width: 120 }}
          >
            {Object.entries(QUESTION_TYPE_LABELS).map(([value, label]) => (
              <Option key={value} value={value}>{label}</Option>
            ))}
          </Select>
          <Select
            value={r.difficulty}
            onChange={(v) => updateRule(i, { difficulty: v })}
            style={{ width: 100 }}
            placeholder="难度不限"
          >
            <Option value={1}>{DIFFICULTY_LABELS[1]}</Option>
            <Option value={2}>{DIFFICULTY_LABELS[2]}</Option>
            <Option value={3}>{DIFFICULTY_LABELS[3]}</Option>
          </Select>
          <span style={{ color: '#8c8c8c' }}>数量</span>
          <InputNumber
            min={1}
            max={50}
            value={r.count}
            onChange={(v) => updateRule(i, { count: Number(v ?? 0) })}
            style={{ width: 80 }}
          />
          <span style={{ color: '#8c8c8c' }}>每题分值</span>
          <InputNumber
            min={0.5}
            max={100}
            value={r.score}
            onChange={(v) => updateRule(i, { score: Number(v ?? 0) })}
            style={{ width: 90 }}
          />
          <Button
            type="link"
            danger
            icon={<MinusCircleOutlined />}
            disabled={rules.length <= 1}
            onClick={() => setRules((prev) => prev.filter((_, x) => x !== i))}
          >
            删除
          </Button>
        </Space>
      ))}
      <Button
        type="dashed"
        icon={<PlusCircleOutlined />}
        onClick={() =>
          setRules((prev) => [
            ...prev,
            { qType: QUESTION_TYPE.SINGLE_CHOICE, difficulty: 2, count: 5, score: 5 },
          ])
        }
      >
        添加规则
      </Button>
    </div>
  );

  return (
    <Modal
      title={editPaper ? '编辑试卷' : '新建试卷'}
      open={open}
      onCancel={onCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      width={960}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        style={{ marginTop: 16 }}
        initialValues={{ shuffleQuestions: false }}
      >
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="title" label="试卷标题" rules={[{ required: true, message: '请输入试卷标题' }]}>
              <Input placeholder="如：第三章随堂测验卷" maxLength={100} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="duration" label="限时（分钟）" tooltip="不填表示不限时">
              <InputNumber min={1} max={600} placeholder="不限时" style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="shuffleQuestions" label="题目乱序" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Col>
        </Row>
      </Form>

      {!editPaper && (
        <Tabs
          activeKey={mode}
          onChange={(k) => setMode(k as 'manual' | 'auto')}
          items={[
            { key: 'manual', label: '手动选题', children: manualTab },
            { key: 'auto', label: '智能组卷', children: autoTab },
          ]}
        />
      )}
      {editPaper && manualTab}
    </Modal>
  );
}

/** 发布试卷弹窗（生成关联任务） */
function PublishModal({
  open, paper, courseId, onCancel, onSuccess,
}: {
  open: boolean;
  paper: ExamPaperVO | null;
  courseId: number;
  onCancel: () => void;
  onSuccess: () => void;
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const publishType = Form.useWatch('publishType', form);
  const allowLate = Form.useWatch('allowLate', form);

  const handleSubmit = useCallback(async () => {
    if (!paper) return;
    try {
      const values = await form.validateFields();
      setLoading(true);
      const params: PaperPublishParams = {
        paperId: paper.paperId,
        courseId,
        title: values.title,
        description: values.description || undefined,
        taskType: values.taskType,
        tag: values.tag || undefined,
        weight: values.weight ?? 100,
        publishType: values.publishType,
        publishTime: values.publishType === PUBLISH_TYPE.SCHEDULED ? fmt(values.publishTime) : undefined,
        // 表单已校验必填，这里断言非空
        deadline: fmt(values.deadline)!,
        allowLate: values.allowLate ? 1 : 0,
        latePenalty: values.allowLate ? values.latePenalty : undefined,
        maxScore: values.maxScore ?? paper.totalScore ?? 100,
        allowResubmit: values.allowResubmit ? 1 : 0,
      };
      await publishPaper(params);
      message.success('发布成功，已创建任务');
      onSuccess();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [paper, courseId, form, onSuccess]);

  return (
    <Modal
      title="发布试卷"
      open={open}
      onCancel={onCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      okText="确认发布"
      width={640}
      destroyOnClose
    >
      <div style={{ marginBottom: 16, color: '#8c8c8c' }}>
        试卷：<span style={{ color: '#262626' }}>{paper?.title}</span>
        <span style={{ marginLeft: 12 }}>总分 {paper?.totalScore ?? '-'} 分</span>
      </div>
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          title: paper?.title,
          taskType: TASK_TYPE.QUIZ,
          publishType: PUBLISH_TYPE.IMMEDIATE,
          allowLate: false,
          allowResubmit: false,
          weight: 100,
          maxScore: paper?.totalScore ?? 100,
        }}
      >
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="title" label="任务标题" rules={[{ required: true, message: '请输入任务标题' }]}>
              <Input maxLength={100} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="taskType" label="任务类型" rules={[{ required: true }]}>
              <Select>
                <Option value={TASK_TYPE.QUIZ}>{TASK_TYPE_LABELS[TASK_TYPE.QUIZ]}</Option>
                <Option value={TASK_TYPE.ASSIGNMENT}>{TASK_TYPE_LABELS[TASK_TYPE.ASSIGNMENT]}</Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="tag" label="标签">
              <Input placeholder="如：期中 / 重点" maxLength={30} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="weight" label="成绩权重（%）">
              <InputNumber min={0} max={100} precision={2} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="publishType" label="发布方式" rules={[{ required: true }]}>
          <Select>
            {Object.entries(PUBLISH_TYPE_LABELS).map(([value, label]) => (
              <Option key={value} value={value}>{label}</Option>
            ))}
          </Select>
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
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="deadline" label="截止时间" rules={[{ required: true, message: '请选择截止时间' }]}>
              <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="maxScore" label="满分">
              <InputNumber min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item name="allowLate" label="允许晚交" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Col>
          {allowLate && (
            <Col span={8}>
              <Form.Item
                name="latePenalty"
                label="晚交扣分（%）"
                rules={[{ required: true, message: '请输入晚交扣分比例' }]}
              >
                <InputNumber min={0.01} max={100} precision={2} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          )}
          <Col span={8}>
            <Form.Item name="allowResubmit" label="允许重交" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="description" label="任务说明">
          <Input.TextArea rows={3} placeholder="考试须知、任务要求等" maxLength={500} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

/** 试卷管理页：生成（手动/智能）、预览、发布 */
export default function PaperManage() {
  const [courseId, setCourseId] = useState<number | undefined>();
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [createOpen, setCreateOpen] = useState(false);
  const [editPaper, setEditPaper] = useState<ExamPaperVO | null>(null);
  const [previewPaperId, setPreviewPaperId] = useState<number | null>(null);
  const [publishPaper, setPublishPaper] = useState<ExamPaperVO | null>(null);

  const { data, isLoading, refetch } = usePaperPage(courseId ?? null, pageNum, pageSize);
  const previewQuery = usePaper(previewPaperId);

  const handleTableChange = useCallback((pagination: TablePaginationConfig) => {
    setPageNum(pagination.current || 1);
    setPageSize(pagination.pageSize || 10);
  }, []);

  /** 删除试卷 */
  const handleDelete = useCallback(async (paperId: number) => {
    try {
      await deletePaper(paperId);
      message.success('试卷已删除');
      refetch();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  }, [refetch]);

  const columns: ColumnsType<ExamPaperVO> = [
    {
      title: '试卷标题',
      dataIndex: 'title',
      key: 'title',
      width: 240,
      ellipsis: true,
      render: (text: string) => <span style={{ fontWeight: 500 }}>{text}</span>,
    },
    {
      title: '总分',
      dataIndex: 'totalScore',
      key: 'totalScore',
      width: 80,
      render: (v: number | null) => v ?? '-',
    },
    {
      title: '限时',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
      render: (v: number | null) => (v ? `${v} 分钟` : <Tag>不限时</Tag>),
    },
    {
      title: '乱序',
      dataIndex: 'shuffleQuestions',
      key: 'shuffleQuestions',
      width: 80,
      render: (v: number | null) => (v === 1 ? <Tag color="blue">乱序</Tag> : <Tag>顺序</Tag>),
    },
    {
      title: '关联任务',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 110,
      render: (v: number | null) => (v ? <Tag color="green">任务 #{v}</Tag> : <Tag>未发布</Tag>),
    },
    {
      title: '版本',
      dataIndex: 'version',
      key: 'version',
      width: 70,
      render: (v: number | null) => v ?? '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (v: string | null) => (v ? new Date(v).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_: unknown, record: ExamPaperVO) => (
        <Space size="small" wrap>
          <Tooltip title="预览试卷">
            <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => setPreviewPaperId(record.paperId)}>
              预览
            </Button>
          </Tooltip>
          <Tooltip title="编辑试卷">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              disabled={!!record.taskId}
              onClick={() => {
                setEditPaper(record);
                setCreateOpen(true);
              }}
            >
              编辑
            </Button>
          </Tooltip>
          <Tooltip title="发布为任务">
            <Button
              type="link"
              size="small"
              icon={<SendOutlined />}
              disabled={!!record.taskId}
              onClick={() => setPublishPaper(record)}
            >
              发布
            </Button>
          </Tooltip>
          <Popconfirm
            title="确认删除该试卷？"
            description="已发布关联任务的试卷不可删除。"
            onConfirm={() => handleDelete(record.paperId)}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />} disabled={!!record.taskId}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* ===== 工具栏 ===== */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col flex="auto">
            <Space wrap>
              <CourseSelect
                value={courseId}
                onChange={(id) => {
                  setCourseId(id);
                  setPageNum(1);
                }}
                allowOwnOnly
              />
              <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
                刷新
              </Button>
            </Space>
          </Col>
          <Col>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              disabled={!courseId}
              onClick={() => {
                setEditPaper(null);
                setCreateOpen(true);
              }}
            >
              新建试卷
            </Button>
          </Col>
        </Row>
      </Card>

      {/* ===== 试卷表格 ===== */}
      <Card>
        {!courseId ? (
          <div style={{ textAlign: 'center', padding: 60, color: '#8c8c8c' }}>请先选择课程</div>
        ) : (
          <Table<ExamPaperVO>
            rowKey="paperId"
            columns={columns}
            dataSource={data?.records || []}
            loading={isLoading}
            pagination={{
              current: pageNum,
              pageSize,
              total: data?.total || 0,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 份试卷`,
            }}
            onChange={handleTableChange}
            scroll={{ x: 1100 }}
          />
        )}
      </Card>

      {/* ===== 新建 / 编辑试卷弹窗 ===== */}
      {createOpen && courseId && (
        <PaperFormModal
          key={editPaper?.paperId ?? 'create'}
          open={createOpen}
          courseId={courseId}
          editPaper={editPaper}
          onCancel={() => {
            setCreateOpen(false);
            setEditPaper(null);
          }}
          onSuccess={() => {
            setCreateOpen(false);
            setEditPaper(null);
            refetch();
          }}
        />
      )}

      {/* ===== 发布弹窗 ===== */}
      {publishPaper && courseId && (
        <PublishModal
          key={publishPaper.paperId}
          open={!!publishPaper}
          paper={publishPaper}
          courseId={courseId}
          onCancel={() => setPublishPaper(null)}
          onSuccess={() => {
            setPublishPaper(null);
            refetch();
          }}
        />
      )}

      {/* ===== 预览抽屉 ===== */}
      <Drawer
        title={previewQuery.data?.title ?? '试卷预览'}
        open={!!previewPaperId}
        onClose={() => setPreviewPaperId(null)}
        width={720}
      >
        {previewQuery.isLoading ? (
          <div style={{ textAlign: 'center', padding: 60, color: '#8c8c8c' }}>加载中...</div>
        ) : (
          <>
            <Space style={{ marginBottom: 16 }} wrap>
              <Tag color="blue">总分 {previewQuery.data?.totalScore ?? '-'}</Tag>
              <Tag color="purple">
                {previewQuery.data?.duration ? `限时 ${previewQuery.data.duration} 分钟` : '不限时'}
              </Tag>
              <Tag color={previewQuery.data?.shuffleQuestions === 1 ? 'orange' : 'default'}>
                {previewQuery.data?.shuffleQuestions === 1 ? '乱序' : '顺序'}
              </Tag>
            </Space>
            {(previewQuery.data?.questions ?? []).map((q, i) => (
              <QuestionCard
                key={q.qId}
                index={i + 1}
                question={{ qType: q.qType, content: q.content, options: q.options, answer: q.answer, analysis: q.analysis, difficulty: q.difficulty, score: q.score }}
              />
            ))}
          </>
        )}
      </Drawer>
    </div>
  );
}
