import { useCallback, useMemo, useState } from 'react';
import {
  Table, Button, Tag, Space, Card, Tabs, InputNumber, Select, message,
  Row, Col, Typography, Popconfirm, Empty, Alert, Spin,
} from 'antd';
import {
  ReloadOutlined, SaveOutlined, CalculatorOutlined, CheckCircleOutlined,
  DownloadOutlined, PlusOutlined, DeleteOutlined, TrophyOutlined,
} from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { useGradeDesign, useGradeSummaryPage } from '../../hooks/useAssessment';
import {
  confirmGradeSummary, exportGradeSummary, saveGradeDesign, summarizeGrades,
} from '../../api/assessment';
import CourseSelect from '../../components/assessment/CourseSelect';
import { TASK_TYPE_LABELS } from '../../constants/assessment';
import type { GradeSummaryVO } from '../../api/types';

const { Title } = Typography;

/** 成绩构成可选项（与 TaskVO.taskType 一致，Grade.componentType 存的就是它） */
const COMPONENT_OPTIONS = Object.entries(TASK_TYPE_LABELS).map(([value, label]) => ({
  value,
  label,
}));

interface DesignRow {
  key: string;
  componentType: string;
  weight: number;
}

let rowSeq = 0;
const nextKey = () => `row-${++rowSeq}`;

/** 成绩构成：可编辑表格（新增/删除/改权重），保存前校验权重合计 = 100 */
function DesignTab({ courseId }: { courseId: number }) {
  const { data, isLoading, refetch } = useGradeDesign(courseId);
  // 已保存的构成由接口数据派生；本地编辑（draft）存在时覆盖它，保存后清空回到服务端数据
  const serverRows: DesignRow[] = useMemo(
    () =>
      (data ?? [])
        .slice()
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map((d) => ({ key: `saved-${d.schemeId}`, componentType: d.componentType, weight: d.weight })),
    [data]
  );
  const [draft, setDraft] = useState<DesignRow[] | null>(null);
  const rows = draft ?? serverRows;
  const setRows = (updater: (prev: DesignRow[]) => DesignRow[]) => setDraft(updater(rows));
  const [saving, setSaving] = useState(false);

  const totalWeight = useMemo(() => rows.reduce((sum, r) => sum + (r.weight || 0), 0), [rows]);
  const weightValid = totalWeight === 100;

  const handleSave = useCallback(async () => {
    if (!rows.length) {
      message.warning('请至少添加一项成绩构成');
      return;
    }
    if (!weightValid) {
      message.warning(`权重合计必须为 100，当前为 ${totalWeight}`);
      return;
    }
    if (rows.some((r) => !r.componentType)) {
      message.warning('请为每一行选择组成部分');
      return;
    }
    setSaving(true);
    try {
      await saveGradeDesign(
        courseId,
        rows.map((r, i) => ({
          componentType: r.componentType,
          weight: r.weight,
          sortOrder: i + 1,
        }))
      );
      message.success('成绩构成已保存');
      await refetch();
      // 保存成功：丢弃本地草稿，改由接口数据渲染（保证 key/顺序与服务端一致）
      setDraft(null);
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setSaving(false);
    }
  }, [courseId, rows, weightValid, totalWeight, refetch]);

  const columns: ColumnsType<DesignRow> = [
    {
      title: '组成部分',
      dataIndex: 'componentType',
      key: 'componentType',
      render: (v: string, r: DesignRow) => (
        <Select
          value={v || undefined}
          onChange={(val) =>
            setRows((prev) => prev.map((x) => (x.key === r.key ? { ...x, componentType: val } : x)))
          }
          options={COMPONENT_OPTIONS}
          placeholder="选择任务类型"
          style={{ width: 200 }}
        />
      ),
    },
    {
      title: '权重（%）',
      dataIndex: 'weight',
      key: 'weight',
      width: 180,
      render: (v: number, r: DesignRow) => (
        <InputNumber
          min={0}
          max={100}
          value={v}
          onChange={(val) =>
            setRows((prev) => prev.map((x) => (x.key === r.key ? { ...x, weight: val ?? 0 } : x)))
          }
          style={{ width: 120 }}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 90,
      render: (_: unknown, r: DesignRow) => (
        <Button
          type="link"
          size="small"
          danger
          icon={<DeleteOutlined />}
          onClick={() => setRows((prev) => prev.filter((x) => x.key !== r.key))}
        >
          删除
        </Button>
      ),
    },
  ];

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 40 }}>
        <Spin />
      </div>
    );
  }

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="成绩构成决定课程最终成绩的加权方式：各部分权重之和必须为 100%，未配置时汇总成绩为空。"
      />
      <Table<DesignRow>
        rowKey="key"
        columns={columns}
        dataSource={rows}
        pagination={false}
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="尚未配置成绩构成，点击下方「添加组成部分」开始"
            />
          ),
        }}
      />
      <Space style={{ marginTop: 16 }}>
        <Button
          icon={<PlusOutlined />}
          onClick={() => setRows((prev) => [...prev, { key: nextKey(), componentType: '', weight: 0 }])}
        >
          添加组成部分
        </Button>
        <Button
          type="primary"
          icon={<SaveOutlined />}
          loading={saving}
          onClick={handleSave}
          disabled={!weightValid}
        >
          保存
        </Button>
        <Tag color={weightValid ? 'success' : 'error'}>权重合计：{totalWeight}%</Tag>
        {!weightValid && (
          <span style={{ color: '#8c8c8c', fontSize: 12 }}>需调整为 100% 才能保存</span>
        )}
      </Space>
    </div>
  );
}

/** 成绩汇总：汇总 → 确认（生成排名）→ 导出 Excel */
function SummaryTab({ courseId }: { courseId: number }) {
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [busy, setBusy] = useState<'summarize' | 'confirm' | 'export' | null>(null);
  const { data, isLoading, refetch } = useGradeSummaryPage(courseId, pageNum, pageSize);

  const rows = data?.records ?? [];
  const confirmed = rows.length > 0 && rows.every((r) => r.confirmedAt != null);

  const handleSummarize = useCallback(async () => {
    setBusy('summarize');
    try {
      await summarizeGrades(courseId);
      message.success('成绩汇总完成');
      refetch();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setBusy(null);
    }
  }, [courseId, refetch]);

  const handleConfirm = useCallback(async () => {
    setBusy('confirm');
    try {
      await confirmGradeSummary(courseId);
      message.success('成绩已确认并生成排名');
      refetch();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setBusy(null);
    }
  }, [courseId, refetch]);

  const handleExport = useCallback(async () => {
    setBusy('export');
    try {
      await exportGradeSummary(courseId);
      message.success('成绩单已开始下载');
      refetch();
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    } finally {
      setBusy(null);
    }
  }, [courseId, refetch]);

  const columns: ColumnsType<GradeSummaryVO> = [
    {
      title: '排名',
      dataIndex: 'rankInCourse',
      key: 'rankInCourse',
      width: 90,
      render: (v: number | null) =>
        v != null ? (
          <Space size={4}>
            <TrophyOutlined style={{ color: v <= 3 ? '#faad14' : '#bfbfbf' }} />
            <b>{v}</b>
          </Space>
        ) : (
          <span style={{ color: '#bfbfbf' }}>未排名</span>
        ),
    },
    {
      title: '学生ID',
      dataIndex: 'studentId',
      key: 'studentId',
      width: 110,
      render: (v: number) => `#${v}`,
    },
    {
      title: '最终成绩',
      dataIndex: 'finalScore',
      key: 'finalScore',
      width: 130,
      render: (v: number | null) => (
        <b style={{ color: '#1677ff', fontSize: 15 }}>{v ?? '-'}</b>
      ),
    },
    {
      title: '状态',
      dataIndex: 'confirmedAt',
      key: 'confirmedAt',
      width: 130,
      render: (v: string | null) =>
        v ? <Tag color="success">已确认</Tag> : <Tag color="processing">待确认</Tag>,
    },
    {
      title: '确认时间',
      dataIndex: 'confirmedAt',
      key: 'confirmedTime',
      width: 190,
      render: (v: string | null) => (v ? new Date(v).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '导出',
      dataIndex: 'isExported',
      key: 'isExported',
      width: 90,
      render: (v: number | null) => (v === 1 ? <Tag color="blue">已导出</Tag> : '-'),
    },
  ];

  return (
    <div>
      <Row gutter={[12, 12]} align="middle" style={{ marginBottom: 16 }}>
        <Col flex="auto">
          <Space size={8} wrap>
            <Popconfirm
              title="重新汇总会覆盖未确认的旧成绩，已确认记录不会被改动。继续？"
              onConfirm={handleSummarize}
              okText="继续汇总"
              cancelText="取消"
            >
              <Button icon={<CalculatorOutlined />} loading={busy === 'summarize'}>
                执行成绩汇总
              </Button>
            </Popconfirm>
            <Popconfirm
              title="确认后将生成课程排名，且汇总不再覆盖已确认成绩。继续？"
              onConfirm={handleConfirm}
              okText="确认成绩"
              cancelText="取消"
              disabled={rows.length === 0}
            >
              <Button
                icon={<CheckCircleOutlined />}
                loading={busy === 'confirm'}
                disabled={rows.length === 0}
              >
                确认成绩并排名
              </Button>
            </Popconfirm>
            <Button
              icon={<DownloadOutlined />}
              loading={busy === 'export'}
              onClick={handleExport}
              disabled={rows.length === 0}
            >
              导出成绩单
            </Button>
          </Space>
        </Col>
        <Col>
          <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
            刷新
          </Button>
        </Col>
      </Row>

      {rows.length === 0 && !isLoading && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="暂无汇总数据"
          description="请先确认「成绩构成」权重合计为 100%，并确保该课程已有批阅后的成绩记录（测验交卷或教师批阅后生成），再执行汇总。"
        />
      )}

      <Table<GradeSummaryVO>
        rowKey="id"
        columns={columns}
        dataSource={rows}
        loading={isLoading}
        pagination={{
          current: pageNum,
          pageSize,
          total: data?.total || 0,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 名学生`,
        }}
        onChange={(pagination: TablePaginationConfig) => {
          setPageNum(pagination.current || 1);
          setPageSize(pagination.pageSize || 10);
        }}
        locale={{ emptyText: '暂无汇总数据' }}
      />
      {confirmed && (
        <div style={{ marginTop: 12, color: '#8c8c8c', fontSize: 12 }}>
          当前页记录均已确认；再次汇总不会覆盖已确认成绩。
        </div>
      )}
    </div>
  );
}

/** 成绩管理（教师）：成绩构成配置 + 成绩汇总/确认/导出 */
export default function GradeManage() {
  const [courseId, setCourseId] = useState<number | undefined>();

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>成绩管理</Title>

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 12]} align="middle">
          <Col>
            <CourseSelect value={courseId} onChange={setCourseId} width={280} placeholder="请选择课程" />
          </Col>
          <Col flex="auto">
            <span style={{ color: '#8c8c8c', fontSize: 13 }}>
              先配置成绩构成（权重合计 100%），再执行汇总与确认；确认后学生端可见课程最终成绩与排名。
            </span>
          </Col>
        </Row>
      </Card>

      {!courseId ? (
        <Card>
          <Empty description="请先选择课程" />
        </Card>
      ) : (
        <Card>
          <Tabs
            items={[
              {
                key: 'design',
                label: '成绩构成',
                // key 随课程变化，切课程时重建组件，避免上一门课的本地草稿残留
                children: <DesignTab key={courseId} courseId={courseId} />,
              },
              {
                key: 'summary',
                label: '成绩汇总',
                children: <SummaryTab key={courseId} courseId={courseId} />,
              },
            ]}
          />
        </Card>
      )}
    </div>
  );
}
