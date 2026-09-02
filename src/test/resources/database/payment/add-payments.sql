-- === USERS ===
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

-- === CARS ===
INSERT INTO cars (id, model, brand, type, inventory, daily_fee, is_deleted)
VALUES
    (1, 'Model S', 'Tesla', 'SEDAN', 5, 199.99, 0),
    (2, 'CX-5', 'Mazda', 'SUV', 3, 149.50, 0);

-- === RENTALS ===
INSERT INTO rentals (id, user_id, car_id, rental_date, return_date, actual_return_date)
VALUES
    (1, 1, 1, '2024-01-01', '2024-01-05', NULL),
    (2, 2, 2, '2024-01-02', '2024-01-06', NULL),
    (3, 2, 1, '2024-02-01', '2024-02-05', NULL);

-- Payment for rental 1 (user 1)
INSERT INTO payments (id, status, type, rental_id, session_url, session_id, amount_to_pay)
VALUES
    (1, 'PENDING', 'PAYMENT', 1, 'https://session/abc', 'session-abc', 100.00);

-- Another payment for rental 1 (user 1)
INSERT INTO payments (id, status, type, rental_id, session_url, session_id, amount_to_pay)
VALUES
    (2, 'PAID', 'FINE', 1, 'https://session/def', 'session-def', 150.00);

-- Payment for rental 2 (user 2)
INSERT INTO payments (id, status, type, rental_id, session_url, session_id, amount_to_pay)
VALUES
    (3, 'PENDING', 'PAYMENT', 2, 'https://session/xyz', 'session-xyz', 200.00);
