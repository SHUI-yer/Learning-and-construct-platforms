import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message } from 'antd';
import { useAuth } from '../hooks/useAuth';
import { login as loginApi } from '../api/auth';
import penSvg from '../assets/fountain-pen.svg';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({ username: '', password: '' });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  /** 表单基础校验 */
  const validate = (): string | null => {
    if (!formData.username.trim()) return '请输入账号';
    if (!formData.password) return '请输入密码';
    if (formData.password.length < 6) return '密码长度不能少于 6 位';
    return null;
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // 前端校验
    const errMsg = validate();
    if (errMsg) {
      message.warning(errMsg);
      return;
    }

    setLoading(true);
    try {
      // API 层已解包 Result<T>，直接拿到 LoginVO；code!=1 或网络错误会抛 ApiError
      const data = await loginApi({
        username: formData.username.trim(),
        password: formData.password,
      });

      // 保存完整用户信息（token + userId + roleCode 等）到 Zustand store & localStorage
      login(data);
      message.success(`欢迎回来，${data.realName || data.username}！`);
      // 登录成功后进入个人主页
      navigate('/profile', { replace: true });
    } catch (err) {
      // request.ts 的拦截器已对常见错误做了中文翻译，这里直接使用 err.message
      if (err instanceof Error && err.message) {
        // 针对不同错误码做更精细的提示
        if (err.message === '账号或密码错误') {
          message.error('账号或密码错误，请重新输入');
        } else if (err.message.includes('过期')) {
          message.error('登录已过期，请重新登录');
        } else {
          message.error(err.message);
        }
      } else {
        message.error('网络异常，请检查网络连接后重试');
      }
    } finally {
      setLoading(false);
    }
  };

  /** Enter 键快捷登录 */
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !loading) {
      const form = e.currentTarget.closest('form');
      if (form) form.requestSubmit();
    }
  };

  return (
    <div className="flex h-screen bg-[#313137]">
      <div className="w-[70%] flex h-screen items-center justify-center z-[100]">
        <div className="bg-black h-200 w-250 z-10">
          <div
            className="note-container"
            style={{
              flex: 1,
              position: 'relative',
              margin: '1.5rem',
              alignItems: 'center',
              justifyContent: 'center',
              background: 'rgb(255, 255, 255)',
              backgroundImage: 'linear-gradient(#e5e5e5 1px, transparent 1px)',
              backgroundSize: '100% 2rem',
            }}
          >
            {/* --- SVG 光线层 --- */}
            <svg
              className="absolute inset-0 w-full h-full pointer-events-none z-0"
              viewBox="0 0 100 100"
              preserveAspectRatio="none"
              style={{ overflow: 'visible' }}
            >
              <defs>
                <linearGradient id="goldLineGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#C9A24A" stopOpacity="0" />
                  <stop offset="10%" stopColor="#C9A24A" stopOpacity="0.2" />
                  <stop offset="90%" stopColor="#C9A24A" stopOpacity="1" />
                  <stop offset="100%" stopColor="#FFFACD" stopOpacity="1" />
                </linearGradient>

                <filter id="premiumGlow" x="-100%" y="-100%" width="300%" height="300%">
                  <feGaussianBlur in="SourceGraphic" stdDeviation="1.2" result="blur" />
                  <feComponentTransfer in="blur" result="processed">
                    <feFuncR type="linear" slope="1.5" intercept="0" />
                    <feFuncG type="linear" slope="1.5" intercept="0" />
                    <feFuncB type="linear" slope="1.2" intercept="0" />
                    <feFuncA type="gamma" amplitude="1.5" exponent="1.5" offset="0" />
                  </feComponentTransfer>
                  <feMerge>
                    <feMergeNode in="processed" />
                    <feMergeNode in="SourceGraphic" />
                  </feMerge>
                </filter>

                <filter id="xRayGlow" x="-200%" y="-200%" width="500%" height="500%">
                  <feGaussianBlur in="SourceGraphic" stdDeviation="0.15" result="core" />
                  <feComponentTransfer in="core" result="coreBoost">
                    <feFuncA type="linear" slope="3" />
                  </feComponentTransfer>
                  <feGaussianBlur in="SourceGraphic" stdDeviation="0.7" result="mid" />
                  <feComponentTransfer in="mid" result="midBoost">
                    <feFuncR type="linear" slope="2" />
                    <feFuncG type="linear" slope="2" />
                    <feFuncB type="linear" slope="1.4" />
                    <feFuncA type="linear" slope="1.8" />
                  </feComponentTransfer>
                  <feGaussianBlur in="SourceGraphic" stdDeviation="2" result="outer" />
                  <feComponentTransfer in="outer" result="outerBoost">
                    <feFuncR type="linear" slope="1.6" />
                    <feFuncG type="linear" slope="1.6" />
                    <feFuncB type="linear" slope="1" />
                    <feFuncA type="linear" slope="0.7" />
                  </feComponentTransfer>
                  <feMerge>
                    <feMergeNode in="outerBoost" />
                    <feMergeNode in="midBoost" />
                    <feMergeNode in="coreBoost" />
                    <feMergeNode in="SourceGraphic" />
                  </feMerge>
                </filter>
              </defs>

              <path
                d="M 0 0 L 0 100 L 100 100"
                fill="none"
                stroke="url(#goldLineGradient)"
                strokeWidth="0.5"
                strokeLinecap="square"
                strokeLinejoin="miter"
                filter="url(#premiumGlow)"
                className="animate-path-1"
              />

              <path
                d="M 0 0 L 100 0 L 100 100"
                fill="none"
                stroke="url(#goldLineGradient)"
                strokeWidth="0.5"
                strokeLinecap="square"
                strokeLinejoin="miter"
                filter="url(#premiumGlow)"
                className="animate-path-2"
              />

              <circle
                cx="100"
                cy="100"
                r="0.5"
                fill="#FFD700"
                filter="url(#premiumGlow)"
                className="animate-dot-ooze"
              />

              <line
                x1="99.5" y1="99.5" x2="100.5" y2="100.5"
                stroke="#FFFEF0"
                strokeWidth="0.08"
                strokeLinecap="round"
                filter="url(#xRayGlow)"
                className="animate-x-arm-1"
              />
              <line
                x1="99.5" y1="100.5" x2="100.5" y2="99.5"
                stroke="#FFFEF0"
                strokeWidth="0.08"
                strokeLinecap="round"
                filter="url(#xRayGlow)"
                className="animate-x-arm-2"
              />

              <style>{`
                .animate-path-1 {
                  stroke-dasharray: 200;
                  stroke-dashoffset: 200;
                  animation: drawPath 2s cubic-bezier(0.22, 1, 0.36, 1) forwards;
                }
                .animate-path-2 {
                  stroke-dasharray: 200;
                  stroke-dashoffset: 200;
                  animation: drawPath 2s cubic-bezier(0.22, 1, 0.36, 1) 0.2s forwards;
                }
                .animate-dot-ooze {
                  opacity: 0;
                  transform-origin: 100% 100%;
                  animation: oozeDot 0.8s ease-out 1.8s forwards;
                }
                .animate-x-arm-1,
                .animate-x-arm-2 {
                  opacity: 0;
                  transform-origin: 100% 100%;
                }
                .animate-x-arm-1 {
                  animation: xBreathe 2.2s ease-in-out 1.7s infinite;
                }
                .animate-x-arm-2 {
                  animation: xBreathe 2.2s ease-in-out 1.95s infinite;
                }
                @keyframes drawPath {
                  to { stroke-dashoffset: 0; }
                }
                @keyframes oozeDot {
                  0% {
                    opacity: 0;
                    filter: url(#premiumGlow) brightness(0.5);
                  }
                  100% {
                    opacity: 1;
                    filter: url(#premiumGlow) brightness(1.2);
                  }
                }
                @keyframes xBreathe {
                  0% {
                    opacity: 0.25;
                    transform: scale(0.5);
                  }
                  30% {
                    opacity: 1;
                    transform: scale(1.8);
                  }
                  50% {
                    opacity: 1;
                    transform: scale(1.8);
                  }
                  80% {
                    opacity: 0.35;
                    transform: scale(0.5);
                  }
                  100% {
                    opacity: 0.25;
                    transform: scale(0.5);
                  }
                }
              `}</style>
            </svg>

            <form onSubmit={handleSubmit}>
              <div
                className="note-content"
                style={{ padding: '8rem', textAlign: 'center' }}
              >
                <h1
                  style={{
                    top: '10%',
                    fontSize: '2.5rem',
                    fontWeight: 'bold',
                    marginBottom: '1rem',
                    color: '#111827',
                    letterSpacing: '0.1em',
                  }}
                >
                  WELCOME
                </h1>
                <p style={{ fontSize: '1.2rem', marginBottom: '2rem' }}>学习与建设平台</p>

                <div style={{ display: 'flex' }}>
                  <label
                    style={{
                      color: '#1118277a',
                      fontSize: '1.2rem',
                      fontWeight: 'bold',
                    }}
                  >
                    账号 / Account
                  </label>
                </div>
                <div style={{ display: 'flex' }}>
                  <input
                    name="username"
                    value={formData.username}
                    onChange={handleChange}
                    onKeyDown={handleKeyDown}
                    style={{
                      width: '90%',
                      height: '2.5rem',
                      marginLeft: '1rem',
                      border: 'none',
                      borderBottom: '1px solid #1118277a',
                      outline: 'none',
                      fontSize: '1.2rem',
                      fontWeight: 'bold',
                    }}
                    type="text"
                    placeholder="请输入账号"
                    autoComplete="username"
                    autoFocus
                    disabled={loading}
                  />
                </div>

                <div style={{ paddingTop: '1.5rem', display: 'flex' }}>
                  <label
                    style={{
                      color: '#1118277a',
                      fontSize: '1.2rem',
                      fontWeight: 'bold',
                    }}
                  >
                    密码 / Password
                  </label>
                </div>
                <div style={{ display: 'flex' }}>
                  <input
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    onKeyDown={handleKeyDown}
                    style={{
                      width: '90%',
                      height: '2.5rem',
                      marginLeft: '1rem',
                      border: 'none',
                      borderBottom: '1px solid #1118277a',
                      outline: 'none',
                      fontSize: '1.2rem',
                      fontWeight: 'bold',
                    }}
                    type="password"
                    placeholder="·······"
                    autoComplete="current-password"
                    disabled={loading}
                  />
                </div>

                <div style={{ paddingTop: '4rem' }}>
                  <button
                    type="submit"
                    disabled={loading}
                    className="login-btn w-full py-4 text-md font-bold uppercase tracking-widest rounded-sm shadow-lg bg-black text-white transition-all duration-300"
                    style={{
                      transform: 'scale(1)',
                      opacity: loading ? 0.7 : 1,
                      cursor: loading ? 'not-allowed' : 'pointer',
                    }}
                  >
                    {loading ? '登录中...' : '登 录'}
                  </button>
                </div>

                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-around',
                    paddingTop: '1rem',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      marginTop: '1rem',
                      fontSize: '1.2rem',
                      color: '#1118277a',
                      fontWeight: 'bold',
                      textDecoration: 'underline',
                      cursor: 'pointer',
                    }}
                    onClick={() => message.info('注册功能开发中，敬请期待')}
                  >
                    注册
                  </div>
                  <div
                    style={{
                      display: 'flex',
                      marginTop: '1rem',
                      fontSize: '1.2rem',
                      color: '#1118277a',
                      fontWeight: 'bold',
                      textDecoration: 'underline',
                      cursor: 'pointer',
                    }}
                    onClick={() => message.info('忘记密码功能开发中，敬请期待')}
                  >
                    忘记密码
                  </div>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
      <div className="w-[30%] flex h-screen items-center justify-center relative">
        {/* 金色氛围光晕 */}
        <div
          className="pen-glow absolute w-105 h-105 rounded-full pointer-events-none"
          style={{
            background:
              'radial-gradient(circle, rgba(242,193,78,0.16) 0%, rgba(201,162,74,0.06) 45%, transparent 72%)',
          }}
        />
        <img
          src={penSvg}
          alt="钢笔"
          className="pen-float relative"
          style={{
            height: '72vh',
            maxHeight: '780px',
            filter:
              'drop-shadow(0 18px 32px rgba(0,0,0,0.55)) drop-shadow(0 0 22px rgba(242,193,78,0.14))',
          }}
        />
      </div>
    </div>
  );
}
