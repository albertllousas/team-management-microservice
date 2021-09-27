CREATE TABLE public.team
(
    id      UUID        NOT NULL,
    name    TEXT        NOT NULL,
    created TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    members TEXT[] NOT NULL,
    CONSTRAINT unique_team_name UNIQUE (name),
    CONSTRAINT pk_team PRIMARY KEY (id)
);

CREATE TABLE public.outbox
(
    id            UUID PRIMARY KEY,
    aggregate_id  UUID                     NOT NULL,
    event_payload BYTEA                    NOT NULL,
    stream        TEXT                     NOT NULL,
    created       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);


CREATE TABLE public.people_replication
(
    id         UUID        NOT NULL,
    status     TEXT        NOT NULL,
    first_name TEXT        NOT NULL,
    last_name  TEXT        NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_people_replication PRIMARY KEY (id)
);
