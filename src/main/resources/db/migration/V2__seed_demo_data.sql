INSERT INTO workspaces (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'Demo Workspace');

INSERT INTO users (id, email, display_name)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'admin@example.com', 'Asha Admin'),
    ('00000000-0000-0000-0000-000000000102', 'lead@example.com', 'Ravi Lead'),
    ('00000000-0000-0000-0000-000000000103', 'member@example.com', 'Jane Smith'),
    ('00000000-0000-0000-0000-000000000104', 'viewer@example.com', 'Bob Chen');

INSERT INTO projects (id, workspace_id, key, name, description)
VALUES (
    '00000000-0000-0000-0000-000000000201',
    '00000000-0000-0000-0000-000000000001',
    'PROJ',
    'Project Management Platform',
    'Seed project for local API development'
);

INSERT INTO project_memberships (project_id, user_id, role)
VALUES
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000101', 'admin'),
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000102', 'project_lead'),
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000103', 'member'),
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000104', 'viewer');

INSERT INTO workflow_statuses (id, project_id, name, category, position, wip_limit)
VALUES
    ('00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000201', 'To Do', 'todo', 1, NULL),
    ('00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000201', 'In Progress', 'in_progress', 2, 5),
    ('00000000-0000-0000-0000-000000000303', '00000000-0000-0000-0000-000000000201', 'In Review', 'in_progress', 3, 3),
    ('00000000-0000-0000-0000-000000000304', '00000000-0000-0000-0000-000000000201', 'Done', 'done', 4, NULL);

INSERT INTO workflow_transitions (project_id, from_status_id, to_status_id, name, action_config)
VALUES
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000302', 'Start Progress', '{}'::jsonb),
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000303', 'Request Review', '{"assignReviewer": true}'::jsonb),
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000303', '00000000-0000-0000-0000-000000000304', 'Complete', '{}'::jsonb),
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000301', 'Move Back', '{}'::jsonb);

INSERT INTO sprints (id, project_id, name, goal, start_date, end_date, status)
VALUES (
    '00000000-0000-0000-0000-000000000401',
    '00000000-0000-0000-0000-000000000201',
    'Sprint 1',
    'Establish backend foundations',
    CURRENT_DATE,
    CURRENT_DATE + 14,
    'active'
);

INSERT INTO issues (
    id, project_id, issue_key, type, title, description, status_id, priority,
    assignee_id, reporter_id, sprint_id, story_points, labels
)
VALUES
    (
        '00000000-0000-0000-0000-000000000501',
        '00000000-0000-0000-0000-000000000201',
        'PROJ-1',
        'story',
        'Create project management backend foundation',
        'Set up Spring Boot, PostgreSQL, Redis, Flyway, health checks, and API docs.',
        '00000000-0000-0000-0000-000000000302',
        'high',
        '00000000-0000-0000-0000-000000000103',
        '00000000-0000-0000-0000-000000000104',
        '00000000-0000-0000-0000-000000000401',
        5,
        ARRAY['backend', 'foundation']
    ),
    (
        '00000000-0000-0000-0000-000000000502',
        '00000000-0000-0000-0000-000000000201',
        'PROJ-2',
        'task',
        'Document local Docker workflow',
        'Write setup notes for running the API, database, Redis, and Swagger locally.',
        '00000000-0000-0000-0000-000000000301',
        'medium',
        '00000000-0000-0000-0000-000000000102',
        '00000000-0000-0000-0000-000000000104',
        NULL,
        2,
        ARRAY['docs']
    );

INSERT INTO issue_watchers (issue_id, user_id)
VALUES
    ('00000000-0000-0000-0000-000000000501', '00000000-0000-0000-0000-000000000103'),
    ('00000000-0000-0000-0000-000000000501', '00000000-0000-0000-0000-000000000104');

INSERT INTO activity_log (project_id, issue_id, actor_id, event_type, payload)
VALUES (
    '00000000-0000-0000-0000-000000000201',
    '00000000-0000-0000-0000-000000000501',
    '00000000-0000-0000-0000-000000000104',
    'IssueCreated',
    '{"issueKey": "PROJ-1"}'::jsonb
);
