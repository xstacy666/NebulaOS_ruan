use serde::{Deserialize, Serialize};

use crate::error::{CryptoengError, Result};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[repr(i32)]
pub enum MethodParamType {
    EncData = 1,
    Key = 2,
    Iv = 3,
    Aad = 4,
    Tag = 5,
    PlainData = 6,
    CertChain = 7,
    Signature = 8,
    Hash = 9,
    Hmac = 10,
    PmsAccountName = 11,
    EcdhPubKey = 12,
    EcdsaPubKey = 13,
    Attestation = 14,
    DeviceId = 15,
    UniqueId = 16,
    SsoId = 17,
    PasswordType = 18,
    PasswordHash = 19,
    Email = 20,
    SecQ = 21,
    RetryCount = 22,
    LockUntil = 23,
    SecQLockUntil = 24,
    Handler = 25,
    PrivacyData = 26,
    SpCipherData = 27,
    OpaqueData = 28,
    FindPhonePwdType = 29,
    FindPhonePwdHash = 30,
    FindPhoneRpmb = 31,
    FindPhoneDevId = 32,
    PmsLockScreenPwdT = 33,
    Secrets = 34,
    CipherText = 35,
    CipherTextLen = 36,
    PlainText = 37,
    PlainTextLen = 38,
    TmpAes = 39,
    RsaModulus = 40,
    RsaExponent = 41,
    RsaKeyVersion = 42,
    DeviceIdentity = 43,
    RsaPubKey = 44,
    ResultCode = 45,
    Content = 46,
    ReqRespType = 47,
    InstructionType = 48,
    MsgId = 49,
    PublicKey = 50,
    Modulus = 51,
    ValidUpTo = 52,
    ErrorLen = 53,
    PmsVersionT = 200,
    PmsTypeT = 201,
    PmsPwdInfoT = 202,
    PmsSecureInfoBufferT = 203,
    PmsVerifyTypeT = 204,
    PmsVerifyBufferT = 205,
    PmsChallengeT = 206,
    PmsInfoTypeT = 207,
    PmsInfoBufferT = 208,
    PmsHandlerT = 209,
    PmsPwdTypeT = 210,
    PmsPwdRetryCountLeftT = 211,
    PmsPwdLockTimeLeftT = 212,
    PmsCommonBuffer0T = 213,
    PmsCommonBuffer1T = 214,
    PmsCommonBuffer2T = 215,
    PmsCommonBuffer3T = 216,
    PmsHideEmailT = 217,
    PmsEncBufferT = 218,
    PmsSecureQuestionRetryCountLeftT = 219,
    PmsSecureQuestionLockTimeLeftT = 220,
    PmsLatestSecureRulesT = 221,
    PmsPwdUseridT = 500,
    PmsOldPwdTypeT = 501,
    PmsOldPwdLengthT = 502,
    PmsOldPwdBufferT = 503,
    PmsCurrentPwdTypeT = 504,
    PmsCurrentPwdLenT = 505,
    PmsCurrentPwdBufferT = 506,
    PmsPwdSystemTimeT = 507,
    PmsVerifyOldPwdResultT = 508,
    PmsOldPwdTryRemainingCount = 509,
    PmsOldPwdTryRemainingTime = 510,
    PmsOldPasswordType = 512,
    PmsCurrentPasswordBuffer = 513,
    PmsResultValidity = 514,
    PmsCurrentPasswordType = 515,
    PkiCalculationT = 600,
    PkiResponseT = 602,
    Unknown(i32),
}

impl MethodParamType {
    pub fn code(&self) -> i32 {
        match self {
            MethodParamType::EncData => 1,
            MethodParamType::Key => 2,
            MethodParamType::Iv => 3,
            MethodParamType::Aad => 4,
            MethodParamType::Tag => 5,
            MethodParamType::PlainData => 6,
            MethodParamType::CertChain => 7,
            MethodParamType::Signature => 8,
            MethodParamType::Hash => 9,
            MethodParamType::Hmac => 10,
            MethodParamType::PmsAccountName => 11,
            MethodParamType::EcdhPubKey => 12,
            MethodParamType::EcdsaPubKey => 13,
            MethodParamType::Attestation => 14,
            MethodParamType::DeviceId => 15,
            MethodParamType::UniqueId => 16,
            MethodParamType::SsoId => 17,
            MethodParamType::PasswordType => 18,
            MethodParamType::PasswordHash => 19,
            MethodParamType::Email => 20,
            MethodParamType::SecQ => 21,
            MethodParamType::RetryCount => 22,
            MethodParamType::LockUntil => 23,
            MethodParamType::SecQLockUntil => 24,
            MethodParamType::Handler => 25,
            MethodParamType::PrivacyData => 26,
            MethodParamType::SpCipherData => 27,
            MethodParamType::OpaqueData => 28,
            MethodParamType::FindPhonePwdType => 29,
            MethodParamType::FindPhonePwdHash => 30,
            MethodParamType::FindPhoneRpmb => 31,
            MethodParamType::FindPhoneDevId => 32,
            MethodParamType::PmsLockScreenPwdT => 33,
            MethodParamType::Secrets => 34,
            MethodParamType::CipherText => 35,
            MethodParamType::CipherTextLen => 36,
            MethodParamType::PlainText => 37,
            MethodParamType::PlainTextLen => 38,
            MethodParamType::TmpAes => 39,
            MethodParamType::RsaModulus => 40,
            MethodParamType::RsaExponent => 41,
            MethodParamType::RsaKeyVersion => 42,
            MethodParamType::DeviceIdentity => 43,
            MethodParamType::RsaPubKey => 44,
            MethodParamType::ResultCode => 45,
            MethodParamType::Content => 46,
            MethodParamType::ReqRespType => 47,
            MethodParamType::InstructionType => 48,
            MethodParamType::MsgId => 49,
            MethodParamType::PublicKey => 50,
            MethodParamType::Modulus => 51,
            MethodParamType::ValidUpTo => 52,
            MethodParamType::ErrorLen => 53,
            MethodParamType::PmsVersionT => 200,
            MethodParamType::PmsTypeT => 201,
            MethodParamType::PmsPwdInfoT => 202,
            MethodParamType::PmsSecureInfoBufferT => 203,
            MethodParamType::PmsVerifyTypeT => 204,
            MethodParamType::PmsVerifyBufferT => 205,
            MethodParamType::PmsChallengeT => 206,
            MethodParamType::PmsInfoTypeT => 207,
            MethodParamType::PmsInfoBufferT => 208,
            MethodParamType::PmsHandlerT => 209,
            MethodParamType::PmsPwdTypeT => 210,
            MethodParamType::PmsPwdRetryCountLeftT => 211,
            MethodParamType::PmsPwdLockTimeLeftT => 212,
            MethodParamType::PmsCommonBuffer0T => 213,
            MethodParamType::PmsCommonBuffer1T => 214,
            MethodParamType::PmsCommonBuffer2T => 215,
            MethodParamType::PmsCommonBuffer3T => 216,
            MethodParamType::PmsHideEmailT => 217,
            MethodParamType::PmsEncBufferT => 218,
            MethodParamType::PmsSecureQuestionRetryCountLeftT => 219,
            MethodParamType::PmsSecureQuestionLockTimeLeftT => 220,
            MethodParamType::PmsLatestSecureRulesT => 221,
            MethodParamType::PmsPwdUseridT => 500,
            MethodParamType::PmsOldPwdTypeT => 501,
            MethodParamType::PmsOldPwdLengthT => 502,
            MethodParamType::PmsOldPwdBufferT => 503,
            MethodParamType::PmsCurrentPwdTypeT => 504,
            MethodParamType::PmsCurrentPwdLenT => 505,
            MethodParamType::PmsCurrentPwdBufferT => 506,
            MethodParamType::PmsPwdSystemTimeT => 507,
            MethodParamType::PmsVerifyOldPwdResultT => 508,
            MethodParamType::PmsOldPwdTryRemainingCount => 509,
            MethodParamType::PmsOldPwdTryRemainingTime => 510,
            MethodParamType::PmsOldPasswordType => 512,
            MethodParamType::PmsCurrentPasswordBuffer => 513,
            MethodParamType::PmsResultValidity => 514,
            MethodParamType::PmsCurrentPasswordType => 515,
            MethodParamType::PkiCalculationT => 600,
            MethodParamType::PkiResponseT => 602,
            MethodParamType::Unknown(code) => *code,
        }
    }
}

impl From<i32> for MethodParamType {
    fn from(code: i32) -> Self {
        match code {
            1 => MethodParamType::EncData,
            2 => MethodParamType::Key,
            3 => MethodParamType::Iv,
            4 => MethodParamType::Aad,
            5 => MethodParamType::Tag,
            6 => MethodParamType::PlainData,
            7 => MethodParamType::CertChain,
            8 => MethodParamType::Signature,
            9 => MethodParamType::Hash,
            10 => MethodParamType::Hmac,
            11 => MethodParamType::PmsAccountName,
            12 => MethodParamType::EcdhPubKey,
            13 => MethodParamType::EcdsaPubKey,
            14 => MethodParamType::Attestation,
            15 => MethodParamType::DeviceId,
            16 => MethodParamType::UniqueId,
            17 => MethodParamType::SsoId,
            18 => MethodParamType::PasswordType,
            19 => MethodParamType::PasswordHash,
            20 => MethodParamType::Email,
            21 => MethodParamType::SecQ,
            22 => MethodParamType::RetryCount,
            23 => MethodParamType::LockUntil,
            24 => MethodParamType::SecQLockUntil,
            25 => MethodParamType::Handler,
            26 => MethodParamType::PrivacyData,
            27 => MethodParamType::SpCipherData,
            28 => MethodParamType::OpaqueData,
            29 => MethodParamType::FindPhonePwdType,
            30 => MethodParamType::FindPhonePwdHash,
            31 => MethodParamType::FindPhoneRpmb,
            32 => MethodParamType::FindPhoneDevId,
            33 => MethodParamType::PmsLockScreenPwdT,
            34 => MethodParamType::Secrets,
            35 => MethodParamType::CipherText,
            36 => MethodParamType::CipherTextLen,
            37 => MethodParamType::PlainText,
            38 => MethodParamType::PlainTextLen,
            39 => MethodParamType::TmpAes,
            40 => MethodParamType::RsaModulus,
            41 => MethodParamType::RsaExponent,
            42 => MethodParamType::RsaKeyVersion,
            43 => MethodParamType::DeviceIdentity,
            44 => MethodParamType::RsaPubKey,
            45 => MethodParamType::ResultCode,
            46 => MethodParamType::Content,
            47 => MethodParamType::ReqRespType,
            48 => MethodParamType::InstructionType,
            49 => MethodParamType::MsgId,
            50 => MethodParamType::PublicKey,
            51 => MethodParamType::Modulus,
            52 => MethodParamType::ValidUpTo,
            53 => MethodParamType::ErrorLen,
            200 => MethodParamType::PmsVersionT,
            201 => MethodParamType::PmsTypeT,
            202 => MethodParamType::PmsPwdInfoT,
            203 => MethodParamType::PmsSecureInfoBufferT,
            204 => MethodParamType::PmsVerifyTypeT,
            205 => MethodParamType::PmsVerifyBufferT,
            206 => MethodParamType::PmsChallengeT,
            207 => MethodParamType::PmsInfoTypeT,
            208 => MethodParamType::PmsInfoBufferT,
            209 => MethodParamType::PmsHandlerT,
            210 => MethodParamType::PmsPwdTypeT,
            211 => MethodParamType::PmsPwdRetryCountLeftT,
            212 => MethodParamType::PmsPwdLockTimeLeftT,
            213 => MethodParamType::PmsCommonBuffer0T,
            214 => MethodParamType::PmsCommonBuffer1T,
            215 => MethodParamType::PmsCommonBuffer2T,
            216 => MethodParamType::PmsCommonBuffer3T,
            217 => MethodParamType::PmsHideEmailT,
            218 => MethodParamType::PmsEncBufferT,
            219 => MethodParamType::PmsSecureQuestionRetryCountLeftT,
            220 => MethodParamType::PmsSecureQuestionLockTimeLeftT,
            221 => MethodParamType::PmsLatestSecureRulesT,
            500 => MethodParamType::PmsPwdUseridT,
            501 => MethodParamType::PmsOldPwdTypeT,
            502 => MethodParamType::PmsOldPwdLengthT,
            503 => MethodParamType::PmsOldPwdBufferT,
            504 => MethodParamType::PmsCurrentPwdTypeT,
            505 => MethodParamType::PmsCurrentPwdLenT,
            506 => MethodParamType::PmsCurrentPwdBufferT,
            507 => MethodParamType::PmsPwdSystemTimeT,
            508 => MethodParamType::PmsVerifyOldPwdResultT,
            509 => MethodParamType::PmsOldPwdTryRemainingCount,
            510 => MethodParamType::PmsOldPwdTryRemainingTime,
            512 => MethodParamType::PmsOldPasswordType,
            513 => MethodParamType::PmsCurrentPasswordBuffer,
            514 => MethodParamType::PmsResultValidity,
            515 => MethodParamType::PmsCurrentPasswordType,
            600 => MethodParamType::PkiCalculationT,
            602 => MethodParamType::PkiResponseT,
            _ => MethodParamType::Unknown(code),
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize, Default)]
#[repr(i32)]
pub enum PasswordQuality {
    #[default]
    Unspecified = 0,
    NumericFour = 1,
    NumericSix = 2,
    NumericComplex = 3,
    Complex = 4,
    Pattern = 5,
}

impl PasswordQuality {
    fn from_i32(code: i32) -> Option<Self> {
        match code {
            0 => Some(PasswordQuality::Unspecified),
            1 => Some(PasswordQuality::NumericFour),
            2 => Some(PasswordQuality::NumericSix),
            3 => Some(PasswordQuality::NumericComplex),
            4 => Some(PasswordQuality::Complex),
            5 => Some(PasswordQuality::Pattern),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[repr(i32)]
pub enum InfoType {
    Unspecified = 0,
    Password = 1,
    SecurityQuestion = 2,
    Account = 3,
    Email = 4,
    Fingerprint = 5,
    Face = 6,
    Iris = 7,
    ScreenLockToken = 8,
    StoredPassword = 9,
    EmailEx1 = 10,
}

impl InfoType {
    fn from_i32(code: i32) -> Option<Self> {
        match code {
            0 => Some(InfoType::Unspecified),
            1 => Some(InfoType::Password),
            2 => Some(InfoType::SecurityQuestion),
            3 => Some(InfoType::Account),
            4 => Some(InfoType::Email),
            5 => Some(InfoType::Fingerprint),
            6 => Some(InfoType::Face),
            7 => Some(InfoType::Iris),
            8 => Some(InfoType::ScreenLockToken),
            9 => Some(InfoType::StoredPassword),
            10 => Some(InfoType::EmailEx1),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[repr(i32)]
pub enum SystemType {
    PrivacyPassword = 1,
    ScreenLock = 2,
}

impl SystemType {
    fn from_i32(code: i32) -> Option<Self> {
        match code {
            1 => Some(SystemType::PrivacyPassword),
            2 => Some(SystemType::ScreenLock),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct SecureBufferEntry {
    pub ty: i32,
    pub declared_len: u32,
    pub payload: [u8; 32],
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SecureBuffer {
    pub entries: Vec<SecureBufferEntry>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Value {
    SystemType(SystemType),
    PasswordQuality(PasswordQuality),
    InfoType(InfoType),
    SecureBuffer(SecureBuffer),
    Integer(i32),
    String(String),
    Buffer(Vec<u8>),
}

impl Value {
    pub fn as_bytes(&self) -> Vec<u8> {
        match self {
            Value::Buffer(bytes) => bytes.clone(),
            Value::String(text) => text.as_bytes().to_vec(),
            Value::Integer(value) => value.to_be_bytes().to_vec(),
            Value::SystemType(value) => (*value as i32).to_be_bytes().to_vec(),
            Value::PasswordQuality(value) => (*value as i32).to_be_bytes().to_vec(),
            Value::InfoType(value) => (*value as i32).to_be_bytes().to_vec(),
            Value::SecureBuffer(buffer) => {
                let mut bytes = Vec::with_capacity(buffer.entries.len() * 40);
                for entry in &buffer.entries {
                    bytes.extend_from_slice(&entry.ty.to_be_bytes());
                    bytes.extend_from_slice(&entry.declared_len.to_be_bytes());
                    bytes.extend_from_slice(&entry.payload);
                }
                bytes
            }
        }
    }

    fn parse(ty: MethodParamType, bytes: &[u8]) -> Result<Self> {
        let needs_value = matches!(
            ty,
            MethodParamType::PmsTypeT
                | MethodParamType::PmsPwdTypeT
                | MethodParamType::PmsVerifyTypeT
                | MethodParamType::PmsInfoTypeT
        );
        if bytes.is_empty() && needs_value {
            return Err(CryptoengError::MissingParameter(ty));
        }
        match ty {
            MethodParamType::PmsTypeT => {
                let code = read_i32(ty, bytes)?;
                SystemType::from_i32(code).map(Value::SystemType).ok_or(
                    CryptoengError::InvalidParameterData {
                        param: ty,
                        value: code,
                    },
                )
            }
            MethodParamType::PmsPwdTypeT => {
                let code = read_i32(ty, bytes)?;
                PasswordQuality::from_i32(code)
                    .map(Value::PasswordQuality)
                    .ok_or(CryptoengError::InvalidParameterData {
                        param: ty,
                        value: code,
                    })
            }
            MethodParamType::PmsVerifyTypeT | MethodParamType::PmsInfoTypeT => {
                let code = read_i32(ty, bytes)?;
                InfoType::from_i32(code).map(Value::InfoType).ok_or(
                    CryptoengError::InvalidParameterData {
                        param: ty,
                        value: code,
                    },
                )
            }
            MethodParamType::PmsSecureInfoBufferT => {
                Ok(Value::SecureBuffer(parse_secure_buffer(bytes)?))
            }
            MethodParamType::PmsPwdRetryCountLeftT
            | MethodParamType::PmsSecureQuestionRetryCountLeftT => {
                Ok(Value::Integer(read_i32(ty, bytes)?))
            }
            MethodParamType::PmsAccountName => {
                Ok(Value::String(String::from_utf8_lossy(bytes).to_string()))
            }
            _ => Ok(Value::Buffer(bytes.to_vec())),
        }
    }
}

fn read_i32(ty: MethodParamType, bytes: &[u8]) -> Result<i32> {
    if bytes.len() != 4 {
        return Err(CryptoengError::ParameterBufferTooShort {
            param: ty,
            expected: 4,
            got: bytes.len(),
        });
    }
    Ok(i32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]))
}

fn parse_secure_buffer(bytes: &[u8]) -> Result<SecureBuffer> {
    const RECORD_SIZE: usize = 40;
    if bytes.is_empty() || !bytes.len().is_multiple_of(RECORD_SIZE) {
        return Err(CryptoengError::MalformedParameter(
            MethodParamType::PmsSecureInfoBufferT,
        ));
    }
    let mut entries = Vec::with_capacity(bytes.len() / RECORD_SIZE);
    for record in bytes.chunks_exact(RECORD_SIZE) {
        let ty = i32::from_be_bytes([record[0], record[1], record[2], record[3]]);
        let declared_len = u32::from_be_bytes([record[4], record[5], record[6], record[7]]);
        if declared_len as usize > 32 {
            return Err(CryptoengError::MalformedParameter(
                MethodParamType::PmsSecureInfoBufferT,
            ));
        }
        let mut payload = [0u8; 32];
        payload.copy_from_slice(&record[8..]);
        entries.push(SecureBufferEntry {
            ty,
            declared_len,
            payload,
        });
    }
    Ok(SecureBuffer { entries })
}

#[derive(Debug, Clone)]
pub struct Parameter {
    param_type: MethodParamType,
    value: Value,
}

impl Parameter {
    pub fn new(param_type: MethodParamType, data: impl AsRef<[u8]>) -> Result<Self> {
        Ok(Self {
            param_type,
            value: Value::parse(param_type, data.as_ref())?,
        })
    }

    pub fn param_type(&self) -> MethodParamType {
        self.param_type
    }

    pub fn value(&self) -> &Value {
        &self.value
    }

    pub fn data(&self) -> Vec<u8> {
        self.value.as_bytes()
    }

    pub fn to_be_bytes(&self) -> Vec<u8> {
        let data = self.value.as_bytes();
        let mut result = Vec::with_capacity(8 + data.len());
        result.extend_from_slice(&self.param_type.code().to_be_bytes());
        result.extend_from_slice(&(data.len() as i32).to_be_bytes());
        result.extend_from_slice(&data);
        result
    }
}
