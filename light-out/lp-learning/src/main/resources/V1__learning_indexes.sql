-- Add unique indexes to support upsert operations
ALTER TABLE video_progress ADD UNIQUE KEY ux_video_progress_user_resource (user_id, resource_id);
ALTER TABLE doc_progress ADD UNIQUE KEY ux_doc_progress_user_resource (user_id, resource_id);
ALTER TABLE study_stats ADD UNIQUE KEY ux_study_stats_user_course_date (user_id, course_id, stat_date);

-- Optional: index for lookup
CREATE INDEX idx_study_stats_course_date ON study_stats (course_id, stat_date);
