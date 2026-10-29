import { useCallback, useState } from 'react';
import {
  Table, Button, Input, Select, Tag, Space, Card, Modal, Form,
  message, Row, Col, Tabs, Upload, Popconfirm, Tooltip, Radio, Checkbox,
} from 'antd';
import {
  PlusOutlined, SearchOutlined, ReloadOutlined, EditOutlined,
  DeleteOutlined, MinusCircleOutlined, UploadOutlined, InboxOutlined,
} from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import type { UploadProps } from 'antd';

/** antd Upload customRequest 的入参类型（不直接依赖内部 rc-upload 路径） */
type UploadRequestOption = Parameters<NonNullable<UploadProps['customRequest']>>[0];
import { useQuestionImportPage, useQuestionPage } from '../../hooks/useAssessment';
import {
  createQuestion, deleteQuestion, getQuestion, getQuestionImportDetail,
  updateQuestion, uploadQuestionFile,
} from '../../api/assessment';
import CourseSelect from '../../components/assessment/CourseSelect';
import {
  QUESTION_TYPE, QUESTION_TYPE_LABELS, QUESTION_TYPE_COLORS,
  DIFFICULTY_LABELS, DIFFICULTY_COLORS,
  IMPORT_STATUS_LABELS, IMPORT_STATUS_COLORS,
  formatReferenceAnswer,
} from '../../constants/assessment';
import type { QuestionCreateParams, QuestionVO, QuestionImportVO } from '../../api/types';

const { Option } = Select;
const { TextArea } = Input;

/** 选项字母：0 -> A */
const letter = (i: number) => String.fromCharCode(65 + i);

/** 题目新建/编辑弹窗 */
function QuestionFormModal({
  open, question, courseId, onCancel, onSuccess,
}: {
  open: boolean;
  question: QuestionVO | null;
  courseId: number;
  onCancel: () => void;
  onSuccess: () => void;
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const qType: string | undefined = Form.useWatch('qType', form);
  const isChoice = qType === QUESTION_TYPE.SINGLE_CHOICE || qType === QUESTION_TYPE.MULTIPLE_CHOICE;
  const isMulti = qType === QUESTION_TYPE.MULTIPLE_CHOICE;

  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const options = (values.options ?? [])
        .map((s: string) => (s ?? '').trim())
        .filter(Boolean);
      const base: QuestionCreateParams = {
        courseId,
        qType: values.qType,
        content: values.content,
        options: isChoice ? options : undefined,
        answer: isMulti ? (values.answer as string[]).join(',') : String(values.answer),
        analysis: values.analysis || undefined,
        difficulty: values.difficulty ?? 2,
        tags: values.tags ?? [],
      };
      if (question) {
        // 后端更新题目强制校验乐观锁版本号
        await updateQuestion({ ...base, qId: question.qId, version: question.version });
        message.success('题目已更新');
      } else {
        await createQuestion(base);
        message.success('题目创建成功');
      }
      onSuccess();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [form, courseId, question, isChoice, isMulti, onSuccess]);

  return (
    <Modal
      title={question ? '编辑题目' : '新建题目'}
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
          difficulty: 2,
          ...(question
            ? {
                ...question,
                // 多选答案以数组回填（用 question.qType 判断，避免挂载时 useWatch 尚未取值）
                answer:
                  question.qType === QUESTION_TYPE.MULTIPLE_CHOICE && question.answer
                    ? question.answer.split(',')
                    : question.answer,
              }
            : { qType: QUESTION_TYPE.SINGLE_CHOICE }),
        }}
      >
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="qType" label="题型" rules={[{ required: true, message: '请选择题型' }]}>
              <Select placeholder="请选择题型">
                {Object.entries(QUESTION_TYPE_LABELS).map(([value, label]) => (
                  <Option key={value} value={value}>{label}</Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="difficulty" label="难度" rules={[{ required: true }]}>
              <Radio.Group>
                {[1, 2, 3].map((d) => (
                  <Radio key={d} value={d}>{DIFFICULTY_LABELS[d]}</Radio>
                ))}
              </Radio.Group>
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="content" label="题干" rules={[{ required: true, message: '请输入题干' }]}>
          <TextArea rows={3} placeholder="题目内容" maxLength={1000} />
        </Form.Item>

        {/* 选项（仅选择题） */}
        {isChoice && (
          <Form.Item label="选项" required>
            <Form.List
              name="options"
              rules={[
                {
                  validator: async (_, options: string[]) => {
                    const valid = (options ?? []).filter((s) => s && s.trim());
                    if (valid.length < 2) {
                      throw new Error('选择题至少需要 2 个选项');
                    }
                  },
                },
              ]}
            >
              {(fields, { add, remove }, { errors }) => (
                <>
                  {fields.map((field, i) => (
                    <Space key={field.key} align="baseline" style={{ display: 'flex', marginBottom: 8 }}>
                      <span style={{ color: '#8c8c8c', width: 24 }}>{letter(i)}.</span>
                      <Form.Item
                        {...field}
                        rules={[{ required: true, message: '选项内容不能为空' }]}
                        style={{ marginBottom: 0, flex: 1 }}
                      >
                        <Input placeholder={`选项 ${letter(i)}`} maxLength={200} />
                      </Form.Item>
                      {fields.length > 2 && (
                        <MinusCircleOutlined onClick={() => remove(field.name)} style={{ color: '#ff4d4f' }} />
                      )}
                    </Space>
                  ))}
                  <Form.Item style={{ marginBottom: 0 }}>
                    <Button type="dashed" onClick={() => add()} icon={<PlusOutlined />} block>
                      添加选项
                    </Button>
                    <Form.ErrorList errors={errors} />
                  </Form.Item>
                </>
              )}
            </Form.List>
          </Form.Item>
        )}

        {/* 答案（按题型切换） */}
        <Form.Item shouldUpdate={(prev, cur) => prev.qType !== cur.qType || prev.options !== cur.options} noStyle>
          {({ getFieldValue }) => {
            const type: string = getFieldValue('qType');
            const options: string[] = (getFieldValue('options') ?? []).filter((s: string) => s && s.trim());
            if (type === QUESTION_TYPE.SINGLE_CHOICE) {
              return (
                <Form.Item name="answer" label="正确答案" rules={[{ required: true, message: '请选择正确答案' }]}>
                  <Radio.Group>
                    {options.map((opt, i) => (
                      <Radio key={i} value={String(i)}>
                        {letter(i)}. {opt}
                      </Radio>
                    ))}
                  </Radio.Group>
                </Form.Item>
              );
            }
            if (type === QUESTION_TYPE.MULTIPLE_CHOICE) {
              return (
                <Form.Item name="answer" label="正确答案（可多选）" rules={[{ required: true, message: '请选择正确答案' }]}>
                  <Checkbox.Group>
                    {options.map((opt, i) => (
                      <Checkbox key={i} value={String(i)}>
                        {letter(i)}. {opt}
                      </Checkbox>
                    ))}
                  </Checkbox.Group>
                </Form.Item>
              );
            }
            if (type === QUESTION_TYPE.JUDGE) {
              return (
                <Form.Item name="answer" label="正确答案" rules={[{ required: true, message: '请选择正确答案' }]}>
                  <Radio.Group>
                    <Radio value="1">对</Radio>
                    <Radio value="0">错</Radio>
                  </Radio.Group>
                </Form.Item>
              );
            }
            if (type === QUESTION_TYPE.FILL_BLANK) {
              return (
                <Form.Item name="answer" label="参考答案" rules={[{ required: true, message: '请输入参考答案' }]}>
                  <Input placeholder="填空题参考答案" maxLength={200} />
                </Form.Item>
              );
            }
            return (
              <Form.Item name="answer" label="参考答案" rules={[{ required: true, message: '请输入参考答案' }]}>
                <TextArea rows={3} placeholder="简答题参考答案 / 评分要点" maxLength={1000} />
              </Form.Item>
            );
          }}
        </Form.Item>

        <Form.Item name="analysis" label="解析">
          <TextArea rows={3} placeholder="答案解析（可选）" maxLength={1000} />
        </Form.Item>
        <Form.Item name="tags" label="标签">
          <Select mode="tags" placeholder="输入后回车添加标签（可选）" open={false} suffixIcon={null} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

/** 题库页：题目管理 + 批量导入 */
export default function QuestionBank() {
  const [courseId, setCourseId] = useState<number | undefined>();
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [query, setQuery] = useState<{ qType?: string; difficulty?: number; keyword?: string }>({});
  const [searchInput, setSearchInput] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editQuestion, setEditQuestion] = useState<QuestionVO | null>(null);

  // 导入记录
  const [importPageNum, setImportPageNum] = useState(1);
  const [importPageSize, setImportPageSize] = useState(10);
  const [errorModalOpen, setErrorModalOpen] = useState(false);
  const [errorList, setErrorList] = useState<QuestionImportVO | null>(null);
  const [uploading, setUploading] = useState(false);

  const { data, isLoading, refetch } = useQuestionPage(courseId ?? null, pageNum, pageSize, query);
  const importQuery = useQuestionImportPage(courseId ?? null, importPageNum, importPageSize);

  /** 搜索 */
  const handleSearch = useCallback(() => {
    setPageNum(1);
    setQuery((prev) => ({ ...prev, keyword: searchInput || undefined }));
  }, [searchInput]);

  /** 分页变化 */
  const handleTableChange = useCallback((pagination: TablePaginationConfig) => {
    setPageNum(pagination.current || 1);
    setPageSize(pagination.pageSize || 10);
  }, []);

  /** 打开编辑（拉取完整题目） */
  const openEdit = useCallback(async (qId: number) => {
    try {
      const q = await getQuestion(qId);
      setEditQuestion(q);
      setModalOpen(true);
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  }, []);

  /** 删除题目 */
  const handleDelete = useCallback(async (qId: number) => {
    try {
      await deleteQuestion(qId);
      message.success('题目已删除');
      refetch();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  }, [refetch]);

  /** 上传题目文件 */
  const handleUpload = useCallback(async (option: UploadRequestOption) => {
    if (!courseId) {
      message.warning('请先选择课程');
      option.onError?.(new Error('请先选择课程'));
      return;
    }
    setUploading(true);
    try {
      const resp = await uploadQuestionFile(courseId, option.file as File);
      message.success(`导入完成：共 ${resp.importCount ?? 0} 题，成功 ${resp.successCount ?? 0} 题，失败 ${resp.failCount ?? 0} 题`);
      option.onSuccess?.(resp);
      importQuery.refetch();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
      option.onError?.(err as Error);
    } finally {
      setUploading(false);
    }
  }, [courseId, importQuery]);

  /** 查看导入错误明细 */
  const openErrors = useCallback(async (id: number) => {
    try {
      const detail = await getQuestionImportDetail(id);
      setErrorList(detail);
      setErrorModalOpen(true);
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  }, []);

  const columns: ColumnsType<QuestionVO> = [
    {
      title: '题干',
      dataIndex: 'content',
      key: 'content',
      width: 320,
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
      width: 100,
      render: (v: string) => <Tag color={QUESTION_TYPE_COLORS[v] || 'default'}>{QUESTION_TYPE_LABELS[v] || v}</Tag>,
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      key: 'difficulty',
      width: 80,
      render: (v: number | null) =>
        v ? <Tag color={DIFFICULTY_COLORS[v] || 'default'}>{DIFFICULTY_LABELS[v] || v}</Tag> : '-',
    },
    {
      title: '答案',
      dataIndex: 'answer',
      key: 'answer',
      width: 140,
      ellipsis: true,
      render: (v: string, record: QuestionVO) => (
        <Tooltip title={formatReferenceAnswer(record.qType, v)} placement="topLeft">
          <span style={{ color: '#52c41a' }}>{formatReferenceAnswer(record.qType, v)}</span>
        </Tooltip>
      ),
    },
    {
      title: '标签',
      dataIndex: 'tags',
      key: 'tags',
      width: 160,
      render: (tags: string[] | null) => (
        <Space size={4} wrap>
          {(tags ?? []).slice(0, 3).map((t) => <Tag key={t} style={{ fontSize: 12 }}>{t}</Tag>)}
          {(tags ?? []).length > 3 && <span style={{ fontSize: 12, color: '#8c8c8c' }}>+{tags!.length - 3}</span>}
        </Space>
      ),
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
      width: 140,
      fixed: 'right',
      render: (_: unknown, record: QuestionVO) => (
        <Space size="small">
          <Tooltip title="编辑题目">
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record.qId)}>
              编辑
            </Button>
          </Tooltip>
          <Popconfirm
            title="确认删除该题目？"
            description="试卷中已引用的题目请谨慎删除。"
            onConfirm={() => handleDelete(record.qId)}
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

  const importColumns: ColumnsType<QuestionImportVO> = [
    {
      title: '文件格式',
      dataIndex: 'fileFormat',
      key: 'fileFormat',
      width: 100,
      render: (v: string | null) => (v === 'excel' ? 'Excel' : v === 'word' ? 'Word' : v || '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (v: string) => (
        <Tag color={IMPORT_STATUS_COLORS[v] || 'default'}>{IMPORT_STATUS_LABELS[v] || v}</Tag>
      ),
    },
    {
      title: '总数',
      dataIndex: 'importCount',
      key: 'importCount',
      width: 80,
      render: (v: number | null) => v ?? '-',
    },
    {
      title: '成功',
      dataIndex: 'successCount',
      key: 'successCount',
      width: 80,
      render: (v: number | null) => <span style={{ color: '#52c41a' }}>{v ?? '-'}</span>,
    },
    {
      title: '失败',
      dataIndex: 'failCount',
      key: 'failCount',
      width: 80,
      render: (v: number | null) => <span style={{ color: v ? '#ff4d4f' : undefined }}>{v ?? '-'}</span>,
    },
    {
      title: '导入时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (v: string | null) => (v ? new Date(v).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: unknown, record: QuestionImportVO) => (
        <Button
          type="link"
          size="small"
          disabled={!record.failCount}
          onClick={() => openErrors(record.id)}
        >
          查看错误
        </Button>
      ),
    },
  ];

  const questionTab = (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col flex="auto">
            <Space wrap>
              <Select
                placeholder="题型"
                value={query.qType}
                onChange={(v) => {
                  setPageNum(1);
                  setQuery((prev) => ({ ...prev, qType: v }));
                }}
                style={{ width: 130 }}
                allowClear
              >
                {Object.entries(QUESTION_TYPE_LABELS).map(([value, label]) => (
                  <Option key={value} value={value}>{label}</Option>
                ))}
              </Select>
              <Select
                placeholder="难度"
                value={query.difficulty}
                onChange={(v) => {
                  setPageNum(1);
                  setQuery((prev) => ({ ...prev, difficulty: v }));
                }}
                style={{ width: 110 }}
                allowClear
              >
                {[1, 2, 3].map((d) => (
                  <Option key={d} value={d}>{DIFFICULTY_LABELS[d]}</Option>
                ))}
              </Select>
              <Input
                placeholder="搜索题干关键词"
                prefix={<SearchOutlined />}
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onPressEnter={handleSearch}
                style={{ width: 200 }}
                allowClear
              />
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                搜索
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  setSearchInput('');
                  setQuery({});
                  setPageNum(1);
                }}
              >
                重置
              </Button>
            </Space>
          </Col>
          <Col>
            <Space>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                disabled={!courseId}
                onClick={() => {
                  setEditQuestion(null);
                  setModalOpen(true);
                }}
              >
                新建题目
              </Button>
              <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
                刷新
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Card>
        {!courseId ? (
          <div style={{ textAlign: 'center', padding: 60, color: '#8c8c8c' }}>请先选择课程</div>
        ) : (
          <Table<QuestionVO>
            rowKey="qId"
            columns={columns}
            dataSource={data?.records || []}
            loading={isLoading}
            pagination={{
              current: pageNum,
              pageSize,
              total: data?.total || 0,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 道题`,
            }}
            onChange={handleTableChange}
            scroll={{ x: 1150 }}
          />
        )}
      </Card>
    </div>
  );

  const importTab = (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col flex="auto">
            <Space direction="vertical" size={4}>
              <Upload
                accept=".xlsx,.xls,.docx"
                showUploadList={false}
                customRequest={handleUpload}
                disabled={!courseId || uploading}
              >
                <Button
                  type="primary"
                  icon={<UploadOutlined />}
                  loading={uploading}
                  disabled={!courseId}
                >
                  上传题目文件
                </Button>
              </Upload>
              <span style={{ fontSize: 12, color: '#8c8c8c' }}>
                支持 .xlsx / .xls / .docx 格式，导入成功后题目自动加入题库
              </span>
            </Space>
          </Col>
          <Col>
            <Button icon={<ReloadOutlined />} onClick={() => importQuery.refetch()}>
              刷新
            </Button>
          </Col>
        </Row>
      </Card>

      <Card>
        {!courseId ? (
          <div style={{ textAlign: 'center', padding: 60, color: '#8c8c8c' }}>请先选择课程</div>
        ) : importQuery.data?.records?.length ? (
          <Table<QuestionImportVO>
            rowKey="id"
            columns={importColumns}
            dataSource={importQuery.data?.records || []}
            loading={importQuery.isLoading}
            pagination={{
              current: importPageNum,
              pageSize: importPageSize,
              total: importQuery.data?.total || 0,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条导入记录`,
            }}
            onChange={(pagination) => {
              setImportPageNum(pagination.current || 1);
              setImportPageSize(pagination.pageSize || 10);
            }}
          />
        ) : (
          <div style={{ textAlign: 'center', padding: 60 }}>
            <InboxOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />
            <div style={{ color: '#8c8c8c', marginTop: 12 }}>暂无导入记录</div>
          </div>
        )}
      </Card>
    </div>
  );

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Row align="middle" gutter={16}>
          <Col>
            <span style={{ color: '#8c8c8c', marginRight: 8 }}>课程</span>
          </Col>
          <Col>
            <CourseSelect
              value={courseId}
              onChange={(id) => {
                setCourseId(id);
                setPageNum(1);
                setImportPageNum(1);
              }}
              allowOwnOnly
            />
          </Col>
        </Row>
      </Card>

      <Tabs
        items={[
          { key: 'manage', label: '题目管理', children: questionTab },
          { key: 'import', label: '导入记录', children: importTab },
        ]}
      />

      {/* ===== 新建 / 编辑题目弹窗 ===== */}
      {modalOpen && courseId && (
        <QuestionFormModal
          key={editQuestion?.qId ?? 'create'}
          open={modalOpen}
          question={editQuestion}
          courseId={courseId}
          onCancel={() => {
            setModalOpen(false);
            setEditQuestion(null);
          }}
          onSuccess={() => {
            setModalOpen(false);
            setEditQuestion(null);
            refetch();
          }}
        />
      )}

      {/* ===== 导入错误明细弹窗 ===== */}
      <Modal
        title={`导入错误明细（共 ${errorList?.failCount ?? 0} 条）`}
        open={errorModalOpen}
        onCancel={() => setErrorModalOpen(false)}
        footer={<Button onClick={() => setErrorModalOpen(false)}>关闭</Button>}
        width={560}
      >
        <Table<{ rowNo: number; reason: string }>
          rowKey="rowNo"
          size="small"
          columns={[
            { title: '行号', dataIndex: 'rowNo', key: 'rowNo', width: 80 },
            { title: '失败原因', dataIndex: 'reason', key: 'reason' },
          ]}
          dataSource={errorList?.errors ?? []}
          pagination={false}
        />
      </Modal>
    </div>
  );
}
