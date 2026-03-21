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


INSERT INTO secura_worklist 
(worklist_task_id, worklists_type, status, creat_ts, creat_usr_id, lst_updt_ts, lst_updt_usrId)
VALUES 
('WL001', 'BOOKING_APPROVAL', 'PENDING', NOW(), 'admin', NOW(), 'admin'),

('WL002', 'BOOKING_APPROVAL', 'PENDING', NOW(), 'user1', NOW(), 'user1'),

('WL003', 'BOOKING_APPROVAL', 'COMPLETED', NOW(), 'admin', NOW(), 'admin'),

('WL004', 'PAYMENT', 'PENDING', NOW(), 'user2', NOW(), 'user2'),

('WL005', 'CAM', 'PENDING', NOW(), 'security1', NOW(), 'security1');