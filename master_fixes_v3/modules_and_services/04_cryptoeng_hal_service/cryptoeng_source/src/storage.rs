// SPDX-License-Identifier: Apache-2.0

use std::collections::HashMap;
use std::fs::{self, OpenOptions};
use std::io::Write;
use std::os::unix::fs::{OpenOptionsExt, PermissionsExt};
use std::path::{Path, PathBuf};

use aes_gcm::aead::{Aead, KeyInit};
use aes_gcm::{Aes256Gcm, Nonce};
use rand::RngExt;
use serde::{Deserialize, Serialize};

use crate::error::{CryptoengError, Result};
use crate::types::{InfoType, PasswordQuality, SecureBufferEntry, SystemType};

pub const STORAGE_DIR: &str = "/data/vendor_de/0/cryptoeng";

const KEY_FILE: &str = "cryptoeng.key";
const DATA_FILE: &str = "cryptoeng.dat";
const STORAGE_HEADER: &[u8] = b"CENG\x00\x00\x00\x01";
const NONCE_SIZE: usize = 12;
const TAG_SIZE: usize = 16;

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct PrivacyData {
    pub password_type: PasswordQuality,
    pub password_hash: Vec<u8>,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub recovery_records: Vec<SecureBufferEntry>,
    pub email: String,
    pub handler: Vec<u8>,
    pub retry_count: i32,
    pub secure_question_retry_count: i32,
    pub enrolled: bool,
    #[serde(default)]
    pub authorization: Option<PendingVerification>,
    #[serde(default)]
    password_retry: RetryRecord,
    #[serde(default)]
    question_retry: RetryRecord,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PendingVerification {
    pub challenge: Vec<u8>,
    pub issued_at: BootTimestamp,
}

impl PendingVerification {
    pub fn is_expired(&self, now: &BootTimestamp, lifetime_ms: u64) -> bool {
        now.elapsed_since(&self.issued_at)
            .is_none_or(|elapsed| elapsed >= lifetime_ms)
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BootTimestamp {
    pub boot_id: String,
    pub milliseconds: u64,
}

impl BootTimestamp {
    fn elapsed_since(&self, earlier: &Self) -> Option<u64> {
        (self.boot_id == earlier.boot_id)
            .then(|| self.milliseconds.checked_sub(earlier.milliseconds))
            .flatten()
    }
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
struct RetryRecord {
    failures: u32,
    last_failed: Option<BootTimestamp>,
}

impl RetryRecord {
    fn status(&self, now: &BootTimestamp, latest: bool) -> (i32, i32) {
        let delay: u64 = if latest {
            match self.failures {
                5 => 60_000,
                10 => 600_000,
                15 => 1_800_000,
                20.. => 3_600_000,
                _ => 0,
            }
        } else if self.failures > 0 && self.failures.is_multiple_of(5) {
            30_000
        } else {
            0
        };

        let elapsed = self
            .last_failed
            .as_ref()
            .and_then(|previous| now.elapsed_since(previous));
        let remaining = delay.saturating_sub(elapsed.unwrap_or(now.milliseconds));
        let locked = remaining > 0;
        let retries = if latest {
            match self.failures {
                0..=4 => 5 - self.failures,
                5 | 10 | 15 => {
                    if locked {
                        0
                    } else {
                        5
                    }
                }
                6..=9 => 10 - self.failures,
                11..=14 => self.failures ^ 15,
                16..=20 => 20 - self.failures,
                _ => 0,
            }
        } else {
            5u32.saturating_sub(self.failures)
        };
        (
            retries as i32,
            if locked {
                (remaining / 1000 + 1) as i32
            } else {
                0
            },
        )
    }
}

impl PrivacyData {
    pub fn recovery_email(&self) -> Option<Vec<u8>> {
        if self.recovery_records.is_empty() {
            return (!self.email.is_empty()).then(|| self.email.as_bytes().to_vec());
        }
        let first = self
            .recovery_records
            .iter()
            .find(|entry| entry.ty == InfoType::Email as i32)?;
        let mut email = first.payload.to_vec();
        if let Some(continuation) = self
            .recovery_records
            .iter()
            .find(|entry| entry.ty == InfoType::EmailEx1 as i32)
        {
            email.extend_from_slice(&continuation.payload);
        }
        Some(email)
    }

    pub fn modify_recovery_info(&mut self, kind: InfoType, information: &[u8]) -> Result<()> {
        let mut records = self.recovery_records.clone();
        if records.is_empty() {
            for (index, bytes) in self.email.as_bytes().chunks(32).enumerate() {
                records.push(recovery_record(
                    if index == 0 {
                        InfoType::Email
                    } else {
                        InfoType::EmailEx1
                    },
                    bytes,
                ));
            }
        }
        let slot = |records: &mut Vec<SecureBufferEntry>, kind: InfoType| -> Result<usize> {
            if let Some(index) = records
                .iter()
                .position(|entry| entry.ty == 0 || entry.ty == kind as i32)
            {
                return Ok(index);
            }
            if records.len() == 6 {
                return Err(if kind == InfoType::SecurityQuestion {
                    CryptoengError::ParseError("no recovery slot available".into())
                } else {
                    CryptoengError::RecoverySlotsFull
                });
            }
            records.push(SecureBufferEntry {
                ty: 0,
                declared_len: 0,
                payload: [0; 32],
            });
            Ok(records.len() - 1)
        };
        let index = slot(&mut records, kind)?;
        records[index] = recovery_record(kind, &information[..information.len().min(32)]);
        if kind == InfoType::Email {
            let continuation = slot(&mut records, InfoType::EmailEx1)?;
            records[continuation] = if information.len() > 32 {
                recovery_record(InfoType::EmailEx1, &information[32..])
            } else {
                SecureBufferEntry {
                    ty: 0,
                    declared_len: 0,
                    payload: [0; 32],
                }
            };
        }
        self.recovery_records = records;
        Ok(())
    }

    pub fn retry_status(
        &mut self,
        kind: InfoType,
        now: &BootTimestamp,
        latest: bool,
    ) -> (i32, i32) {
        self.retry(kind).status(now, latest)
    }

    pub fn record_failure(&mut self, kind: InfoType, now: &BootTimestamp) -> i32 {
        let record = self.retry(kind);
        record.failures = record.failures.saturating_add(1);
        record.last_failed = Some(now.clone());
        let retries_left = record.status(now, true).0;
        if kind == InfoType::Password {
            self.retry_count = retries_left;
        } else {
            self.secure_question_retry_count = retries_left;
        }
        retries_left
    }

    fn retry(&mut self, kind: InfoType) -> &mut RetryRecord {
        if kind == InfoType::Password {
            &mut self.password_retry
        } else {
            &mut self.question_retry
        }
    }

    pub fn reset_retries(&mut self) {
        self.password_retry = RetryRecord::default();
        self.question_retry = RetryRecord::default();
        self.retry_count = 5;
        self.secure_question_retry_count = 5;
    }
}

fn recovery_record(kind: InfoType, bytes: &[u8]) -> SecureBufferEntry {
    let mut payload = [0; 32];
    payload[..bytes.len()].copy_from_slice(bytes);
    SecureBufferEntry {
        ty: kind as i32,
        declared_len: bytes.len() as u32,
        payload,
    }
}

#[derive(Debug)]
pub struct RuntimeState {
    boot_id: Option<String>,
}

impl RuntimeState {
    pub fn new() -> Self {
        let boot_id = fs::read_to_string("/proc/sys/kernel/random/boot_id")
            .ok()
            .map(|value| value.trim().to_owned())
            .filter(|value| {
                value.len() == 36
                    && value.bytes().enumerate().all(|(index, byte)| {
                        if matches!(index, 8 | 13 | 18 | 23) {
                            byte == b'-'
                        } else {
                            byte.is_ascii_hexdigit()
                        }
                    })
            });
        Self { boot_id }
    }

    pub fn now(&self) -> Result<BootTimestamp> {
        let boot_id = self
            .boot_id
            .as_ref()
            .ok_or(CryptoengError::ClockUnavailable)?;
        let mut time = libc::timespec {
            tv_sec: 0,
            tv_nsec: 0,
        };
        let status = unsafe { libc::clock_gettime(libc::CLOCK_BOOTTIME, &mut time) };
        if status != 0 {
            return Err(CryptoengError::ClockUnavailable);
        }
        let seconds = u64::try_from(time.tv_sec).map_err(|_| CryptoengError::ClockUnavailable)?;
        let nanoseconds =
            u64::try_from(time.tv_nsec).map_err(|_| CryptoengError::ClockUnavailable)?;
        let milliseconds = seconds
            .checked_mul(1000)
            .and_then(|value| value.checked_add(nanoseconds / 1_000_000))
            .ok_or(CryptoengError::ClockUnavailable)?;
        Ok(BootTimestamp {
            boot_id: boot_id.clone(),
            milliseconds,
        })
    }
}

#[derive(Debug, Default, Serialize, Deserialize)]
struct Persisted {
    #[serde(default)]
    privacy_data: HashMap<i32, PrivacyData>,
}

pub struct Storage {
    dir: PathBuf,
    key: [u8; 32],
    privacy_data: HashMap<i32, PrivacyData>,
}

impl Storage {
    pub fn load() -> Result<Self> {
        let dir = PathBuf::from(STORAGE_DIR);
        fs::create_dir_all(&dir)?;
        let _ = fs::set_permissions(&dir, fs::Permissions::from_mode(0o770));
        let key = load_or_create_key(&dir)?;

        let mut privacy_data = HashMap::new();
        let data_path = dir.join(DATA_FILE);
        if data_path.exists() {
            let envelope = fs::read(&data_path)?;
            let plaintext = decrypt(&key, &envelope)?;
            let persisted: Persisted = serde_json::from_slice(&plaintext)?;
            privacy_data = persisted.privacy_data;
        }
        Ok(Self {
            dir,
            key,
            privacy_data,
        })
    }

    pub fn save(&self) -> Result<()> {
        let persisted = Persisted {
            privacy_data: self.privacy_data.clone(),
        };
        let plaintext = serde_json::to_vec(&persisted)?;
        let envelope = encrypt(&self.key, &plaintext)?;
        let tmp_path = self.dir.join(format!("{DATA_FILE}.tmp"));
        let data_path = self.dir.join(DATA_FILE);
        let mut file = OpenOptions::new()
            .write(true)
            .create(true)
            .truncate(true)
            .mode(0o600)
            .open(&tmp_path)?;
        file.write_all(&envelope)?;
        file.sync_all()?;
        fs::rename(&tmp_path, &data_path)?;
        Ok(())
    }

    pub fn get_privacy_data(&self, system_type: SystemType) -> Option<&PrivacyData> {
        self.privacy_data.get(&(system_type as i32))
    }

    pub fn get_privacy_data_mut(&mut self, system_type: SystemType) -> &mut PrivacyData {
        self.privacy_data.entry(system_type as i32).or_default()
    }

    pub fn reset_privacy_data(&mut self, system_type: SystemType) {
        self.privacy_data.remove(&(system_type as i32));
    }
}

fn load_or_create_key(dir: &Path) -> Result<[u8; 32]> {
    let key_path = dir.join(KEY_FILE);
    if key_path.exists() {
        let bytes = fs::read(&key_path)?;
        return <[u8; 32]>::try_from(bytes.as_slice())
            .map_err(|_| CryptoengError::StorageError("invalid storage key length".into()));
    }
    let mut key = [0u8; 32];
    rand::rng().fill(&mut key[..]);
    let mut file = OpenOptions::new()
        .write(true)
        .create_new(true)
        .mode(0o600)
        .open(&key_path)?;
    file.write_all(&key)?;
    file.sync_all()?;
    Ok(key)
}

fn encrypt(key: &[u8; 32], plaintext: &[u8]) -> Result<Vec<u8>> {
    let cipher = Aes256Gcm::new_from_slice(key)
        .map_err(|_| CryptoengError::StorageError("invalid storage key".into()))?;
    let mut nonce_bytes = [0u8; NONCE_SIZE];
    rand::rng().fill(&mut nonce_bytes[..]);
    let ciphertext = cipher
        .encrypt(Nonce::from_slice(&nonce_bytes), plaintext)
        .map_err(|_| CryptoengError::StorageError("storage encryption failed".into()))?;
    let mut envelope = Vec::with_capacity(STORAGE_HEADER.len() + NONCE_SIZE + ciphertext.len());
    envelope.extend_from_slice(STORAGE_HEADER);
    envelope.extend_from_slice(&nonce_bytes);
    envelope.extend_from_slice(&ciphertext);
    Ok(envelope)
}

fn decrypt(key: &[u8; 32], envelope: &[u8]) -> Result<Vec<u8>> {
    if !envelope.starts_with(STORAGE_HEADER)
        || envelope.len() < STORAGE_HEADER.len() + NONCE_SIZE + TAG_SIZE
    {
        return Err(CryptoengError::StorageError(
            "invalid storage envelope".into(),
        ));
    }
    let payload = &envelope[STORAGE_HEADER.len()..];
    let cipher = Aes256Gcm::new_from_slice(key)
        .map_err(|_| CryptoengError::StorageError("invalid storage key".into()))?;
    cipher
        .decrypt(
            Nonce::from_slice(&payload[..NONCE_SIZE]),
            &payload[NONCE_SIZE..],
        )
        .map_err(|_| CryptoengError::StorageError("storage authentication failed".into()))
}
