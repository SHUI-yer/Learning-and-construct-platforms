import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Button, Input, Select, Tag, Space, Card, Modal, Form,
  message, Tooltip, Row, Col,
} from 'antd';
import {
  PlusOutlined, SearchOutlined, ReloadOutlined, EditOutlined,
  EyeOutlined, CheckCircleOutlined, CloseCircleOutlined, CopyOutlined,
} from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { useCourses } from '../../hooks/useCourses';
import { createCourse, auditCourse, copyCourse } from '../../api/course';
import { useAuth } from '../../hooks/useAuth';
import { ROLES } from '../../constants/roles';
import {
  COURSE_STATUS, COURSE_STATUS_LABELS, COURSE_STATUS_COLORS,
  AUDIT_STATUS,
} from '../../constants/course';
import type { CourseVO, CourseQueryParams, CourseCreateParams } from '../../api/types';

const { Option } = Select;
const { TextArea } = Input;

export default function CourseList() {
  const navigate = useNavigate();
  const { roleCode, userId } = useAuth();

  // 查询参数
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [query, setQuery] = useState<CourseQueryParams>({});
  const [searchInput, setSearchInput] = useState('');

  // 创建课程弹窗
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [createLoading, setCreateLoading] = useState(false);

  // 审核弹窗
  const [auditOpen, setAuditOpen] = useState(false);
  const [auditCourseId, setAuditCourseId] = useState<number | null>(null);
  const [auditAction, setAuditAction] = useState<number>(AUDIT_STATUS.APPROVE);
  const [auditRemark, setAuditRemark] = useState('');
  const [auditLoading, setAuditLoading] = useState(false);

  // 查询课程列表
  const { data, isLoading, refetch } = useCourses(pageNum, pageSize, query);

  const isAdmin = roleCode === ROLES.SUPER_ADMIN || roleCode === ROLES.DEPT_ADMIN;
  const isTeacher = roleCode === ROLES.TEACHER;

  /** 搜索 */
  const handleSearch = useCallback(() => {
    setPageNum(1);
    setQuery((prev: CourseQueryParams) => ({ ...prev, courseName: searchInput || undefined }));
  }, [searchInput]);

  /** 重置筛选 */
  const handleReset = useCallback(() => {
    setSearchInput('');
    setQuery({});
    setPageNum(1);
  }, []);

  /** 分页变化 */
  const handleTableChange = useCallback((pagination: TablePaginationConfig) => {
    setPageNum(pagination.current || 1);
    setPageSize(pagination.pageSize || 10);
  }, []);

  /** 创建课程 */
  const handleCreate = useCallback(async () => {
    try {
      const values = await createForm.validateFields();
      setCreateLoading(true);
      const params: CourseCreateParams = {
        courseName: values.courseName,
        courseCode: values.courseCode || undefined,
        semester: values.semester || undefined,
        isPublic: values.isPublic ?? 1,
        description: values.description || undefined,
      };
      await createCourse(params);
      message.success('课程创建成功');
      setCreateOpen(false);
      createForm.resetFields();
      refetch();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setCreateLoading(false);
    }
  }, [createForm, refetch]);

  /** 打开审核弹窗 */
  const openAudit = useCallback((courseId: number, action: number) => {
    setAuditCourseId(courseId);
    setAuditAction(action);
    setAuditRemark('');
    setAuditOpen(true);
  }, []);

  /** 执行审核 */
  const handleAudit = useCallback(async () => {
    if (!auditCourseId) return;
    setAuditLoading(true);
    try {
      await auditCourse({
        courseId: auditCourseId,
        auditStatus: auditAction,
        auditRemark: auditRemark || undefined,
      });
      message.success(auditAction === AUDIT_STATUS.APPROVE ? '审核通过' : '已驳回');
      setAuditOpen(false);
      refetch();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setAuditLoading(false);
    }
  }, [auditCourseId, auditAction, auditRemark, refetch]);

  /** 复制课程 */
  const handleCopy = useCallback(async (course: CourseVO) => {
    try {
      const newId = await copyCourse({
        sourceCourseId: course.courseId,
        courseName: `${course.courseName}（副本）`,
      });
      message.success(`课程已复制，新课程 ID: ${newId}`);
      refetch();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  }, [refetch]);

  // ===== 表格列定义 =====
  const columns: ColumnsType<CourseVO> = [
    {
      title: '课程名称',
      dataIndex: 'courseName',
      key: 'courseName',
      width: 200,
      ellipsis: true,
      render: (text: string, record: CourseVO) => (
        <a onClick={() => navigate(`/courses/${record.courseId}`)} style={{ fontWeight: 500 }}>
          {text}
        </a>
      ),
    },
    {
      title: '课程编号',
      dataIndex: 'courseCode',
      key: 'courseCode',
      width: 120,
      ellipsis: true,
    },
    {
      title: '学期',
      dataIndex: 'semester',
      key: 'semester',
      width: 120,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number | null) => (
        <Tag color={COURSE_STATUS_COLORS[status ?? 0] || 'default'}>
          {COURSE_STATUS_LABELS[status ?? 0] || '未知'}
        </Tag>
      ),
    },
    {
      title: '公开',
      dataIndex: 'isPublic',
      key: 'isPublic',
      width: 80,
      render: (v: number | null) => (
        <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '公开' : '私有'}</Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (v: string | null) => v ? new Date(v).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      fixed: 'right',
      render: (_: unknown, record: CourseVO) => (
        <Space size="small" wrap>
          <Tooltip title="查看详情">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => navigate(`/courses/${record.courseId}`)}
            >
              详情
            </Button>
          </Tooltip>

          {/* 教师可编辑自己的课程，管理员可编辑所有 */}
          {(isAdmin || (isTeacher && record.teacherId === userId)) && (
            <Tooltip title="编辑课程">
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => navigate(`/courses/${record.courseId}?mode=edit`)}
              >
                编辑
              </Button>
            </Tooltip>
          )}

          {/* 管理员可审核待审核课程 */}
          {isAdmin && record.status === COURSE_STATUS.PENDING && (
            <>
              <Tooltip title="审核通过">
                <Button
                  type="link"
                  size="small"
                  icon={<CheckCircleOutlined />}
                  style={{ color: '#52c41a' }}
                  onClick={() => openAudit(record.courseId, AUDIT_STATUS.APPROVE)}
                >
                  通过
                </Button>
              </Tooltip>
              <Tooltip title="驳回">
                <Button
                  type="link"
                  size="small"
                  icon={<CloseCircleOutlined />}
                  danger
                  onClick={() => openAudit(record.courseId, AUDIT_STATUS.REJECT)}
                >
                  驳回
                </Button>
              </Tooltip>
            </>
          )}

          {/* 复制课程 */}
          <Tooltip title="复制为新课程">
            <Button
              type="link"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => handleCopy(record)}
            >
              复制
            </Button>
          </Tooltip>
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
              <Input
                placeholder="搜索课程名称"
                prefix={<SearchOutlined />}
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onPressEnter={handleSearch}
                style={{ width: 240 }}
                allowClear
              />
              <Select
                placeholder="课程状态"
                value={query.status}
                onChange={(v) => {
                  setPageNum(1);
                  setQuery((prev: CourseQueryParams) => ({ ...prev, status: v }));
                }}
                style={{ width: 140 }}
                allowClear
              >
                <Option value={COURSE_STATUS.DRAFT}>草稿</Option>
                <Option value={COURSE_STATUS.PENDING}>待审核</Option>
                <Option value={COURSE_STATUS.APPROVED}>已发布</Option>
                <Option value={COURSE_STATUS.REJECTED}>已驳回</Option>
              </Select>
              <Input
                placeholder="学期（如 2025-1）"
                value={query.semester}
                onChange={(e) => {
                  setQuery((prev: CourseQueryParams) => ({ ...prev, semester: e.target.value || undefined }));
                }}
                style={{ width: 160 }}
                allowClear
              />
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                搜索
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>
                重置
              </Button>
            </Space>
          </Col>
          <Col>
            <Space>
              {/* 教师和管理员可以创建课程 */}
              {(isAdmin || isTeacher) && (
                <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
                  新建课程
                </Button>
              )}
              <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
                刷新
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* ===== 课程表格 ===== */}
      <Card>
        <Table<CourseVO>
          rowKey="courseId"
          columns={columns}
          dataSource={data?.records || []}
          loading={isLoading}
          pagination={{
            current: pageNum,
            pageSize,
            total: data?.total || 0,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 门课程`,
          }}
          onChange={handleTableChange}
          scroll={{ x: 1100 }}
        />
      </Card>

      {/* ===== 新建课程弹窗 ===== */}
      <Modal
        title="新建课程"
        open={createOpen}
        onCancel={() => {
          setCreateOpen(false);
          createForm.resetFields();
        }}
        onOk={handleCreate}
        confirmLoading={createLoading}
        width={600}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="courseName"
            label="课程名称"
            rules={[{ required: true, message: '请输入课程名称' }]}
          >
            <Input placeholder="如：高等数学" maxLength={100} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="courseCode" label="课程编号">
                <Input placeholder="如：MATH101" maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="semester" label="学期">
                <Input placeholder="如：2025-1" maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="isPublic" label="是否公开" initialValue={1}>
            <Select>
              <Option value={1}>公开</Option>
              <Option value={0}>私有</Option>
            </Select>
          </Form.Item>
          <Form.Item name="description" label="课程描述">
            <TextArea rows={4} placeholder="简要描述课程内容和目标" maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      {/* ===== 审核弹窗 ===== */}
      <Modal
        title={auditAction === AUDIT_STATUS.APPROVE ? '审核通过确认' : '驳回课程'}
        open={auditOpen}
        onCancel={() => setAuditOpen(false)}
        onOk={handleAudit}
        confirmLoading={auditLoading}
        okText={auditAction === AUDIT_STATUS.APPROVE ? '确认通过' : '确认驳回'}
        okButtonProps={auditAction === AUDIT_STATUS.REJECT ? { danger: true } : {}}
        destroyOnClose
      >
        <div style={{ marginBottom: 16 }}>
          <p>
            {auditAction === AUDIT_STATUS.APPROVE
              ? '确认通过该课程审核？通过后课程将对外发布。'
              : '确认驳回该课程？驳回后教师需要修改后重新提交。'}
          </p>
        </div>
        <Form layout="vertical">
          <Form.Item label={auditAction === AUDIT_STATUS.APPROVE ? '审核备注（可选）' : '驳回原因'}>
            <Input.TextArea
              rows={3}
              value={auditRemark}
              onChange={(e) => setAuditRemark(e.target.value)}
              placeholder={
                auditAction === AUDIT_STATUS.REJECT
                  ? '请填写驳回原因，帮助教师改进'
                  : '可选备注信息'
              }
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
