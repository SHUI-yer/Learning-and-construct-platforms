import { Select } from 'antd';
import { useCourses } from '../../hooks/useCourses';
import { useAuth } from '../../hooks/useAuth';

interface CourseSelectProps {
  value?: number;
  onChange: (courseId: number | undefined) => void;
  /** 教师端：只展示本人任教的课程（后端 /course/page 不按身份过滤，这里前端筛 teacherId） */
  allowOwnOnly?: boolean;
  /** 学生端：仅展示已发布(status=2)的课程 */
  studentMode?: boolean;
  width?: number;
  placeholder?: string;
}

/**
 * 课程选择器
 *
 * 教师模式固定走 useCourses(1, 50)（queryKey ['courses',1,50,{}]），
 * TaskList 的表单弹窗复用同一缓存反查课程名，改参数会多一次请求。
 */
export default function CourseSelect({
  value,
  onChange,
  allowOwnOnly = false,
  studentMode = false,
  width = 240,
  placeholder = '请选择课程',
}: CourseSelectProps) {
  const { userId } = useAuth();
  // 学生端只看已发布课程；教师端取全量后再按任教筛
  const { data, isLoading } = useCourses(1, 50, studentMode ? { status: 2 } : {});

  const all = data?.records ?? [];
  const courses = allowOwnOnly && userId ? all.filter((c) => c.teacherId === userId) : all;

  return (
    <Select
      value={value}
      onChange={onChange}
      loading={isLoading}
      allowClear
      showSearch
      placeholder={placeholder}
      style={{ width }}
      optionFilterProp="label"
      options={courses.map((c) => ({
        value: c.courseId,
        label: c.courseName,
      }))}
    />
  );
}
