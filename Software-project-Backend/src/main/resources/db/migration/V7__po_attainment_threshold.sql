-- Persists a per-PO CQI attainment target, mirroring the per-LO threshold that
-- `los.attainment_threshold` already stored. Previously the PO-level CQI trigger
-- (CQIService.checkAndTriggerCQI_PO) and the accreditation audit dashboard only had a hardcoded
-- 70% default with no way to change it per PO without a code change.

ALTER TABLE `program_outcomes` ADD COLUMN `attainment_threshold` double DEFAULT 70.0;
