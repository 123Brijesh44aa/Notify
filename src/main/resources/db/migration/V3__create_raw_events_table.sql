create table raw_events(
    id bigint auto_increment primary key,
    user_id bigint not null ,
    source varchar(50) not null,
    event_type varchar(100) not null ,
    external_id varchar(255),
    payload_json longtext not null ,
    received_at timestamp default current_timestamp,
    constraint fk_raw_events_user foreign key (user_id) references users(id)
);