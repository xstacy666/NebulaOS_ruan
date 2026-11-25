use std::fmt;

use crate::types::{MethodParamType, SystemType};

#[derive(Debug, Clone)]
pub enum CryptoengError {
    ParseError(String),
    BufferTooShort {
        got: usize,
        min: usize,
    },
    MissingParameter(MethodParamType),
    ParameterBufferTooShort {
        param: MethodParamType,
        expected: usize,
        got: usize,
    },
    InvalidParameterData {
        param: MethodParamType,
        value: i32,
    },
    MalformedParameter(MethodParamType),
    UnsupportedOperation,
    UnsupportedEmail,
    ChallengeExpired,
    ChallengeMismatch(SystemType),
    AlreadyEnrolled(SystemType),
    NotEnrolled(SystemType),
    RecoverySlotsFull,
    VerificationFailed {
        retries_left: i32,
    },
    ClockUnavailable,
    StorageError(String),
}

impl fmt::Display for CryptoengError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            CryptoengError::ParseError(msg) => write!(f, "parse error: {}", msg),
            CryptoengError::BufferTooShort { got, min } => {
                write!(f, "buffer too short: got {}, need {}", got, min)
            }
            CryptoengError::MissingParameter(p) => write!(f, "missing parameter: {:?}", p),
            CryptoengError::ParameterBufferTooShort {
                param,
                expected,
                got,
            } => write!(
                f,
                "parameter buffer too short for {:?}: expected {}, got {}",
                param, expected, got
            ),
            CryptoengError::InvalidParameterData { param, value } => {
                write!(f, "invalid parameter value {} for {:?}", value, param)
            }
            CryptoengError::MalformedParameter(p) => write!(f, "malformed parameter: {:?}", p),
            CryptoengError::UnsupportedOperation => write!(f, "operation not supported"),
            CryptoengError::UnsupportedEmail => write!(f, "invalid email"),
            CryptoengError::ChallengeExpired => write!(f, "challenge expired"),
            CryptoengError::ChallengeMismatch(system) => {
                write!(f, "challenge mismatch for {:?}", system)
            }
            CryptoengError::AlreadyEnrolled(system) => {
                write!(f, "already enrolled for {:?}", system)
            }
            CryptoengError::NotEnrolled(system) => write!(f, "not enrolled for {:?}", system),
            CryptoengError::RecoverySlotsFull => write!(f, "recovery slots are full"),
            CryptoengError::VerificationFailed { retries_left } => {
                write!(f, "verification failed, retries left: {}", retries_left)
            }
            CryptoengError::ClockUnavailable => write!(f, "boot clock unavailable"),
            CryptoengError::StorageError(msg) => write!(f, "storage error: {}", msg),
        }
    }
}

impl std::error::Error for CryptoengError {}

impl CryptoengError {
    pub fn result_code(&self) -> Option<i32> {
        match self {
            CryptoengError::UnsupportedOperation => Some(10012),
            CryptoengError::MissingParameter(parameter) => Some(10100 + parameter.code()),
            CryptoengError::ParameterBufferTooShort { .. }
            | CryptoengError::InvalidParameterData { .. }
            | CryptoengError::MalformedParameter(_)
            | CryptoengError::ParseError(_) => Some(10014),
            CryptoengError::RecoverySlotsFull => Some(10027),
            CryptoengError::AlreadyEnrolled(_) => Some(10031),
            CryptoengError::UnsupportedEmail => Some(10001),
            CryptoengError::VerificationFailed { .. } => Some(10017),
            CryptoengError::ChallengeMismatch(_) => Some(10033),
            CryptoengError::ChallengeExpired => Some(10034),
            _ => None,
        }
    }
}

impl From<std::io::Error> for CryptoengError {
    fn from(error: std::io::Error) -> Self {
        CryptoengError::StorageError(error.to_string())
    }
}

impl From<serde_json::Error> for CryptoengError {
    fn from(error: serde_json::Error) -> Self {
        CryptoengError::StorageError(error.to_string())
    }
}

pub type Result<T> = std::result::Result<T, CryptoengError>;
