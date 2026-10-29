import type { ReactNode } from 'react';
import { Card, Tag, Space, Typography, Divider } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, MinusCircleOutlined } from '@ant-design/icons';
import {
  QUESTION_TYPE, QUESTION_TYPE_LABELS, QUESTION_TYPE_COLORS,
  DIFFICULTY_LABELS, formatReferenceAnswer, indexToLetter,
} from '../../constants/assessment';

const { Text, Paragraph } = Typography;

export interface QuestionCardQuestion {
  qType?: string | null;
  content?: string | null;
  options?: string[] | null;
  /** 参考答案（下标编码）；作答中不传则不显示 */
  answer?: string | null;
  analysis?: string | null;
  difficulty?: number | null;
  score?: number | null;
}

export interface QuestionCardReview {
  studentAnswer?: string | null;
  isCorrect?: number | null;
  score?: number | null;
  fullScore?: number | null;
}

interface QuestionCardProps {
  /** 展示序号，从 1 开始 */
  index: number;
  question: QuestionCardQuestion;
  /** 作答区插槽（考试中传入 AnswerInput） */
  answerNode?: ReactNode;
  /** 批阅/复盘信息：传入后展示学生答案与判分 */
  review?: QuestionCardReview;
}

/** 学生答案展示：选择题下标转字母，其余原样 */
function formatStudentAnswer(qType: string | null | undefined, answer: string | null | undefined): string {
  if (!answer) return '（未作答）';
  if (qType === QUESTION_TYPE.SINGLE_CHOICE || qType === QUESTION_TYPE.MULTIPLE_CHOICE) {
    return formatReferenceAnswer(qType, answer);
  }
  if (qType === QUESTION_TYPE.JUDGE) {
    return answer === '1' ? '正确' : '错误';
  }
  return answer;
}

/** 判分结果 Tag；主观题 isCorrect 为 null → 待批阅 */
function ReviewTag({ review }: { review: QuestionCardReview }) {
  const { isCorrect, score, fullScore } = review;
  const scoreText =
    score === null || score === undefined ? '-' : `${score}${fullScore ? ` / ${fullScore}` : ''} 分`;

  if (isCorrect === 1) {
    return (
      <Tag icon={<CheckCircleOutlined />} color="success">
        正确 · {scoreText}
      </Tag>
    );
  }
  if (isCorrect === 0) {
    return (
      <Tag icon={<CloseCircleOutlined />} color="error">
        错误 · {scoreText}
      </Tag>
    );
  }
  return (
    <Tag icon={<MinusCircleOutlined />} color="warning">
      待批阅 · {scoreText}
    </Tag>
  );
}

/**
 * 题目卡片：题干 + 选项 + （可选）参考答案/解析 + （可选）作答区/批阅结果
 */
export default function QuestionCard({ index, question, answerNode, review }: QuestionCardProps) {
  const { qType, content, options, answer, analysis, difficulty, score } = question;
  const isChoice =
    qType === QUESTION_TYPE.SINGLE_CHOICE || qType === QUESTION_TYPE.MULTIPLE_CHOICE;
  const optionList = isChoice ? options ?? [] : [];

  return (
    <Card size="small" style={{ marginBottom: 12 }}>
      {/* 头部：序号 + 题型/难度/分值 */}
      <Space wrap size={8} style={{ marginBottom: 8 }}>
        <Text strong>{index}.</Text>
        <Tag color={QUESTION_TYPE_COLORS[qType ?? ''] || 'default'}>
          {QUESTION_TYPE_LABELS[qType ?? ''] || qType || '未知题型'}
        </Tag>
        {difficulty ? <Tag>{DIFFICULTY_LABELS[difficulty] || `难度${difficulty}`}</Tag> : null}
        {score !== null && score !== undefined ? <Tag color="blue">{score} 分</Tag> : null}
      </Space>

      {/* 题干 */}
      <Paragraph style={{ marginBottom: optionList.length > 0 ? 12 : 0, whiteSpace: 'pre-wrap' }}>
        {content || '（题干为空）'}
      </Paragraph>

      {/* 选项 */}
      {optionList.length > 0 && (
        <div style={{ marginBottom: 8 }}>
          {optionList.map((opt, i) => (
            <div key={i} style={{ marginBottom: 4 }}>
              <Text>
                <Text strong style={{ marginRight: 8 }}>{indexToLetter(i)}</Text>
                {opt}
              </Text>
            </div>
          ))}
        </div>
      )}

      {/* 作答区 */}
      {answerNode && (
        <>
          <Divider style={{ margin: '12px 0' }} />
          {answerNode}
        </>
      )}

      {/* 批阅结果 */}
      {review && (
        <>
          <Divider style={{ margin: '12px 0' }} />
          <Space direction="vertical" size={6} style={{ width: '100%' }}>
            <div>
              <Text type="secondary">学生答案：</Text>
              <Text>{formatStudentAnswer(qType, review.studentAnswer)}</Text>
            </div>
            <div>
              <ReviewTag review={review} />
            </div>
          </Space>
        </>
      )}

      {/* 参考答案（有才显示：作答中不传 answer，避免泄题） */}
      {answer !== null && answer !== undefined && answer !== '' && (
        <>
          <Divider style={{ margin: '12px 0' }} />
          <Space direction="vertical" size={4} style={{ width: '100%' }}>
            <div>
              <Text type="secondary">参考答案：</Text>
              <Text style={{ color: '#52c41a' }}>{formatReferenceAnswer(qType, answer)}</Text>
            </div>
            {analysis && (
              <div>
                <Text type="secondary">解析：</Text>
                <Text>{analysis}</Text>
              </div>
            )}
          </Space>
        </>
      )}
    </Card>
  );
}
