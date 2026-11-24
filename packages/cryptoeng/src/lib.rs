pub mod error;
pub mod log;
pub mod pms;
pub mod request;
pub mod response;
pub mod storage;
pub mod types;

#[allow(unexpected_cfgs, unused_mut)]
#[path = "gen/vendor/oplus/hardware/cryptoeng/ICryptoeng.rs"]
pub mod ICryptoeng;

pub use error::{CryptoengError, Result};
pub use request::{parse, CryptoengRequest, Native, Request, Standard};
pub use ICryptoeng::{
    transactions, BnCryptoeng, BpCryptoeng, ICryptoengDefaultRef, HASH, SERVICE_NAME,
    SERVICE_STABILITY, VERSION,
};

pub mod hal;
pub mod service;

pub use hal::Hal;
pub use service::CryptoengService;
