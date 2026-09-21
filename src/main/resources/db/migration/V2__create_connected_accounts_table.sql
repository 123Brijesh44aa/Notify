create table connected_accounts(
    id bigint auto_increment primary key,
    user_id bigint not null,
    provider varchar(50) not null,
    external_username varchar(255),
    access_token_encrypted text not null ,
    scope varchar(255),
    connected_at timestamp default current_timestamp,
    constraint fk_connected_accounts_user foreign key (user_id) references users(id),
    constraint uq_user_provider unique (user_id, provider)
);