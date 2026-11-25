use std::sync::Mutex;

use crate::storage::{RuntimeState, Storage};
use crate::types::MethodType;
use crate::{loge, logi, logw, parse, pms, response};

enum State {
    Ready {
        storage: Storage,
        runtime: RuntimeState,
    },
    Failed(String),
}

pub struct Hal {
    state: Mutex<State>,
}

impl Hal {
    pub fn new() -> Option<Self> {
        let state = match Storage::load() {
            Ok(storage) => {
                logi!("PMS storage loaded from {}", crate::storage::STORAGE_DIR);
                State::Ready {
                    storage,
                    runtime: RuntimeState::new(),
                }
            }
            Err(error) => {
                logw!("PMS storage unavailable: {}", error);
                State::Failed(error.to_string())
            }
        };
        Some(Self {
            state: Mutex::new(state),
        })
    }

    pub fn handle_command(&self, request: Vec<u8>) -> binder::Result<Vec<u8>> {
        if request.len() < 4 {
            loge!("request too short: {} bytes", request.len());
            return Ok(vec![]);
        }

        let req = match parse(&request) {
            Ok(r) => r,
            Err(e) => {
                loge!("parse error: {:?}", e);
                return Ok(vec![]);
            }
        };

        let method = req.method_type();
        logi!("handle_command: method={:?}", method);

        let response = match method {
            MethodType::CeCmdPmsEnroll
            | MethodType::CeCmdPmsVerify
            | MethodType::CeCmdPmsModify
            | MethodType::CeCmdPmsDelete
            | MethodType::CeCmdPmsGetInfo
            | MethodType::CeCmdPmsRequestEncryptData
            | MethodType::CeCmdPmsResetAllData => self.pms_response(req.as_ref(), method),
            MethodType::CeCmdGoogleAttestationWrite => self.handle_attestation_write(req.as_ref()),
            MethodType::CeCmdGoogleAttestationVerify => {
                self.handle_attestation_verify(req.as_ref())
            }
            MethodType::CeCmdWriteSecret => self.handle_write_secret(req.as_ref()),
            MethodType::CeCmdFindphoneGetStatus => self.handle_findphone_status(req.as_ref()),
            MethodType::CeCmdGeneratePkiCert => self.handle_generate_pki_cert(req.as_ref()),
            MethodType::CeCmdVerifyPkiCert => self.handle_verify_pki_cert(req.as_ref()),
            MethodType::CeCmdPresetDataDec => self.handle_preset_data_dec(req.as_ref()),
            MethodType::CeCmdGroupPkiVerify => self.handle_group_pki_verify(req.as_ref()),
            MethodType::CeCmdHdcpKeyWrite => self.handle_hdcp_key_write(req.as_ref()),
            MethodType::CeCmdHdcpKeyVerify => self.handle_hdcp_key_verify(req.as_ref()),
            MethodType::CeCmdCleanUp => self.handle_cleanup(req.as_ref()),
            MethodType::CeCmdGetSecuretype => self.handle_get_securetype(req.as_ref()),
            MethodType::CeCmdWidevineSupport => self.handle_widevine_support(req.as_ref()),
            MethodType::CeCmdCryptoSupport => self.handle_crypto_support(req.as_ref()),
            MethodType::CeCmdEngineer => self.handle_engineer(req.as_ref()),
            MethodType::CeCmdRunFindphoneGetMemberFromRpmb => {
                self.handle_findphone_rpmb_get(req.as_ref())
            }
            MethodType::CeCmdRunFindphoneResetRpmb => {
                self.handle_findphone_rpmb_reset(req.as_ref())
            }
            MethodType::CeCmdRunFindphoneIsSupportUnlockByPasswd => {
                self.handle_findphone_unlock_support(req.as_ref())
            }
            MethodType::CeCmdRunFindphoneSetPasswdType => {
                self.handle_findphone_set_passwd(req.as_ref())
            }
            MethodType::CeCmdRunFindphoneGetPasswdType => {
                self.handle_findphone_get_passwd(req.as_ref())
            }
            MethodType::CeCmdRunFindphoneVerifyPasswd => {
                self.handle_findphone_verify_passwd(req.as_ref())
            }
            MethodType::CeCmdRunFindphoneGetSalt => self.handle_findphone_get_salt(req.as_ref()),
            MethodType::CeCmdRpmbStorageWriteCustomizeMsg => self.handle_rpmb_write(req.as_ref()),
            MethodType::CeCmdRpmbStorageReadCustomizeMsg => self.handle_rpmb_read(req.as_ref()),
            MethodType::CeCmdCoreGetVersion => self.handle_core_version(req.as_ref()),
            MethodType::CeCmdCoreGetFeatureSupport => self.handle_core_features(req.as_ref()),
            MethodType::CeCmdCoreGetDeviceid => self.handle_core_device_id(req.as_ref()),
            MethodType::CeCmdIdMseEncCmd => self.handle_mse_enc(req.as_ref()),
            MethodType::CeCmdIdMseDecCmd => self.handle_mse_dec(req.as_ref()),
            MethodType::CeCmdRunPkiHash => self.handle_pki_hash(req.as_ref()),
            MethodType::CeCmdRunPkiHmac => self.handle_pki_hmac(req.as_ref()),
            MethodType::CeCmdRunPkiHkdf => self.handle_pki_hkdf(req.as_ref()),
            MethodType::CeCmdRunPkiAes => self.handle_pki_aes(req.as_ref()),
            MethodType::CeCmdRunPkiRsa => self.handle_pki_rsa(req.as_ref()),
            MethodType::CeCmdRunPkiEcdh => self.handle_pki_ecdh(req.as_ref()),
            MethodType::CeCmdRunPkiEcies => self.handle_pki_ecies(req.as_ref()),
            MethodType::CeCmdRunPkiEcdsa => self.handle_pki_ecdsa(req.as_ref()),
            MethodType::CeCmdRunPkiCertVerify => self.handle_pki_cert_verify(req.as_ref()),
            MethodType::CeCmdRunPkiSign => self.handle_pki_sign(req.as_ref()),
            MethodType::CeCmdRunPkiEciesCertEnc => self.handle_pki_ecies_enc(req.as_ref()),
            MethodType::CeCmdRunPkiEciesCertDec => self.handle_pki_ecies_dec(req.as_ref()),
            MethodType::CeCmdRunPkiPresetDataDec => self.handle_pki_preset_dec(req.as_ref()),
            MethodType::CeCmdRunPkiBasicAttestation => {
                self.handle_pki_basic_attestation(req.as_ref())
            }
            MethodType::CeCmdRunPkiCommonIdAttestation => {
                self.handle_pki_common_id_attestation(req.as_ref())
            }
            MethodType::CeCmdRunPkiExportCert => self.handle_pki_export_cert(req.as_ref()),
            MethodType::TaCmdStoreOldPassword => self.handle_ta_store_old_pwd(req.as_ref()),
            MethodType::TaCmdRetrievePasswordByOldPwd => self.handle_ta_retrieve_pwd(req.as_ref()),
            MethodType::TaCmdPasswordRetrieveValidity => self.handle_ta_pwd_validity(req.as_ref()),
            MethodType::TaCmdDeleteOldPassword => self.handle_ta_delete_old_pwd(req.as_ref()),
            MethodType::TaCmdPasswordVerifyResult => self.handle_ta_pwd_verify_result(req.as_ref()),
            MethodType::CeCmdIdOlockStateOpen => self.handle_olock_state_open(req.as_ref()),
            MethodType::CeCmdIdOlockStateClose => self.handle_olock_state_close(req.as_ref()),
            MethodType::CeCmdIdOlockSecureLockStateOpen => {
                self.handle_olock_secure_open(req.as_ref())
            }
            MethodType::CeCmdIdOlockSecureLockStateClose => {
                self.handle_olock_secure_close(req.as_ref())
            }
            MethodType::CeCmdIdOlockStateGet => self.handle_olock_state_get(req.as_ref()),
            MethodType::CeCmdIdOlockSecureLockStateGet => {
                self.handle_olock_secure_get(req.as_ref())
            }
            MethodType::CeCmdIdOlockGetPasswdType => {
                self.handle_olock_get_passwd_type(req.as_ref())
            }
            MethodType::CeCmdIdOlockGetSalt => self.handle_olock_get_salt(req.as_ref()),
            MethodType::CeCmdIdOlockVerifyPasswd => self.handle_olock_verify_passwd(req.as_ref()),
            _ => {
                logw!("unhandled method: {:?}", method);
                Vec::new()
            }
        };

        Ok(response)
    }

    fn pms_response(&self, req: &dyn crate::CryptoengRequest, method: MethodType) -> Vec<u8> {
        let mut guard = match self.state.lock() {
            Ok(guard) => guard,
            Err(_) => {
                loge!("PMS state lock poisoned");
                return response::failure(method, None);
            }
        };
        let (storage, runtime) = match &mut *guard {
            State::Ready { storage, runtime } => (storage, runtime),
            State::Failed(message) => {
                loge!("PMS unavailable: {}", message);
                return response::failure(method, None);
            }
        };
        let result = match method {
            MethodType::CeCmdPmsEnroll => pms::enroll(req, storage, runtime),
            MethodType::CeCmdPmsVerify => pms::verify(req, storage, runtime),
            MethodType::CeCmdPmsModify => pms::modify(req, storage, runtime),
            MethodType::CeCmdPmsDelete => pms::delete(req, storage, runtime),
            MethodType::CeCmdPmsGetInfo => pms::get_info(req, storage, runtime),
            MethodType::CeCmdPmsRequestEncryptData => {
                pms::request_encrypt_data(req, storage, runtime)
            }
            MethodType::CeCmdPmsResetAllData => pms::reset_all_data(req, storage, runtime),
            _ => return response::failure(method, None),
        };
        match result {
            Ok(bytes) => {
                logi!("PMS {:?} succeeded ({} bytes)", method, bytes.len());
                bytes
            }
            Err(error) => {
                loge!(
                    "PMS {:?} failed: {} (code {:?})",
                    method,
                    error,
                    error.result_code()
                );
                response::failure(method, error.result_code())
            }
        }
    }

    fn ok_response(&self) -> Vec<u8> {
        vec![0]
    }

    fn empty_response(&self) -> Vec<u8> {
        Vec::new()
    }

    // Stub handlers - return empty response for now
    fn handle_attestation_write(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_attestation_verify(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_write_secret(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_findphone_status(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_generate_pki_cert(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_verify_pki_cert(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_preset_data_dec(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_group_pki_verify(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_hdcp_key_write(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_hdcp_key_verify(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_cleanup(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_get_securetype(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1]
    }
    fn handle_widevine_support(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1]
    }
    fn handle_crypto_support(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1]
    }
    fn handle_engineer(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_findphone_rpmb_get(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_findphone_rpmb_reset(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_findphone_unlock_support(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1]
    }
    fn handle_findphone_set_passwd(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_findphone_get_passwd(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![0]
    }
    fn handle_findphone_verify_passwd(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1]
    }
    fn handle_findphone_get_salt(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![0; 16]
    }
    fn handle_rpmb_write(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_rpmb_read(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_core_version(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1, 0, 0, 0]
    }
    fn handle_core_features(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1]
    }
    fn handle_core_device_id(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![0; 16]
    }
    fn handle_mse_enc(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_mse_dec(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_hash(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_hmac(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_hkdf(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_aes(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_rsa(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_ecdh(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_ecies(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_ecdsa(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_cert_verify(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1]
    }
    fn handle_pki_sign(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_ecies_enc(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_ecies_dec(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_preset_dec(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_basic_attestation(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_common_id_attestation(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_pki_export_cert(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_ta_store_old_pwd(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_ta_retrieve_pwd(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.empty_response()
    }
    fn handle_ta_pwd_validity(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1]
    }
    fn handle_ta_delete_old_pwd(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_ta_pwd_verify_result(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1]
    }
    fn handle_olock_state_open(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_olock_state_close(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_olock_secure_open(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_olock_secure_close(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        self.ok_response()
    }
    fn handle_olock_state_get(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![0]
    }
    fn handle_olock_secure_get(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![0]
    }
    fn handle_olock_get_passwd_type(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![0]
    }
    fn handle_olock_get_salt(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![0; 16]
    }
    fn handle_olock_verify_passwd(&self, _req: &dyn crate::CryptoengRequest) -> Vec<u8> {
        vec![1]
    }
}
