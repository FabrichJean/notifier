ALTER TABLE notifications ADD COLUMN target_device_id TEXT REFERENCES devices(id);
