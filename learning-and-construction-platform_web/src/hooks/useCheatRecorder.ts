import { useCallback, useEffect, useRef, useState } from 'react';
import { recordCheatLog } from '../api/assessment';
import { CHEAT_TYPE } from '../constants/assessment';
import type { CheatEventItem } from '../api/types';

/** 同类型事件在该时间窗内只记一次（切屏会同时触发 blur + visibilitychange） */
const DEDUPE_WINDOW_MS = 3000;

/**
 * LocalDateTime 字符串：后端 Jackson 反序列化 LocalDateTime 不接受末尾的 'Z'，
 * 因此这里用本地时间格式，与页面 dayjs.format('YYYY-MM-DDTHH:mm:ss') 保持一致。
 */
function toLocalDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

interface UseCheatRecorderOptions {
  /** 当前提交ID；非限时考试交卷前为 null（此时事件先本地缓冲） */
  submissionId: number | null;
  /** 是否处于作答中（false 时移除监听） */
  active: boolean;
}

interface CheatRecorderResult {
  /** 已捕获的异常行为条数（用于页面提示） */
  count: number;
  /**
   * 上报缓冲的事件。
   * 不传 id 时用当前的 submissionId；没有 submissionId 则保留缓冲等交卷后补报。
   */
  flush: (submissionId?: number) => Promise<unknown>;
}

/**
 * 考试防作弊记录器
 *
 * 后端只接受 tab_switch / copy_paste / right_click / over_time 四种类型
 * （over_time 由服务端在超时拒交时自行落库，前端不上报）。
 */
export function useCheatRecorder({ submissionId, active }: UseCheatRecorderOptions): CheatRecorderResult {
  const [count, setCount] = useState(0);

  const eventsRef = useRef<CheatEventItem[]>([]);
  const lastAtRef = useRef<Record<string, number>>({});
  // 供 flush 读取最新 submissionId，避免其身份随 props 变化
  const submissionIdRef = useRef<number | null>(submissionId);
  useEffect(() => {
    submissionIdRef.current = submissionId;
  }, [submissionId]);

  const record = useCallback((cheatType: string, detail: string) => {
    const now = Date.now();
    const last = lastAtRef.current[cheatType];
    if (last !== undefined && now - last < DEDUPE_WINDOW_MS) {
      return;
    }
    lastAtRef.current[cheatType] = now;
    eventsRef.current.push({
      cheatType,
      detail,
      occurredAt: toLocalDateTime(new Date(now)),
    });
    setCount((c) => c + 1);
  }, []);

  // ===== 监听异常行为（仅作答阶段） =====
  useEffect(() => {
    if (!active) return;

    const onVisibilityChange = () => {
      if (document.hidden) record(CHEAT_TYPE.TAB_SWITCH, '切换标签页或最小化窗口');
    };
    const onBlur = () => record(CHEAT_TYPE.TAB_SWITCH, '考试窗口失去焦点');
    const onCopy = () => record(CHEAT_TYPE.COPY_PASTE, '复制题目内容');
    const onPaste = () => record(CHEAT_TYPE.COPY_PASTE, '粘贴内容到作答区');
    const onContextMenu = (e: MouseEvent) => {
      // 考试中屏蔽右键菜单并留痕
      e.preventDefault();
      record(CHEAT_TYPE.RIGHT_CLICK, '触发右键菜单');
    };

    document.addEventListener('visibilitychange', onVisibilityChange);
    window.addEventListener('blur', onBlur);
    document.addEventListener('copy', onCopy);
    document.addEventListener('paste', onPaste);
    document.addEventListener('contextmenu', onContextMenu);

    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange);
      window.removeEventListener('blur', onBlur);
      document.removeEventListener('copy', onCopy);
      document.removeEventListener('paste', onPaste);
      document.removeEventListener('contextmenu', onContextMenu);
    };
  }, [active, record]);

  const flush = useCallback(async (overrideId?: number) => {
    const sid = overrideId ?? submissionIdRef.current;
    // 还没有 submissionId：保留缓冲，等交卷后用返回的 id 补报
    if (!sid) return null;

    const events = eventsRef.current;
    if (events.length === 0) return null;

    // 先取走再上报，避免重复提交
    eventsRef.current = [];
    try {
      return await recordCheatLog({ submissionId: sid, events });
    } catch (e) {
      // 上报失败则放回缓冲，下次 flush 再试
      eventsRef.current = [...events, ...eventsRef.current];
      throw e;
    }
  }, []);

  return { count, flush };
}
