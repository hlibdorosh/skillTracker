-- Create Habits table
create table public.habits (
    id uuid not null default gen_random_uuid(),
    name text not null,
    created_at timestamp with time zone not null default now(),
    total_time_spent bigint not null default 0,
    constraint habits_pkey primary key (id)
);

-- Create Habit Sessions table
create table public.habit_sessions (
    id uuid not null default gen_random_uuid(),
    habit_id uuid not null,
    date_started date not null default current_date,
    duration_ms bigint not null default 0,
    constraint habit_sessions_pkey primary key (id),
    constraint habit_sessions_habit_id_fkey foreign key (habit_id) references public.habits (id) on delete cascade
);

-- Enable RLS (Row Level Security) and allow all access for local development simplicity
alter table public.habits enable row level security;
alter table public.habit_sessions enable row level security;

create policy "Enable all access for all users on habits"
on public.habits for all
using (true)
with check (true);

create policy "Enable all access for all users on habit_sessions"
on public.habit_sessions for all
using (true)
with check (true);
