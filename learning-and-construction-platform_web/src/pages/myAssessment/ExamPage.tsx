import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  Button, Tag, Space, Card, Modal, Result, Alert, Spin, Progress, message,
} from 'antd';
import {
  ArrowLeftOutlined, SendOutlined, SafetyCertificateOutlined, RedoOutlined,
} from '@ant-design/icons';
import { useAuth } from '../../hooks/useAuth';
import { useCountdown } from '../../hooks/useCountdown';
import { useCheatRecorder } from '../../hooks/useCheatRecorder';
import {
  getPaper, getPaperPage, getSubmission, getSubmissionPage, getTask,
  startSubmission, submitSubmission,
} from '../../api/assessment';
import QuestionCard from '../../components/assessment/QuestionCard';
import AnswerInput from '../../components/assessment/AnswerInput';
import {
  SUBMISSION_STATUS_LABELS, SUBMISSION_STATUS_COLORS,
  TASK_TYPE, TASK_TYPE_LABELS, TASK_TYPE_COLORS, PUBLISH_TYPE,
} from '../../constants/assessment';
import type { ExamPaperVO, ExamStartVO, SubmissionVO, TaskVO } from '../../api/types';

type Phase = 'loading' | 'error' | 'confirm' | 'answering' | 'submit-confirm' | 'submitting' | 'result';

/** 翻页查找本人提交（最多查 5 页） */
async function findOwnSubmission(taskId: number, userId: number): Promise<SubmissionVO | null> {
  for (let page = 1; page <= 5; page++) {
    const res = await getSubmissionPage(taskId, page, 100);
    const own = res.records.find((s) => s.studentId === userId);
    if (own) return own;
    if (res.records.length < 100 || res.total <= page * 100) break;
  }
  return null;
}

/** 查找任务关联的试卷 */
async function findPaper(courseId: number, taskId: number): Promise<ExamPaperVO | null> {
  for (let page = 1; page <= 5; page++) {
    const res = await getPaperPage(courseId, page, 100);
    const match = res.records.find((p) => p.taskId === taskId);
    if (match) return getPaper(match.paperId);
    if (res.records.length < 100 || res.total <= page * 100) break;
  }
  return null;
}

/** 从提交详情重建作答 map */
function rebuildAnswers(submission: SubmissionVO): Record<number, string> {
  const map: Record<number, string> = {};
  (submission.answers ?? []).forEach((a) => {
    if (a.studentAnswer != null && a.studentAnswer !== '') {
      map[a.qId] = a.studentAnswer;
    }
  });
  return map;
}

const isTimed = (paper: ExamPaperVO | null) => !!paper?.duration && paper.duration > 0;

/** 本地答案草稿：刷新/误关页面不丢答案（仅存本机，不提交给后端） */
const draftKey = (taskId: number) => `exam_draft_${taskId}`;

function readDraft(taskId: number): Record<number, string> | null {
  try {
    const raw = localStorage.getItem(draftKey(taskId));
    return raw ? (JSON.parse(raw) as Record<number, string>) : null;
  } catch {
    return null;
  }
}

function clearDraft(taskId: number) {
  try {
    localStorage.removeItem(draftKey(taskId));
  } catch {
    // 隐私模式下 localStorage 不可用，忽略即可
  }
}

/** 在线考试页（学生）：开始（防作弊提示）→ 作答（倒计时 + 违规记录）→ 交卷确认 → 结果 */
export default function ExamPage() {
  const navigate = useNavigate();
  const { taskId: taskIdStr } = useParams<{ taskId: string }>();
  const [searchParams] = useSearchParams();
  const { userId } = useAuth();
  const taskId = Number(taskIdStr);
  const retake = searchParams.get('retake') === '1';

  const [phase, setPhase] = useState<Phase>('loading');
  const [errorMsg, setErrorMsg] = useState('');
  const [task, setTask] = useState<TaskVO | null>(null);
  const [paper, setPaper] = useState<ExamPaperVO | null>(null);
  const [own, setOwn] = useState<SubmissionVO | null>(null);
  const [startInfo, setStartInfo] = useState<ExamStartVO | null>(null);
  const [answers, setAnswers] = useState<Record<number, string>>({});
  const [submitting, setSubmitting] = useState(false);

  // 供交卷回调读取最新作答（在 effect 中同步，渲染期不写 ref）
  const answersRef = useRef(answers);
  useEffect(() => {
    answersRef.current = answers;
  }, [answers]);

  // 任务是否已截止：加载任务时算一次即可（渲染期调用 Date.now() 会让渲染不纯）
  const [deadlinePassed, setDeadlinePassed] = useState(false);
  // ===== 防作弊记录器（作答阶段挂监听） =====
  const { count: cheatCount, flush: flushCheat } = useCheatRecorder({
    submissionId: startInfo?.submissionId ?? null,
    active: phase === 'answering',
  });

  // ===== 交卷 =====
  const doSubmit = useCallback(async () => {
    const currentTaskId = taskId;
    const currentAnswers = answersRef.current;
    setSubmitting(true);
    setPhase('submitting');
    // 先尽力上报已缓冲的违规事件
    try {
      await flushCheat();
    } catch {
      // 忽略上报失败，不影响交卷
    }
    try {
      const entries = Object.entries(currentAnswers)
        .filter(([, v]) => v != null && v !== '')
        .map(([qId, studentAnswer]) => ({ qId: Number(qId), studentAnswer }));
      const resp = await submitSubmission({ taskId: currentTaskId, answers: entries });
      clearDraft(currentTaskId);
      // 非限时考试交卷后补报本地缓冲的违规事件
      try {
        await flushCheat(resp.submissionId);
      } catch {
        // 忽略
      }
      setOwn(resp);
      setPhase('result');
      message.success('交卷成功');
    } catch (err) {
      // 可能是超时被拒等：刷新提交状态判断
      const own2 = await findOwnSubmission(currentTaskId, userId!);
      if (own2 && own2.status === 'graded') {
        setOwn(own2);
        setPhase('result');
      } else {
        if (err instanceof Error) message.error(err.message);
        setPhase('answering');
      }
    } finally {
      setSubmitting(false);
    }
  }, [taskId, flushCheat, userId]);

  // ===== 倒计时（到点自动交卷） =====
  const { remaining, text, expired } = useCountdown(
    phase === 'answering' && startInfo ? startInfo.deadline : null,
    useCallback(() => {
      message.warning('考试时间到，系统将自动交卷');
      void doSubmit();
    }, [doSubmit])
  );

  // ===== 页面加载 =====
  useEffect(() => {
    let cancelled = false;
    (async () => {
      setPhase('loading');
      try {
        const t = await getTask(taskId);
        if (cancelled) return;
        if (!t || t.taskType !== TASK_TYPE.QUIZ) {
          setErrorMsg('该任务不支持在线作答');
          setPhase('error');
          return;
        }
        setTask(t);
        if (
          t.publishType !== PUBLISH_TYPE.IMMEDIATE &&
          (t.publishTime == null || Date.parse(t.publishTime) > Date.now())
        ) {
          setErrorMsg('任务尚未发布，暂不可作答');
          setPhase('error');
          return;
        }
        const p = await findPaper(t.courseId, taskId);
        if (cancelled) return;
        setPaper(p);
        const existing = await findOwnSubmission(taskId, userId!);
        if (cancelled) return;

        const isDeadlinePassed = t.deadline != null && Date.parse(t.deadline) < Date.now();
        setDeadlinePassed(isDeadlinePassed);

        // 已有批阅提交且非重考 → 直接进结果页
        if (existing?.status === 'graded' && !retake) {
          const detail = await getSubmission(existing.submissionId);
          if (cancelled) return;
          setOwn(detail);
          setPhase('result');
          return;
        }
        // 进行中 → 恢复作答（刷新页面场景）；服务端优先，本地草稿补空缺
        if (existing?.status === 'in_progress') {
          const detail = await getSubmission(existing.submissionId);
          if (cancelled) return;
          const draft = readDraft(taskId);
          setAnswers({ ...(draft ?? {}), ...rebuildAnswers(detail) });
          if (isTimed(p)) {
            const start = await startSubmission(taskId); // 幂等，返回原 deadline
            if (cancelled) return;
            setStartInfo(start);
          }
          setPhase('answering');
          return;
        }
        // 无提交 / 重考
        if (isDeadlinePassed) {
          setErrorMsg(retake ? '任务已截止，无法重新考试' : '任务已截止，无法作答');
          setPhase('error');
          return;
        }
        if (!p) {
          setErrorMsg('任务尚未关联试卷，无法作答');
          setPhase('error');
          return;
        }
        if ((p.questions ?? []).length === 0) {
          setErrorMsg('试卷未配置题目，无法作答');
          setPhase('error');
          return;
        }
        if (isTimed(p)) {
          setPhase('confirm'); // 限时：先展示考试须知 + 防作弊提示
        } else {
          setAnswers(readDraft(taskId) ?? {});
          setPhase('answering');
        }
      } catch (err) {
        if (!cancelled) {
          setErrorMsg(err instanceof Error ? err.message : '加载失败');
          setPhase('error');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [taskId]);

  /** 开始限时考试 */
  const handleStart = useCallback(async () => {
    try {
      const start = await startSubmission(taskId);
      setStartInfo(start);
      setAnswers(readDraft(taskId) ?? {});
      setPhase('answering');
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  }, [taskId]);

  /** 重新考试：清空上一轮答案与本地草稿 */
  const handleRetake = useCallback(() => {
    setOwn(null);
    setStartInfo(null);
    clearDraft(taskId);
    setAnswers({});
    if (isTimed(paper)) {
      setPhase('confirm');
    } else {
      setPhase('answering');
    }
  }, [paper, taskId]);

  const questions = useMemo(
    () => [...(paper?.questions ?? [])].sort((a, b) => a.sortOrder - b.sortOrder),
    [paper]
  );
  const answeredCount = useMemo(
    () => Object.values(answers).filter((v) => v != null && v !== '').length,
    [answers]
  );

  // 作答中每次变更即落地本地草稿，刷新/误关页面可恢复
  useEffect(() => {
    if (phase !== 'answering') return;
    try {
      localStorage.setItem(draftKey(taskId), JSON.stringify(answers));
    } catch {
      // 存储不可用（隐私模式/配额满）时静默降级，不影响作答
    }
  }, [answers, phase, taskId]);

  // ===== 加载 / 错误 =====
  if (phase === 'loading') {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" tip="试卷加载中...">
          <div style={{ height: 100 }} />
        </Spin>
      </div>
    );
  }
  if (phase === 'error') {
    return (
      <Result
        status="warning"
        title="无法作答"
        subTitle={errorMsg}
        extra={
          <Button type="primary" onClick={() => navigate('/my-assessment/tasks')}>
            返回我的任务
          </Button>
        }
      />
    );
  }

  // ===== 开始确认弹窗（限时） =====
  if (phase === 'confirm') {
    return (
      <Modal
        title="开始考试确认"
        open
        closable={false}
        maskClosable={false}
        onCancel={() => navigate('/my-assessment/tasks')}
        onOk={handleStart}
        okText="开始考试"
        cancelText="返回任务列表"
        width={560}
      >
        <Space direction="vertical" size={12} style={{ width: '100%', marginTop: 8 }}>
          <div>
            <div style={{ fontWeight: 500, marginBottom: 4 }}>考试信息</div>
            <div style={{ color: '#595959' }}>
              试卷：{paper?.title}
              {paper?.duration ? `，限时 ${paper.duration} 分钟` : ''}
              {task?.deadline ? `，任务截止 ${new Date(task.deadline).toLocaleString('zh-CN')}` : ''}
            </div>
            <div style={{ color: '#595959', marginTop: 4 }}>
              共 {questions.length} 题，满分 {paper?.totalScore ?? '-'} 分；开始后计时，倒计时结束自动交卷。
            </div>
          </div>
          <Alert
            type="warning"
            showIcon
            icon={<SafetyCertificateOutlined />}
            message="防作弊提示"
            description="考试期间系统将记录切屏切换、复制粘贴、右键操作等违规行为并同步给教师，请诚信作答。"
          />
        </Space>
      </Modal>
    );
  }

  // ===== 结果页 =====
  if (phase === 'result' && own) {
    const canRetake = task?.allowResubmit === 1 && !deadlinePassed;
    return (
      <div>
        <Card style={{ marginBottom: 16 }}>
          <Space size={24} wrap>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/my-assessment/tasks')}>
              返回我的任务
            </Button>
            <span style={{ fontWeight: 600, fontSize: 16 }}>{paper?.title ?? own.taskTitle ?? '考试结果'}</span>
            <Tag color={SUBMISSION_STATUS_COLORS[own.status] || 'default'}>
              {SUBMISSION_STATUS_LABELS[own.status] || own.status}
            </Tag>
            <span style={{ fontSize: 18 }}>
              得分：<b style={{ color: '#1677ff' }}>{own.score ?? '-'}</b>
              <span style={{ color: '#8c8c8c', fontSize: 14 }}> / {own.maxScore ?? '-'}</span>
            </span>
            {own.isLate === 1 && <Tag color="orange">晚交</Tag>}
            {own.isExcellent === 1 && <Tag color="gold">优秀范本</Tag>}
            <span style={{ color: '#8c8c8c', fontSize: 13 }}>
              提交于 {own.submittedAt ? new Date(own.submittedAt).toLocaleString('zh-CN') : '-'}
            </span>
            {canRetake && (
              <Button icon={<RedoOutlined />} onClick={handleRetake}>
                重新考试
              </Button>
            )}
          </Space>
          {own.teacherComment && (
            <Alert
              style={{ marginTop: 12 }}
              type="info"
              showIcon
              message="教师评语"
              description={own.teacherComment}
            />
          )}
        </Card>

        {(own.answers ?? []).map((a, i) => (
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

  // ===== 作答页 / 交卷确认 =====
  return (
    <div>
      {/* 头部：进度 + 倒计时 + 交卷 */}
      <Card style={{ marginBottom: 16 }}>
        <Space size={24} wrap style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space size={16} wrap>
            <span style={{ fontWeight: 600, fontSize: 16 }}>{paper?.title ?? '在线考试'}</span>
            <Tag color={TASK_TYPE_COLORS[TASK_TYPE.QUIZ]}>{TASK_TYPE_LABELS[TASK_TYPE.QUIZ]}</Tag>
            <span style={{ color: '#595959' }}>
              已答 <b style={{ color: '#1677ff' }}>{answeredCount}</b> / 共 {questions.length} 题
            </span>
            <span style={{ color: '#bfbfbf', fontSize: 12 }}>答案自动暂存本地，刷新不丢失</span>
          </Space>
          <Space size={24}>
            {startInfo && (
              <span
                style={{
                  fontSize: expired ? 22 : 18,
                  fontWeight: 700,
                  fontVariantNumeric: 'tabular-nums',
                  color: expired || remaining < 60 ? '#ff4d4f' : '#1677ff',
                }}
              >
                剩余时间 {text}
              </span>
            )}
            <Button
              type="primary"
              icon={<SendOutlined />}
              loading={submitting}
              onClick={() => setPhase('submit-confirm')}
            >
              交卷
            </Button>
          </Space>
        </Space>
      </Card>

      {/* 违规警示条 */}
      {cheatCount > 0 && (
        <Alert
          type="warning"
          showIcon
          icon={<SafetyCertificateOutlined />}
          message={`已记录 ${cheatCount} 次违规行为（切屏/复制/右键等），将同步给教师`}
          style={{ marginBottom: 16 }}
        />
      )}

      {/* 题目列表 */}
      {questions.map((q, i) => (
        <QuestionCard
          key={q.qId}
          index={i + 1}
          question={{
            qType: q.qType,
            content: q.content,
            options: q.options,
            score: q.score,
          }}
          answerNode={
            <AnswerInput
              qType={q.qType}
              options={q.options}
              value={answers[q.qId] ?? ''}
              onChange={(v) => setAnswers((prev) => ({ ...prev, [q.qId]: v }))}
            />
          }
        />
      ))}

      {/* 交卷确认弹窗 */}
      <Modal
        title="交卷确认"
        open={phase === 'submit-confirm'}
        onCancel={() => setPhase('answering')}
        onOk={() => {
          if (answeredCount === 0) {
            message.warning('请至少作答一题');
            return;
          }
          void doSubmit();
        }}
        okText="确认交卷"
        cancelText="继续作答"
        confirmLoading={submitting}
        width={480}
      >
        <div style={{ marginTop: 8 }}>
          <Progress
            percent={questions.length ? Math.round((answeredCount / questions.length) * 100) : 0}
            format={() => `${answeredCount}/${questions.length}`}
          />
          {answeredCount < questions.length && (
            <Alert
              type="warning"
              showIcon
              style={{ marginTop: 12 }}
              message={`还有 ${questions.length - answeredCount} 题未作答，确定交卷？`}
            />
          )}
          {answeredCount === questions.length && (
            <Alert type="success" showIcon style={{ marginTop: 12 }} message="全部题目已作答，可以交卷" />
          )}
        </div>
      </Modal>
    </div>
  );
}
