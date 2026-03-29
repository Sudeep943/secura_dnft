select * from secura_bkng

select * from secura_aprmnt


drop Table secura_dsc_fine; 
drop Table secura_aprmnt; 
drop Table secura_profl; 
drop Table secura_flat; 
drop Table secura_trnsac; 
drop Table secura_bkng; 
drop Table secura_halls; 
drop Table secura_skl_class; 
drop Table secura_cam;
drop Table secura_pymnt; 
drop Table secura_tenant; 
drop Table secura_access; 
drop Table secura_worklist;
drop Table secura_owner;
drop Table secura_profl_access;



INSERT INTO secura_aprmnt (
    aprmnt_id,
    aprmnt_name,
    aprmnt_address,
    aprmnt_bank_acccount_list,
    aprmnt_executive_role_list,
    aprmnt_logo,
    creat_ts,
    creat_usr_id,
    lst_updt_ts,
    lst_updt_usrId
) VALUES (
    'APRT001',
    'DN Fairytale',
    '123 Main Road, Bhubaneswar, Odisha',
    '',
    '',
    '',
    CURRENT_TIMESTAMP,
    'admin_user',
    CURRENT_TIMESTAMP,
    'admin_user'
);