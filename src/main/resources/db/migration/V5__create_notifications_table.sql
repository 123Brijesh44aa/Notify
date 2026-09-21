create table notifications(
    id bigint auto_increment primary key,
    user_id bigint not null ,
    raw_event_id bigint not null ,
    title varchar(255) not null ,
    body text,
    priority varchar(20) not null ,
    source varchar(50) not null ,
    read_at timestamp null,
    created_at timestamp default current_timestamp,
    constraint fk_notifications_user foreign key (user_id) references users(id),
    constraint fk_notifications_raw_event foreign key (raw_event_id) references raw_events(id)
);