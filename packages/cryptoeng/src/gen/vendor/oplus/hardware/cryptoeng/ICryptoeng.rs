#![forbid(unsafe_code)]
#![allow(non_upper_case_globals)]
#![allow(non_snake_case)]
#[allow(unused_imports)]
use binder::binder_impl::IBinderInternal;
use binder::declare_binder_interface;

#[cfg(feature = "vintf_stability")]
pub const SERVICE_STABILITY: binder::binder_impl::Stability = binder::binder_impl::Stability::Vintf;
#[cfg(not(feature = "vintf_stability"))]
pub const SERVICE_STABILITY: binder::binder_impl::Stability = binder::binder_impl::Stability::Local;

declare_binder_interface! {
  ICryptoeng["vendor.oplus.hardware.cryptoeng.ICryptoeng"] {
    native: BnCryptoeng(on_transact),
    proxy: BpCryptoeng {
      cached_version: std::sync::atomic::AtomicI32 = std::sync::atomic::AtomicI32::new(-1),
      cached_hash: std::sync::Mutex<Option<String>> = std::sync::Mutex::new(None)
    },
    stability: SERVICE_STABILITY,
  }
}

pub const SERVICE_NAME: &str = "vendor.oplus.hardware.cryptoeng.ICryptoeng/default";

pub trait ICryptoeng: binder::Interface + Send {
    fn get_descriptor() -> &'static str
    where
        Self: Sized,
    {
        "vendor.oplus.hardware.cryptoeng.ICryptoeng"
    }
    fn cryptoeng_invoke_command(&self, request: Vec<u8>) -> binder::Result<Vec<u8>>;
    fn get_interface_version(&self) -> binder::Result<i32> {
        Ok(VERSION)
    }
    fn get_interface_hash(&self) -> binder::Result<String> {
        Ok(HASH.into())
    }
    fn getDefaultImpl() -> ICryptoengDefaultRef
    where
        Self: Sized,
    {
        DEFAULT_IMPL.lock().unwrap().clone()
    }
    fn setDefaultImpl(d: ICryptoengDefaultRef) -> ICryptoengDefaultRef
    where
        Self: Sized,
    {
        std::mem::replace(&mut *DEFAULT_IMPL.lock().unwrap(), d)
    }
}

pub mod transactions {
    pub const CRYPTOENG_INVOKE_COMMAND: binder::binder_impl::TransactionCode =
        binder::binder_impl::FIRST_CALL_TRANSACTION + 0;
    pub const GET_INTERFACE_VERSION: binder::binder_impl::TransactionCode =
        binder::binder_impl::FIRST_CALL_TRANSACTION + 16777214;
    pub const GET_INTERFACE_HASH: binder::binder_impl::TransactionCode =
        binder::binder_impl::FIRST_CALL_TRANSACTION + 16777213;
}

pub type ICryptoengDefaultRef = Option<std::sync::Arc<dyn ICryptoeng>>;
static DEFAULT_IMPL: std::sync::Mutex<ICryptoengDefaultRef> = std::sync::Mutex::new(None);
pub const VERSION: i32 = 1;
pub const HASH: &str = "765136eb397eb5b85ee1e089d8fec72c15e266b3";

impl BpCryptoeng {
    fn read_response_void(
        &self,
        _aidl_reply: std::result::Result<binder::binder_impl::Parcel, binder::StatusCode>,
    ) -> binder::Result<()> {
        let _aidl_reply = _aidl_reply?;
        let _aidl_status: binder::Status = _aidl_reply.read()?;
        if !_aidl_status.is_ok() {
            return Err(_aidl_status);
        }
        Ok(())
    }

    fn read_response_value<T: binder::binder_impl::Deserialize>(
        &self,
        _aidl_reply: std::result::Result<binder::binder_impl::Parcel, binder::StatusCode>,
    ) -> binder::Result<T> {
        let _aidl_reply = _aidl_reply?;
        let _aidl_status: binder::Status = _aidl_reply.read()?;
        if !_aidl_status.is_ok() {
            return Err(_aidl_status);
        }
        let _aidl_return: T = _aidl_reply.read()?;
        Ok(_aidl_return)
    }
}

impl ICryptoeng for BpCryptoeng {
    fn cryptoeng_invoke_command(&self, request: Vec<u8>) -> binder::Result<Vec<u8>> {
        let mut _aidl_data = self.binder.prepare_transact()?;
        _aidl_data.write(&request)?;
        let _aidl_reply = self.binder.submit_transact(
            transactions::CRYPTOENG_INVOKE_COMMAND,
            _aidl_data,
            binder::binder_impl::FLAG_PRIVATE_LOCAL,
        );
        self.read_response_value(_aidl_reply)
    }
}

fn on_transact(
    _aidl_service: &dyn ICryptoeng,
    _aidl_code: binder::binder_impl::TransactionCode,
    _aidl_data: &binder::binder_impl::BorrowedParcel<'_>,
    _aidl_reply: &mut binder::binder_impl::BorrowedParcel<'_>,
) -> std::result::Result<(), binder::StatusCode> {
    match _aidl_code {
        transactions::CRYPTOENG_INVOKE_COMMAND => {
            let _arg_request: Vec<u8> = _aidl_data.read()?;
            let _aidl_return = _aidl_service.cryptoeng_invoke_command(_arg_request);
            match &_aidl_return {
                Ok(_aidl_return) => {
                    _aidl_reply.write(&binder::Status::from(binder::StatusCode::OK))?;
                    _aidl_reply.write(_aidl_return)?;
                }
                Err(_aidl_status) => _aidl_reply.write(_aidl_status)?,
            }
            Ok(())
        }
        transactions::GET_INTERFACE_VERSION => {
            let _aidl_return = _aidl_service.get_interface_version();
            match &_aidl_return {
                Ok(_aidl_return) => {
                    _aidl_reply.write(&binder::Status::from(binder::StatusCode::OK))?;
                    _aidl_reply.write(_aidl_return)?;
                }
                Err(_aidl_status) => _aidl_reply.write(_aidl_status)?,
            }
            Ok(())
        }
        transactions::GET_INTERFACE_HASH => {
            let _aidl_return = _aidl_service.get_interface_hash();
            match &_aidl_return {
                Ok(_aidl_return) => {
                    _aidl_reply.write(&binder::Status::from(binder::StatusCode::OK))?;
                    _aidl_reply.write(_aidl_return)?;
                }
                Err(_aidl_status) => _aidl_reply.write(_aidl_status)?,
            }
            Ok(())
        }
        _ => Err(binder::StatusCode::UNKNOWN_TRANSACTION),
    }
}

impl ICryptoeng for binder::binder_impl::Binder<BnCryptoeng> {
    fn cryptoeng_invoke_command(&self, request: Vec<u8>) -> binder::Result<Vec<u8>> {
        self.0.cryptoeng_invoke_command(request)
    }
}
