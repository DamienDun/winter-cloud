ALTER TABLE `ds_job_log`
  ADD COLUMN `xxl_log_id` bigint(20) DEFAULT NULL COMMENT 'XXL-JOB log id for kill' AFTER `job_message`;
