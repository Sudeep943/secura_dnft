CREATE TABLE secura_aprmnt (
    aprmnt_id VARCHAR PRIMARY KEY,
    aprmnt_name VARCHAR,
    aprmnt_address TEXT,
    aprmnt_bank_acccount_list TEXT,
    aprmnt_executive_role_list TEXT,
    aprmnt_logo TEXT,
    aprmnt_letter_head TEXT,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR
);



CREATE TABLE secura_profl (
    prfl_id VARCHAR PRIMARY KEY,
    prfl_name VARCHAR,
    prfl_acount_details TEXT,
    prfl_dob TIMESTAMP,
    prfl_phone_no VARCHAR,
    prfl_email_adrss VARCHAR,
    prfl_primary_postal_adrss TEXT,
    prfl_othr_adrss TEXT,
    prfl_access VARCHAR,
    profile_kind VARCHAR,
    profile_pic TEXT,
    gender VARCHAR,
    password TEXT,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR
);


CREATE TABLE secura_profl_access(
 prfl_id VARCHAR,
 aprmnt_id VARCHAR,
 profile_blocked boolean,
 access TEXT,
 creat_ts TIMESTAMP,
 creat_usr_id VARCHAR,
 lst_updt_ts TIMESTAMP,
 lst_updt_usr_id VARCHAR,
  PRIMARY KEY (prfl_id,aprmnt_id)
 );

 
CREATE TABLE secura_trnsac (
    aprmnt_id VARCHAR,
    trnsc_id VARCHAR PRIMARY KEY,
    trns_date TIMESTAMP,
    trns_by VARCHAR,
    trns_tender VARCHAR,
    trns_type VARCHAR,
    trns_shrt_desc VARCHAR,
    trns_files TEXT,
    trns_bnk_accnt VARCHAR,
    trns_amt VARCHAR,
    trns_currency VARCHAR,
    pymnt_id VARCHAR,
    trns_status VARCHAR,
    no_of_person VARCHAR,
    third_party_trns_ref VARCHAR,
    third_party_name VARCHAR,
    due_details TEXT,
    worklist_id VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR
);

CREATE TABLE secura_bkng (
    aprmnt_id VARCHAR(50) NOT NULL,
    bkng_id VARCHAR PRIMARY KEY,
    bkng_date TIMESTAMP,
    bkng_by VARCHAR,
    bkng_hall_id VARCHAR,
    hall_name VARCHAR,
    bkng_evnt_dt TIMESTAMP,
    bkng_flt_no VARCHAR,
    bkng_phn_no VARCHAR,
    bkng_pros VARCHAR,
    bkng_expt_gest VARCHAR,
    bkng_type VARCHAR,
    bkng_sts VARCHAR,
    tender VARCHAR,
    amound_paid VARCHAR,
    trnsc_id VARCHAR,
    bkng_rcpt TEXT,
    bkng_documet TEXT,
    bkng_aprvd_by VARCHAR,
    bkng_cncld_by VARCHAR,
    bkng_trnsc_id VARCHAR,
    bkng_cncld_reason VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR
);



CREATE TABLE secura_halls (
    aprmnt_id VARCHAR,
    hall_id VARCHAR PRIMARY KEY,
    hall_name VARCHAR,
    hall_details TEXT,
    hall_location VARCHAR,
    hall_area VARCHAR,
    hall_amount TEXT,
    hall_sop TEXT,
    hall_status VARCHAR,
    hall_images TEXT,
    blocked_flat_list TEXT,
    hall_config TEXT,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR
);

ALTER TABLE secura_halls DROP COLUMN hall_amount;

ALTER TABLE secura_halls ADD COLUMN hall_amount TEXT;

CREATE TABLE secura_tenant (
    aprmt_id       VARCHAR(100) NOT NULL,
    prfl_id        TEXT NOT NULL,
    flat_no         VARCHAR(100)  NOT NULL,
    status         VARCHAR(50)  NOT NULL,
    start_date     TIMESTAMP,
    end_date       TIMESTAMP,
    document TEXT,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR,

    PRIMARY KEY (aprmt_id,prfl_id, flat_no, status)
);

ALTER TABLE secura_tenant 
ADD COLUMN verified BOOLEAN;

CREATE TABLE secura_owner (
    aprmt_id       VARCHAR(100) NOT NULL,
    prfl_id        TEXT NOT NULL,
    flat_no        VARCHAR(100)  NOT NULL,
    status         VARCHAR(50)  NOT NULL,
    start_date     TIMESTAMP,
    end_date       TIMESTAMP,
    document TEXT,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR,

    PRIMARY KEY (aprmt_id,prfl_id, flat_no, status)
);
ALTER TABLE secura_owner DROP CONSTRAINT  secura_owner_pkey;

ALTER TABLE secura_owner 
ADD COLUMN ownerId VARCHAR PRIMARY KEY; 

ALTER TABLE secura_tenant DROP CONSTRAINT  secura_tenant_pkey;

ALTER TABLE secura_tenant 
ADD COLUMN tenantId VARCHAR PRIMARY KEY; 

CREATE TABLE secura_flat (
    aprmnt_id VARCHAR,
    flat_no VARCHAR PRIMARY KEY,
    flat_owner_list TEXT,
    flat_tower VARCHAR,
    flat_possn_date TIMESTAMP,
    flat_owner_type VARCHAR,
    flat_area VARCHAR,
    flat_pndng_paymnt_lst TEXT,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR
);


CREATE TABLE secura_worklist (
    aprmt_id       VARCHAR(100) NOT NULL,
    worklist_task_id   VARCHAR(100) PRIMARY KEY,
    worklists_type     VARCHAR(50),
    status             VARCHAR(50),
    short_remark       TEXT,
    worklists_assign_flow TEXT,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR
);


INSERT INTO secura_aprmnt (
    aprmnt_id,
    aprmnt_name,
    aprmnt_address,
    aprmnt_bank_acccount_list,
    aprmnt_executive_role_list,
    aprmnt_logo,
    aprmnt_letter_head,
    creat_ts,
    creat_usr_id,
    lst_updt_ts,
    lst_updt_usr_id
) VALUES (
    'APRT001',
    'DN Fairytale',
    '123 Main Road, Bhubaneswar, Odisha',
    '',
    '',
    '',
    '',
    CURRENT_TIMESTAMP,
    'admin_user',
    CURRENT_TIMESTAMP,
    'admin_user'
);

ALTER TABLE secura_bkng 
ADD COLUMN sec_deposite VARCHAR;

ALTER TABLE secura_bkng DROP COLUMN bkng_date;

ALTER TABLE secura_bkng ADD COLUMN bkng_date TIMESTAMP;


CREATE TABLE secura_notice (
    aprmt_id VARCHAR(255),
    notice_id VARCHAR(255) PRIMARY KEY,
    publishing_date TIMESTAMP,
    letter_number VARCHAR(255),
    header TEXT,
    short_details TEXT,
    status VARCHAR(255),
    notice_document_id VARCHAR(255),
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR(255),
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR(255)
);

CREATE TABLE secura_doc (
    document_id      VARCHAR(255) PRIMARY KEY,
    document_type    VARCHAR(255),
    document_data    TEXT,
    creat_ts         TIMESTAMP,
    creat_usr_id     VARCHAR(255),
    lst_updt_ts      TIMESTAMP,
    lst_updt_usrId   VARCHAR(255)
);

ALTER TABLE secura_doc ADD COLUMN aprmt_id VARCHAR;


CREATE TABLE secura_events (
    aprmt_id VARCHAR,
    event_id VARCHAR PRIMARY KEY,
    event_type VARCHAR,
    event_date_time TIMESTAMP,
    location VARCHAR,
    duration VARCHAR,
    occurance VARCHAR,
    till_date TIMESTAMP,
    header TEXT,
    short_details TEXT,
    paid BOOLEAN,
    payment_type VARCHAR,
    collection_start_date TIMESTAMP,
    collection_end_date TIMESTAMP,
    registration_form_link TEXT,
    invitees TEXT,
    required_coupon_creation_for_paid_member BOOLEAN,
    bank_account_id VARCHAR,
    payment_amount VARCHAR,
    status VARCHAR,
    creat_ts TIMESTAMP,
    creat_usr_id VARCHAR,
    lst_updt_ts TIMESTAMP,
    lst_updt_usr_id VARCHAR
);
