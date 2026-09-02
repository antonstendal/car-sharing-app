
insert into roles (id, role)
values
    (1, 'CUSTOMER'),
    (2, 'MANAGER');

insert into users (id, first_name, last_name, email, password)
values
    (1, 'Bob', 'Thornton', 'manager@gmail.com',
     '$2a$10$eomvkdaTLl.ZyuZgR.nq5eKqUAmahk9q82L/fYEKSE6J6FD.ldvIe'),
    (2, 'Alice', 'Harrison', 'customer@gmail.com',
     '$2a$10$eomvkdaTLl.ZyuZgR.nq5eKqUAmahk9q82L/fYEKSE6J6FD.ldvIe'),
    (3, 'Ghost', 'User', 'deleted@gmail.com',
     '$2a$10$eomvkdaTLl.ZyuZgR.nq5eKqUAmahk9q82L/fYEKSE6J6FD.ldvIe');

insert into users_roles (user_id, role_id)
values
    (1, 2),
    (2, 1);
