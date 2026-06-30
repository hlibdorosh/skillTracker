-- Add goal_ms to habit_sessions
ALTER TABLE public.habit_sessions ADD COLUMN goal_ms bigint not null default 0;
