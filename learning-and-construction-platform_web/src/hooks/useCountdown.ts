import { useEffect, useRef, useState } from 'react';

/** 秒 -> mm:ss（超过 1 小时为 hh:mm:ss） */
function formatRemaining(seconds: number): string {
  const s = Math.max(0, seconds);
  const hours = Math.floor(s / 3600);
  const minutes = Math.floor((s % 3600) / 60);
  const secs = s % 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  return hours > 0 ? `${pad(hours)}:${pad(minutes)}:${pad(secs)}` : `${pad(minutes)}:${pad(secs)}`;
}

interface CountdownResult {
  /** 剩余秒数 */
  remaining: number;
  /** 格式化后的剩余时间 */
  text: string;
  /** 是否已到截止时间 */
  expired: boolean;
}

/**
 * 限时倒计时
 *
 * 以服务端返回的 deadline 为基准（ExamPage 用 ExamStartVO.deadline），
 * 每秒刷新；到点后回调 onExpire 一次（用于自动交卷）。
 */
export function useCountdown(deadline: string | null, onExpire: () => void): CountdownResult {
  const [remaining, setRemaining] = useState(0);
  // 已触发过到点回调，避免 setInterval 每次 tick 重复交卷
  const firedRef = useRef(false);
  // 用 ref 持有最新回调，避免把 onExpire 放进 effect 依赖导致定时器被反复重建
  const onExpireRef = useRef(onExpire);
  useEffect(() => {
    onExpireRef.current = onExpire;
  }, [onExpire]);

  useEffect(() => {
    if (!deadline) return;
    firedRef.current = false;

    const tick = () => {
      const diff = Math.floor((new Date(deadline).getTime() - Date.now()) / 1000);
      const next = diff > 0 ? diff : 0;
      setRemaining(next);
      if (next <= 0 && !firedRef.current) {
        firedRef.current = true;
        onExpireRef.current();
      }
    };

    // 首次计算放进回调，避免在 effect 体内同步 setState（会触发级联渲染）
    const first = window.setTimeout(tick, 0);
    const timer = window.setInterval(tick, 1000);
    return () => {
      window.clearTimeout(first);
      window.clearInterval(timer);
    };
  }, [deadline]);

  return {
    // deadline 为空（未开始作答）时对外恒为 0
    remaining: deadline ? remaining : 0,
    text: formatRemaining(deadline ? remaining : 0),
    expired: !!deadline && remaining <= 0,
  };
}
