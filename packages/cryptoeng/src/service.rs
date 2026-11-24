use std::sync::Arc;

use binder::{BinderFeatures, Interface, Strong};

use crate::hal::Hal;
use crate::ICryptoeng::{BnCryptoeng, ICryptoeng as ICryptoengTrait};
use crate::{HASH, VERSION};

pub struct CryptoengService {
    hal: Arc<Hal>,
}

impl CryptoengService {
    pub fn new(hal: Arc<Hal>) -> Self {
        Self { hal }
    }

    pub fn create_binder(self) -> Strong<dyn ICryptoengTrait> {
        BnCryptoeng::new_binder(self, BinderFeatures::default())
    }
}

impl Interface for CryptoengService {}

impl ICryptoengTrait for CryptoengService {
    fn cryptoeng_invoke_command(&self, request: Vec<u8>) -> binder::Result<Vec<u8>> {
        self.hal.handle_command(request)
    }

    fn get_interface_version(&self) -> binder::Result<i32> {
        Ok(VERSION)
    }

    fn get_interface_hash(&self) -> binder::Result<String> {
        Ok(HASH.to_string())
    }
}
