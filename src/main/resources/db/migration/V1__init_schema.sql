create table users(
    id bigint auto_increment primary key,
    username varchar(255) not null,
    email varchar(255) not null ,
    created_at timestamp not null default current_timestamp,
    constraint uk_users_username unique (username),
    constraint uk_users_email unique (email)
);

create table notifications(
    id bigint auto_increment primary key,
    user_id bigint not null,
    title varchar(255) not null,
    description text,
    source varchar(255) not null,
    priority varchar(255) not null default 'MEDIUM',
    is_read boolean not null default false,
    dedup_key varchar(255) not null ,
    created_at timestamp not null default current_timestamp,
    constraint fk_notifications_user foreign key (user_id) references users(id),
    constraint uk_notifications_dedup_key unique (dedup_key)
);

create index idx_notifications_user_created on notifications (user_id, created_at desc );
create index idx_notifications_user_unread on notifications (user_id, is_read);