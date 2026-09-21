create table watched_repositories(
    id bigint auto_increment primary key,
    user_id bigint not null ,
    repo_full_name varchar(255) not null ,
    github_webhook_id bigint not null ,
    created_at timestamp default current_timestamp,
    constraint fk_watched_repos_user foreign key (user_id) references users(id),
    constraint uq_user_repo unique (user_id,repo_full_name)
);