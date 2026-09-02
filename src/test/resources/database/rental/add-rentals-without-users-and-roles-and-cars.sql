
insert into roles (id, role)
values
    (1, 'CUSTOMER'),
    (2, 'MANAGER');

insert into users (id, first_name, last_name, email, password)
values
    (1, 'Bob', 'Thornton', 'manager@gmail.com', '12345678'),
    (2, 'Alice', 'Harrison', 'customer@gmail.com', '12345678'),
    (3, 'Ghost', 'User', 'deleted@gmail.com', '12345678');

insert into users_roles (user_id, role_id)
values
    (1, 2),
    (2, 1);

insert into cars (id, model, brand, type, inventory, daily_fee, is_deleted)
values
    (1, 'Model S', 'Tesla', 'SEDAN', 5, 199.99, 0),
    (2, 'CX-5', 'Mazda', 'SUV', 3, 149.50, 0),
    (3, 'Golf', 'Volkswagen', 'HATCHBACK', 7, 99.00, 0);

insert into rentals (id, user_id, car_id, rental_date, return_date, actual_return_date)
values
    (1, 1, 1, '2024-01-01', '2024-01-05', null);

insert into rentals (id, user_id, car_id, rental_date, return_date, actual_return_date)
values
    (2, 1, 1, '2024-01-10', '2024-01-15', '2024-01-14');

insert into rentals (id, user_id, car_id, rental_date, return_date, actual_return_date)
values
    (3, 2, 2, '2024-01-02', '2024-01-06', null);

insert into rentals (id, user_id, car_id, rental_date, return_date, actual_return_date)
values
    (4, 2, 2, '2024-01-07', '2024-01-10', '2024-01-09');
