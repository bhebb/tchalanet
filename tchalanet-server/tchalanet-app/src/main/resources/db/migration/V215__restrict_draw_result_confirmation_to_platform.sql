-- Tenant operators propose draw results; platform super admins confirm them.
DELETE FROM role_permission
 WHERE permission_code = 'draw_result.confirm'
   AND role_id IN (
     '00000000-0000-0000-0000-000000000302'::uuid,
     '00000000-0000-0000-0000-000000000305'::uuid
   );
