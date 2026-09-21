create table users(
    id bigint auto_increment primary key,
    email varchar(255) not null unique ,
    password_hash varchar(255) not null,
    full_name varchar(255),
    created_at timestamp default current_timestamp
);