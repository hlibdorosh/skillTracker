-- Add daily_goal_ms to habits
alter table public.habits add column daily_goal_ms bigint not null default 0;
