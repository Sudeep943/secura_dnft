CREATE TABLE secura_aprmnt (
    aprmnt_id VARCHAR PRIMARY KEY,
    aprmnt_name VARCHAR,
    aprmnt_address VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR
);


CREATE TABLE secura_dsc_fine (
    disc_fn_id VARCHAR PRIMARY KEY,
    disc_fn_type VARCHAR,
    disc_fn_mode VARCHAR,
    disc_fn_strt_dt TIMESTAMP,
    disc_fn_end_dt TIMESTAMP,
    fn_type VARCHAR,
    fn_cumlation_cycle VARCHAR,
    aprmnt_id VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR,

    FOREIGN KEY (aprmnt_id) REFERENCES secura_aprmnt(aprmnt_id)
);

CREATE TABLE secura_profl (
    prfl_id VARCHAR PRIMARY KEY,
    prfl_name VARCHAR,
    prfl_flat_no VARCHAR,
    prfl_dob TIMESTAMP,
    prfl_phone_no VARCHAR,
    prfl_email_adrss VARCHAR,
    prfl_othr_adrss TEXT,
    prfl_type VARCHAR,
    prfl_age VARCHAR,
    prfl_stus VARCHAR,
    prfl_access VARCHAR,
    profile_pic TEXT,
    prfl_position VARCHAR,
    aprmnt_id VARCHAR,
    gender VARCHAR,
    password VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR
);


CREATE TABLE secura_flat (
    flat_no VARCHAR PRIMARY KEY,
    flat_owner_id VARCHAR,
    flat_tower VARCHAR,
    flat_possn_date TIMESTAMP,
    flat_owner_type VARCHAR,
    flat_area VARCHAR,
    flat_pndng_paymnt_lst VARCHAR,
    aprmnt_id VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR,

    FOREIGN KEY (aprmnt_id) REFERENCES secura_aprmnt(aprmnt_id)
);


CREATE TABLE secura_trnsac (
    aprmnt_id VARCHAR,
    trnsc_id VARCHAR PRIMARY KEY,
    trns_date TIMESTAMP,
    trns_by VARCHAR,
    trns_tender VARCHAR,
    trns_type VARCHAR,
    trns_shrt_desc VARCHAR,
    trns_files VARCHAR,
    trns_bnk_accnt VARCHAR,
    trns_amt VARCHAR,
    pymnt_id VARCHAR,
    third_party_trns_ref VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR
);


CREATE TABLE secura_bkng (
    bkng_id VARCHAR PRIMARY KEY,
    bkng_date TIMESTAMP,
    bkng_by VARCHAR,
    bkng_hall_id VARCHAR,
    bkng_evnt_dt TIMESTAMP,
    bkng_flt_no VARCHAR,
    bkng_phn_no VARCHAR,
    bkng_pros VARCHAR,
    bkng_expt_gest VARCHAR,
    bkng_type VARCHAR,
    bkng_sts VARCHAR,
    bkng_aprvd_by VARCHAR,
    bkng_cncld_by VARCHAR,
    bkng_trnsc_id VARCHAR,
    bkng_cncld_reason VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR,

    FOREIGN KEY (bkng_trnsc_id) REFERENCES secura_trnsac(trnsc_id)
);

ALTER TABLE secura_bkng
ADD COLUMN aprmnt_id VARCHAR(50) NOT NULL

ALTER TABLE secura_bkng
ADD COLUMN tender VARCHAR(50)

ALTER TABLE secura_bkng
ADD COLUMN hall_name VARCHAR(50)

ALTER TABLE secura_bkng
ADD COLUMN amound_paid VARCHAR(50)

CREATE TABLE secura_halls (
    hall_id VARCHAR PRIMARY KEY,
    hall_name VARCHAR,
    hall_location VARCHAR,
    hall_area VARCHAR,
    hall_amount VARCHAR,
    hall_sop VARCHAR,
    hall_status VARCHAR,
    hall_images VARCHAR,
    aprmnt_id VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR,

    FOREIGN KEY (aprmnt_id) REFERENCES secura_aprmnt(aprmnt_id)
);


CREATE TABLE secura_skl_class (
    skl_clss_id VARCHAR PRIMARY KEY,
    skl_clss_name VARCHAR,
    skl_clss_by VARCHAR,
    skl_clss_fees VARCHAR,
    skl_clss_days_per_week VARCHAR,
    skl_clss_total_student VARCHAR,
    skl_clss_strt_dt TIMESTAMP,
    skl_clss_lst_dt TIMESTAMP,
    aprmnt_id VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR,

    FOREIGN KEY (aprmnt_id) REFERENCES secura_aprmnt(aprmnt_id)
);


CREATE TABLE secura_cam (
    cam_id VARCHAR PRIMARY KEY,
    cam_strt_dt TIMESTAMP,
    cam_end_dt TIMESTAMP,
    cam_collection_cycle VARCHAR,
    cam_clctn_dt TIMESTAMP,
    cam_pricing_mthd VARCHAR,
    cam_pricing_disc_fn_id VARCHAR,
    cam_status VARCHAR,
    aprmnt_id VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR,

    FOREIGN KEY (cam_pricing_disc_fn_id) REFERENCES secura_dsc_fine(disc_fn_id),
    FOREIGN KEY (aprmnt_id) REFERENCES secura_aprmnt(aprmnt_id)
);


CREATE TABLE secura_pymnt (
    pymnt_id VARCHAR PRIMARY KEY,
    pymnt_name VARCHAR,
    pymnt_heading VARCHAR,
    pymnt_long_desc VARCHAR,
    pymnt_strt_dt TIMESTAMP,
    pymnt_end_dt TIMESTAMP,
    pymnt_type VARCHAR,
    disc_fn_id VARCHAR,
    aprmnt_id VARCHAR,
	flt_lst VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR,
   );

CREATE TABLE secura_tenant (
    aprmt_id       VARCHAR(100) NOT NULL,
    prfl_id        VARCHAR(100) NOT NULL,
    flat_no        VARCHAR(50)  NOT NULL,
    status         VARCHAR(50)  NOT NULL,
    start_date     TIMESTAMP,
    end_date       TIMESTAMP,
    document TEXT,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR,

    PRIMARY KEY (aprmt_id,prfl_id, flat_no, status)
);




CREATE TABLE secura_access (
 aprmt_id       VARCHAR(100) NOT NULL,
    role VARCHAR(50) PRIMARY KEY,
    acces_json JSONB,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR
);

INSERT INTO secura_access (role, acces_json)
VALUES (
  'ADMIN',
  '{"canCreate": true, "canUpdate": true, "canDelete": true}'
);


CREATE TABLE secura_worklist (
 aprmt_id       VARCHAR(100) NOT NULL,
    worklist_task_id   VARCHAR(100) PRIMARY KEY,
    worklists_type     VARCHAR(50),
    status             VARCHAR(50),
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usrId VARCHAR
);