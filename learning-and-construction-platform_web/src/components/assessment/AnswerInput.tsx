import { Radio, Checkbox, Input, Space } from 'antd';
import { QUESTION_TYPE, indexToLetter } from '../../constants/assessment';

const { TextArea } = Input;

interface AnswerInputProps {
  qType: string | null | undefined;
  options?: string[] | null;
  /** 统一用字符串存储：单选 "0"、多选 "0,2"、判断 "1"/"0"、填空/简答原始文本 */
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

/** "0,2" -> [0, 2] */
function parseIndexes(value: string): number[] {
  return value
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s !== '')
    .map(Number)
    .filter((n) => Number.isInteger(n) && n >= 0);
}

/**
 * 按题型渲染作答控件
 *
 * 答案编码对齐后端 SubmissionServiceImpl.normalizedChoiceSet / normalizeJudge：
 * 选择题用选项下标（多选逗号分隔），判断题用 "1"/"0"。
 */
export default function AnswerInput({ qType, options, value, onChange, disabled }: AnswerInputProps) {
  const optionList = options ?? [];

  if (qType === QUESTION_TYPE.SINGLE_CHOICE) {
    return (
      <Radio.Group
        value={value === '' ? undefined : Number(value)}
        onChange={(e) => onChange(String(e.target.value))}
        disabled={disabled}
      >
        <Space direction="vertical">
          {optionList.map((opt, i) => (
            <Radio key={i} value={i}>
              <span style={{ marginRight: 8, fontWeight: 500 }}>{indexToLetter(i)}</span>
              {opt}
            </Radio>
          ))}
        </Space>
      </Radio.Group>
    );
  }

  if (qType === QUESTION_TYPE.MULTIPLE_CHOICE) {
    return (
      <Checkbox.Group
        value={parseIndexes(value)}
        onChange={(vals) =>
          // 排序后拼接，保证同一组答案字符串稳定（后端按集合比对，顺序无关）
          onChange(
            (vals as number[])
              .slice()
              .sort((a, b) => a - b)
              .join(',')
          )
        }
        disabled={disabled}
      >
        <Space direction="vertical">
          {optionList.map((opt, i) => (
            <Checkbox key={i} value={i}>
              <span style={{ marginRight: 8, fontWeight: 500 }}>{indexToLetter(i)}</span>
              {opt}
            </Checkbox>
          ))}
        </Space>
      </Checkbox.Group>
    );
  }

  if (qType === QUESTION_TYPE.JUDGE) {
    return (
      <Radio.Group value={value === '' ? undefined : value} onChange={(e) => onChange(e.target.value)} disabled={disabled}>
        <Radio value="1">正确</Radio>
        <Radio value="0">错误</Radio>
      </Radio.Group>
    );
  }

  if (qType === QUESTION_TYPE.FILL_BLANK) {
    return (
      <Input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="请填写答案"
        disabled={disabled}
        style={{ maxWidth: 420 }}
      />
    );
  }

  // 默认按简答题处理（short_answer）
  return (
    <TextArea
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder="请作答"
      rows={4}
      disabled={disabled}
      maxLength={2000}
      showCount
    />
  );
}
