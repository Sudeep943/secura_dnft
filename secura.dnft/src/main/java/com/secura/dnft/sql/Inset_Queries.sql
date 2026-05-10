select * from secura_aprmnt
select * from secura_halls


INSERT INTO secura_halls VALUES
('DNHALL01','Activity Hall 1','Towewr 1','2000 sqft','1000','Standard SOP','APT001','hall1.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL02','Activity Hall 2','Towewr 2','2000 sqft','500','Standard SOP','A','hall2.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL03','Activity Hall 3','Towewr 3','2000 sqft','500','Standard SOP','A','hall3.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL04','Activity Hall 4','Towewr 4','2000 sqft','500','Standard SOP','A','hall4.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL05','Activity Hall 5','Towewr 5','2000 sqft','500','Standard SOP','A','hall5.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL06','Activity Hall 6','Towewr 6','2000 sqft','500','Standard SOP','A','hall6.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL07','Activity Hall 7','Towewr 7','2000 sqft','500','Standard SOP','A','hall7.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL08','Activity Hall 8','Towewr 8','2000 sqft','500','Standard SOP','A','hall8.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL09','Activity Hall 9','Towewr 9','2000 sqft','500','Standard SOP','A','hall9.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL10','Activity Hall 10','Towewr 10','2000 sqft','500','Standard SOP','A','hall10.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL11','Activity Hall 11','Towewr 11', '2000 sqft','500','Standard SOP','A','hall11.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL12','Activity Hall 12','Towewr 12','2000 sqft','500','Standard SOP','D','hall12.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');

INSERT INTO secura_halls VALUES
('DNHALL13','Banquet Hall','Mart','6000 sqft','15000','Standard SOP','a','hall12.jpg','APT001',CURRENT_TIMESTAMP,'admin',CURRENT_TIMESTAMP,'admin');


INSERT INTO secura_worklist 
(worklist_task_id, worklists_type, status, creat_ts, creat_usr_id, lst_updt_ts, lst_updt_usrId)
VALUES 
('WL001', 'BOOKING_APPROVAL', 'PENDING', NOW(), 'admin', NOW(), 'admin'),

('WL002', 'BOOKING_APPROVAL', 'PENDING', NOW(), 'user1', NOW(), 'user1'),

('WL003', 'BOOKING_APPROVAL', 'COMPLETED', NOW(), 'admin', NOW(), 'admin'),

('WL004', 'PAYMENT', 'PENDING', NOW(), 'user2', NOW(), 'user2'),

('WL005', 'CAM', 'PENDING', NOW(), 'security1', NOW(), 'security1');


INSERT INTO public.society_collection_types
(collection_type, purpose_of_collection, sac_code, taxable, type_constant)
VALUES
('Maintenance', 'Routine upkeep, security, housekeeping, and common electricity expenses', '9995', true, 'MAINTENANCE'),
('Sinking Fund', 'Long-term major repairs and structural replacement', '9995', true, 'SINKING FUND'),
('Repair & Maintenance Fund', 'Building repairs, plumbing, painting, and lift servicing', '9995', true, 'REPAIR_MAINTENANCE_FUND'),
('Corpus Fund / Reserve Fund', 'Capital reserve for future projects and contingencies', '9995', true, 'CORPUS_FUND'),
('Car Parking Charges', 'Use and maintenance of parking facilities', '9995', true, 'CAR_PARKING_CHARGES'),
('Clubhouse Charges', 'Gym, swimming pool, and community hall upkeep', '9995', true, 'CLUBHOUSE_CHARGES'),
('Non-Occupancy Charges', 'Charges when a flat is rented out or not occupied by the owner', '9995', true, 'NON_OCCUPANCY_CHARGES'),
('Festival Fund', 'Celebrations such as Diwali, Durga Puja, and other community events', '9995', true, 'FESTIVAL_FUND'),
('Water Charges (Society-Collected)', 'Common water supply and pumping expenses', '9995', true, 'WATER CHARGES (SOCIETY_COLLECTED)'),
('Generator / Power Backup Charges', 'Diesel costs and generator maintenance', '9995', true, 'POWER_BACKUP_CHARGES'),
('Security Charges', 'Security guards and surveillance systems', '9995', true, 'SECURITY_CHARGES'),
('Late Payment Interest', 'Penalty for delayed payment of society dues', '9995', true, 'LATE_PAYMENT_INTEREST');