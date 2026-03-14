insert into users(id, username, password)
values (1, 'bruce', 'wayne'),
       (2, 'peter', 'security_rules'),
       (3, 'tom', 'guessmeifyoucan'),
       (4, 'secure', 'travel');

insert into persons(id, firstName, lastName, email)
values (1, 'bruce', 'wayne', 'notBatman@gmail.com'),
       (2, 'Peter', 'Petigrew', 'oneFingernailFewerToClean@gmail.com'),
       (3, 'Tom', 'Riddle', 'theyGotMyNose@gmail.com'),
       (4, 'secure', 'travel', 'rbs@rbstravelsecurly.com');

insert into hashedUsers(id, username, passwordHash, salt)
values (1, 'bruce', 'qw8Uxa2fXimKruS9wYEm4qm3ZaIGw/hJNvOG3PemhoA=', 'MEI4PU5hcHhaRHZz'),
       (2, 'peter', 'qPWryBEWiWdHsC+67dmO+y5ugGrMVI2w4MSz0+CpDm4=', 'MnY1am14c2d1ZlBf'),
       (3, 'tom', 'FLmYMYmwSRxcy0n2uwysy39ax0TRWvKHswSCPMo+PiI=', 'OChoOitAKWE0TWlD');

insert into country (name)
values ('Serbia');
insert into country (name)
values ('Italy');
insert into country (name)
values ('Greece');

insert into city (countryId, name)
values (1, 'Belgrade');
insert into city (countryId, name)
values (1, 'Novi Sad');
insert into city (countryId, name)
values (2, 'Rome');
insert into city (countryId, name)
values (2, 'Milan');
insert into city (countryId, name)
values (3, 'Athens');
insert into city (countryId, name)
values (3, 'Thessaloniki');

insert into hotel (cityId, name, description, address)
values (1, 'Danube View Hotel', 'Modern hotel near the river promenade.', 'Cara Urosa 10, Belgrade');

insert into hotel (cityId, name, description, address)
values (3, 'Roma Centro Stay', 'Comfortable hotel in central Rome.', 'Via Nazionale 25, Rome');

insert into hotel (cityId, name, description, address)
values (5, 'Acropolis Boutique Hotel', 'Boutique hotel close to major attractions.', 'Dionysiou Areopagitou 7, Athens');

insert into roomType (hotelId, name, capacity, pricePerNight, totalRooms)
values (1, 'Standard Double', 2, 79.99, 20);

insert into roomType (hotelId, name, capacity, pricePerNight, totalRooms)
values (1, 'Family Suite', 4, 129.50, 5);

insert into roomType (hotelId, name, capacity, pricePerNight, totalRooms)
values (2, 'Economy Single', 1, 69.00, 15);

insert into roomType (hotelId, name, capacity, pricePerNight, totalRooms)
values (2, 'Deluxe Double', 2, 149.99, 10);

insert into roomType (hotelId, name, capacity, pricePerNight, totalRooms)
values (3, 'Standard Double', 2, 119.00, 12);

insert into roomType (hotelId, name, capacity, pricePerNight, totalRooms)
values (3, 'Junior Suite', 3, 169.00, 6);

insert into reservation
(userId, hotelId, roomTypeId, startDate, endDate, roomsCount, guestsCount, totalPrice)
values (1, 1, 1, DATE '2026-03-10', DATE '2026-03-13', 1, 2, 239.97);

insert into reservation
(userId, hotelId, roomTypeId, startDate, endDate, roomsCount, guestsCount, totalPrice)
values (2, 2, 4, DATE '2026-04-02', DATE '2026-04-06', 1, 2, 599.96);

insert into reservation
(userId, hotelId, roomTypeId, startDate, endDate, roomsCount, guestsCount, totalPrice)
values (1, 3, 6, DATE '2026-05-15', DATE '2026-05-18', 1, 3, 507.00);

insert into ratings(hotelId, userId, rating)
values (1, 3, 5),
       (3, 2, 1),
       (3, 1, 3),
       (1, 1, 5),
       (1, 2, 4);

insert into permissions(id, name)
values (1, 'VIEW_HOTEL_LIST'),
       (2, 'VIEW_HOTEL'),
       (3, 'CREATE_COUNTRY'),
       (4, 'CREATE_CITY'),
       (5, 'CREATE_HOTEL'),
       (6, 'VIEW_PERSONS_LIST'),
       (7, 'VIEW_PERSON'),
       (8, 'UPDATE_PERSON'),
       (9, 'VIEW_MY_PROFILE'),
       (10, 'RATE_HOTEL'),
       (11, 'CREATE_RESERVATION'),
       (12, 'VIEW_RESERVATION');

-- ADMIN -> sve
insert into role_to_permissions(roleId, permissionId)
values (1, 1),
       (1, 2),
       (1, 3),
       (1, 4),
       (1, 5),
       (1, 6),
       (1, 7),
       (1, 8),
       (1, 9),
       (1, 10),
       (1, 11),
       (1, 12);

-- MANAGER
insert into role_to_permissions(roleId, permissionId)
values (2, 1), -- VIEW_HOTEL_LIST
       (2, 2), -- VIEW_HOTEL
       (2, 5), -- CREATE_HOTEL
       (2, 6), -- VIEW_PERSONS_LIST
       (2, 8), -- UPDATE_PERSON
       (2, 9), -- VIEW_MY_PROFILE
       (2, 11);-- CREATE_RESERVATION

-- CUSTOMER
insert into role_to_permissions(roleId, permissionId)
values (3, 1),  -- VIEW_HOTEL_LIST
       (3, 2),  -- VIEW_HOTEL
       (3, 8),  -- UPDATE_PERSON
       (3, 9),  -- VIEW_MY_PROFILE
       (3, 10), -- RATE_HOTEL
       (3, 11), -- CREATE_RESERVATION
       (3, 12); -- VIEW_RESERVATION

insert into roles(id, name)
values (1, 'ADMIN'),
       (2, 'MANAGER'),
       (3, 'CUSTOMER');

insert into user_to_roles(userId, roleId)
values (4, 1),
       (3, 2),
       (1, 3),
       (2, 3);