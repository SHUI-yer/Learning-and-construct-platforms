import { useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card, Tree, Tag, Button, Descriptions, Breadcrumb, Space,
  Spin, Empty, Typography, Modal, Form, Input, InputNumber, Select,
  message, Popconfirm, Tooltip, Badge,
} from 'antd';
import {
  EditOutlined, PlusOutlined, DeleteOutlined,
  PlayCircleOutlined, FileTextOutlined, LinkOutlined, PictureOutlined,
  FilePdfOutlined, FolderOutlined, DownOutlined,
} from '@ant-design/icons';
import { useCourse, useChapterTree } from '../../hooks/useCourses';
import { updateCourse, addChapter, deleteChapter, addResource, deleteResource } from '../../api/course';
import { useAuth } from '../../hooks/useAuth';
import { ROLES } from '../../constants/roles';
import {
  COURSE_STATUS_LABELS, COURSE_STATUS_COLORS,
  getResourceTypeLabel, getResourceTypeColor,
} from '../../constants/course';
import type { ChapterTreeVO, ContentResource } from '../../api/types';

const { Title, Text } = Typography;
const { TextArea } = Input;

/** 资源类型 → 图标 */
function getResourceIcon(type: string | null) {
  switch (type) {
    case 'video': return <PlayCircleOutlined style={{ color: '#1677ff' }} />;
    case 'document': return <FileTextOutlined style={{ color: '#52c41a' }} />;
    case 'pdf': return <FilePdfOutlined style={{ color: '#fa541c' }} />;
    case 'link': return <LinkOutlined style={{ color: '#13c2c2' }} />;
    case 'image': return <PictureOutlined style={{ color: '#722ed1' }} />;
    default: return <FileTextOutlined style={{ color: '#8c8c8c' }} />;
  }
}

/** 格式化时长（秒 → mm:ss） */
function formatDuration(seconds: number | null): string {
  if (!seconds) return '';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export default function CourseDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const courseId = Number(id);

  const { roleCode, userId } = useAuth();
  const isAdmin = roleCode === ROLES.SUPER_ADMIN || roleCode === ROLES.DEPT_ADMIN;
  const isTeacher = roleCode === ROLES.TEACHER;

  // 课程数据
  const { data: course, isLoading: courseLoading } = useCourse(courseId);
  const { data: chapters, isLoading: treeLoading, refetch: refetchTree } = useChapterTree(courseId);

  // 编辑课程弹窗
  const [editOpen, setEditOpen] = useState(false);
  const [editForm] = Form.useForm();
  const [editLoading, setEditLoading] = useState(false);

  // 添加章节弹窗
  const [chapterOpen, setChapterOpen] = useState(false);
  const [chapterForm] = Form.useForm();
  const [chapterLoading, setChapterLoading] = useState(false);
  const [parentChapterId, setParentChapterId] = useState<number | null>(null);

  // 添加资源弹窗
  const [resourceOpen, setResourceOpen] = useState(false);
  const [resourceForm] = Form.useForm();
  const [resourceLoading, setResourceLoading] = useState(false);
  const [resourceChapterId, setResourceChapterId] = useState<number | null>(null);

  /** 判断当前用户是否可以编辑此课程 */
  const canEdit = isAdmin || (isTeacher && course?.teacherId === userId);

  /** 打开编辑课程弹窗 */
  const openEdit = useCallback(() => {
    if (!course) return;
    editForm.setFieldsValue({
      courseName: course.courseName,
      courseCode: course.courseCode,
      semester: course.semester,
      isPublic: course.isPublic ?? 1,
      description: course.description,
    });
    setEditOpen(true);
  }, [course, editForm]);

  /** 保存课程编辑 */
  const handleEditSave = useCallback(async () => {
    if (!course) return;
    try {
      const values = await editForm.validateFields();
      setEditLoading(true);
      await updateCourse({
        courseId: course.courseId,
        version: course.version,
        ...values,
      });
      message.success('课程已更新');
      setEditOpen(false);
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setEditLoading(false);
    }
  }, [course, editForm]);

  /** 添加章节 */
  const handleAddChapter = useCallback(async (parentId: number | null) => {
    setParentChapterId(parentId);
    chapterForm.resetFields();
    setChapterOpen(true);
  }, [chapterForm]);

  /** 提交章节 */
  const handleChapterSubmit = useCallback(async () => {
    try {
      const values = await chapterForm.validateFields();
      setChapterLoading(true);
      await addChapter({
        courseId,
        title: values.title,
        parentId: parentChapterId ?? undefined,
      });
      message.success('章节已添加');
      setChapterOpen(false);
      chapterForm.resetFields();
      refetchTree();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setChapterLoading(false);
    }
  }, [courseId, parentChapterId, chapterForm, refetchTree]);

  /** 删除章节 */
  const handleDeleteChapter = useCallback(async (chapterId: number) => {
    try {
      await deleteChapter(chapterId);
      message.success('章节已删除');
      refetchTree();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  }, [refetchTree]);

  /** 添加资源 */
  const openAddResource = useCallback((chapterId: number) => {
    setResourceChapterId(chapterId);
    resourceForm.resetFields();
    setResourceOpen(true);
  }, [resourceForm]);

  /** 提交资源 */
  const handleResourceSubmit = useCallback(async () => {
    if (resourceChapterId === null) return;
    try {
      const values = await resourceForm.validateFields();
      setResourceLoading(true);
      await addResource({
        chapterId: resourceChapterId,
        resourceType: values.resourceType || undefined,
        title: values.title,
        url: values.url || undefined,
        contentText: values.contentText || undefined,
        duration: values.duration || undefined,
      });
      message.success('资源已添加');
      setResourceOpen(false);
      resourceForm.resetFields();
      refetchTree();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setResourceLoading(false);
    }
  }, [resourceChapterId, resourceForm, refetchTree]);

  /** 删除资源 */
  const handleDeleteResource = useCallback(async (resourceId: number) => {
    try {
      await deleteResource(resourceId);
      message.success('资源已删除');
      refetchTree();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  }, [refetchTree]);

  /** 将章节树转为 Ant Design Tree 数据 */
  const buildTreeData = (nodes: ChapterTreeVO[]): any[] => {
    return nodes.map((node) => ({
      key: `chapter-${node.chapterId}`,
      title: (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', paddingRight: 8 }}>
          <span style={{ fontWeight: 500 }}>{node.title}</span>
          {canEdit && (
            <Space size={4} onClick={(e) => e.stopPropagation()}>
              <Tooltip title="添加子章节">
                <Button type="text" size="small" icon={<PlusOutlined />} onClick={() => handleAddChapter(node.chapterId)} />
              </Tooltip>
              <Tooltip title="添加资源">
                <Button type="text" size="small" icon={<FileTextOutlined />} onClick={() => openAddResource(node.chapterId)} />
              </Tooltip>
              <Popconfirm title="确认删除该章节？其下子章节和资源也将一并删除。" onConfirm={() => handleDeleteChapter(node.chapterId)} okText="删除" cancelText="取消" okButtonProps={{ danger: true }}>
                <Button type="text" size="small" icon={<DeleteOutlined />} danger />
              </Popconfirm>
            </Space>
          )}
        </div>
      ),
      children: [
        // 子章节
        ...buildTreeData(node.children || []),
        // 资源节点
        ...(node.resourceList || []).map((res: ContentResource) => ({
          key: `resource-${res.resourceId}`,
          isLeaf: true,
          title: (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', paddingRight: 8 }}>
              <Space size={8}>
                {getResourceIcon(res.resourceType)}
                <span style={{ color: '#595959' }}>{res.title || '未命名资源'}</span>
                <Tag color={getResourceTypeColor(res.resourceType)} style={{ fontSize: 11 }}>
                  {getResourceTypeLabel(res.resourceType)}
                </Tag>
                {res.duration ? (
                  <Text type="secondary" style={{ fontSize: 12 }}>{formatDuration(res.duration)}</Text>
                ) : null}
              </Space>
              <Space size={4} onClick={(e) => e.stopPropagation()}>
                {res.url && (
                  <Tooltip title="打开/下载">
                    <Button type="text" size="small" onClick={() => window.open(res.url!, '_blank')}>
                      <LinkOutlined />
                    </Button>
                  </Tooltip>
                )}
                {canEdit && (
                  <Popconfirm title="确认删除该资源？" onConfirm={() => handleDeleteResource(res.resourceId)} okText="删除" cancelText="取消" okButtonProps={{ danger: true }}>
                    <Button type="text" size="small" icon={<DeleteOutlined />} danger />
                  </Popconfirm>
                )}
              </Space>
            </div>
          ),
        })),
      ],
    }));
  };

  if (courseLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 100 }}>
        <Spin size="large" tip="加载课程信息..." />
      </div>
    );
  }

  if (!course) {
    return (
      <Empty description="课程不存在或已被删除">
        <Button type="primary" onClick={() => navigate(-1)}>返回</Button>
      </Empty>
    );
  }

  const treeData = chapters ? buildTreeData(chapters) : [];

  return (
    <div>
      {/* ===== 面包屑 ===== */}
      <Breadcrumb style={{ marginBottom: 16 }} items={[
        { title: <a onClick={() => navigate('/')}>首页</a> },
        { title: <a onClick={() => navigate('/courses')}>课程管理</a> },
        { title: course.courseName },
      ]} />

      {/* ===== 课程信息卡片 ===== */}
      <Card style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <Space align="center" style={{ marginBottom: 8 }}>
              <Title level={4} style={{ margin: 0 }}>{course.courseName}</Title>
              <Tag color={COURSE_STATUS_COLORS[course.status ?? 0] || 'default'}>
                {COURSE_STATUS_LABELS[course.status ?? 0] || '未知'}
              </Tag>
              <Tag color={course.isPublic === 1 ? 'green' : 'default'}>
                {course.isPublic === 1 ? '公开' : '私有'}
              </Tag>
            </Space>
            <Descriptions size="small" column={4} style={{ marginTop: 8 }}>
              {course.courseCode && <Descriptions.Item label="课程编号">{course.courseCode}</Descriptions.Item>}
              {course.semester && <Descriptions.Item label="学期">{course.semester}</Descriptions.Item>}
              <Descriptions.Item label="创建时间">
                {course.createdAt ? new Date(course.createdAt).toLocaleString('zh-CN') : '-'}
              </Descriptions.Item>
              {course.updatedAt && (
                <Descriptions.Item label="更新时间">
                  {new Date(course.updatedAt).toLocaleString('zh-CN')}
                </Descriptions.Item>
              )}
            </Descriptions>
            {course.description && (
              <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
                {course.description}
              </Text>
            )}
          </div>
          {canEdit && (
            <Button icon={<EditOutlined />} onClick={openEdit}>
              编辑课程
            </Button>
          )}
        </div>
      </Card>

      {/* ===== 章节 & 资源 ===== */}
      <Card
        title={
          <Space>
            <FolderOutlined />
            <span>章节与资源</span>
            <Badge count={chapters?.length || 0} style={{ backgroundColor: '#1677ff' }} />
          </Space>
        }
        extra={
          canEdit && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAddChapter(null)}>
              添加章节
            </Button>
          )
        }
      >
        {treeLoading ? (
          <div style={{ textAlign: 'center', padding: 60 }}>
            <Spin tip="加载章节树..." />
          </div>
        ) : treeData.length === 0 ? (
          <Empty description="暂无章节，点击上方按钮添加" />
        ) : (
          <Tree
            treeData={treeData}
            defaultExpandAll
            showLine={{ showLeafIcon: false }}
            switcherIcon={<DownOutlined />}
            blockNode
            style={{ padding: '8px 0' }}
          />
        )}
      </Card>

      {/* ===== 编辑课程弹窗 ===== */}
      <Modal
        title="编辑课程"
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={handleEditSave}
        confirmLoading={editLoading}
        width={600}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="courseName" label="课程名称" rules={[{ required: true, message: '请输入课程名称' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="courseCode" label="课程编号">
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="semester" label="学期">
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="isPublic" label="是否公开">
            <Select>
              <Select.Option value={1}>公开</Select.Option>
              <Select.Option value={0}>私有</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="description" label="课程描述">
            <TextArea rows={4} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      {/* ===== 添加章节弹窗 ===== */}
      <Modal
        title={parentChapterId ? '添加子章节' : '添加顶级章节'}
        open={chapterOpen}
        onCancel={() => { setChapterOpen(false); chapterForm.resetFields(); }}
        onOk={handleChapterSubmit}
        confirmLoading={chapterLoading}
        destroyOnClose
      >
        <Form form={chapterForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="title" label="章节标题" rules={[{ required: true, message: '请输入章节标题' }]}>
            <Input placeholder="如：第一章 绪论" maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>

      {/* ===== 添加资源弹窗 ===== */}
      <Modal
        title="添加资源"
        open={resourceOpen}
        onCancel={() => { setResourceOpen(false); resourceForm.resetFields(); }}
        onOk={handleResourceSubmit}
        confirmLoading={resourceLoading}
        destroyOnClose
      >
        <Form form={resourceForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="title" label="资源标题" rules={[{ required: true, message: '请输入资源标题' }]}>
            <Input placeholder="如：课程导论视频" maxLength={200} />
          </Form.Item>
          <Form.Item name="resourceType" label="资源类型">
            <Select placeholder="选择类型" allowClear>
              <Select.Option value="video">视频</Select.Option>
              <Select.Option value="document">文档</Select.Option>
              <Select.Option value="pdf">PDF</Select.Option>
              <Select.Option value="link">链接</Select.Option>
              <Select.Option value="image">图片</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="url" label="资源链接">
            <Input placeholder="https://..." />
          </Form.Item>
          <Form.Item name="duration" label="时长（秒）">
            <InputNumber min={0} style={{ width: '100%' }} placeholder="视频/音频时长" />
          </Form.Item>
          <Form.Item name="contentText" label="内容描述">
            <TextArea rows={3} placeholder="可选：资源内容摘要或笔记" maxLength={1000} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
