use std::sync::Arc;

use binder::binder_impl::Proxy;

use cryptoeng_hal::hal::Hal;
use cryptoeng_hal::service::CryptoengService;
use cryptoeng_hal::ICryptoeng::{BpCryptoeng, ICryptoeng as ICryptoengTrait};
use cryptoeng_hal::{loge, logi, SERVICE_NAME};

fn run_selftest(hal: Arc<Hal>) -> i32 {
    let service = CryptoengService::new(hal).create_binder();
    let proxy = match BpCryptoeng::from_binder(service.as_binder()) {
        Ok(proxy) => proxy,
        Err(error) => {
            loge!("selftest: from_binder failed: {error:?}");
            return 1;
        }
    };

    let mut failures = 0;
    macro_rules! check {
        ($name:literal, $expr:expr) => {
            match $expr {
                Ok(value) => logi!("selftest {} ok: {:?}", $name, value),
                Err(error) => {
                    failures += 1;
                    loge!("selftest {} failed: {error:?}", $name);
                }
            }
        };
    }

    check!("getInterfaceVersion", proxy.get_interface_version());
    check!("getInterfaceHash", proxy.get_interface_hash());
    check!(
        "CeCmdGetSecuretype",
        proxy.cryptoeng_invoke_command(encode_native(54))
    );
    check!(
        "CeCmdWidevineSupport",
        proxy.cryptoeng_invoke_command(encode_native(59))
    );
    check!(
        "CeCmdCryptoSupport",
        proxy.cryptoeng_invoke_command(encode_native(60))
    );
    check!(
        "CeCmdCleanUp",
        proxy.cryptoeng_invoke_command(encode_native(53))
    );
    check!(
        "CeCmdEngineer",
        proxy.cryptoeng_invoke_command(encode_native(90))
    );
    check!(
        "CeCmdFindphoneGetStatus",
        proxy.cryptoeng_invoke_command(encode_native(18))
    );

    if failures == 0 {
        logi!("selftest: all checks passed");
        0
    } else {
        loge!("selftest: {failures} checks failed");
        1
    }
}

fn main() {
    logi!("cryptoeng AIDL HAL starting");

    let arg = std::env::args().nth(1);

    if arg.as_deref() == Some("--client") {
        let name = std::env::args()
            .nth(2)
            .unwrap_or_else(|| SERVICE_NAME.to_string());
        let payload = std::env::args().nth(3).map(|hex| decode_hex(&hex));
        let Some(binder) = binder::get_service(&name) else {
            loge!("client: service {name} not found");
            std::process::exit(1);
        };
        let proxy: binder::Strong<dyn ICryptoengTrait> = match binder.into_interface() {
            Ok(proxy) => proxy,
            Err(error) => {
                loge!("client: into_interface failed: {error:?}");
                std::process::exit(1);
            }
        };
        if let Some(payload) = payload {
            match proxy.cryptoeng_invoke_command(payload) {
                Ok(response) => {
                    println!("response[{}]: {}", response.len(), encode_hex(&response))
                }
                Err(error) => {
                    eprintln!("command failed: {error:?}");
                    std::process::exit(1);
                }
            }
            std::process::exit(0);
        }
        for code in [54i32, 59, 60, 53, 90, 18] {
            match proxy.cryptoeng_invoke_command(code.to_le_bytes().to_vec()) {
                Ok(response) => println!("code {} -> {:?}", code, response),
                Err(error) => eprintln!("code {} failed: {error:?}", code),
            }
        }
        std::process::exit(0);
    }

    if arg.as_deref() == Some("--selftest") {
        let Some(hal) = Hal::new().map(Arc::new) else {
            loge!("failed to open cryptoeng HAL");
            std::process::exit(1);
        };
        std::process::exit(run_selftest(hal));
    }

    let service_name = arg.unwrap_or_else(|| SERVICE_NAME.to_string());
    logi!("using service name {service_name}");

    let Some(hal) = Hal::new().map(Arc::new) else {
        loge!("failed to open cryptoeng HAL");
        std::process::exit(1);
    };

    let service = CryptoengService::new(hal).create_binder();

    binder::ProcessState::set_thread_pool_max_thread_count(4);
    binder::ProcessState::start_thread_pool();

    if let Err(error) = binder::add_service(&service_name, service.as_binder()) {
        loge!(
            "failed to register {service_name}: {error:?} (raw {})",
            error as i32
        );
        std::process::exit(1);
    }

    logi!("cryptoeng AIDL HAL ready");
    binder::ProcessState::join_thread_pool();
}

fn encode_native(code: i32) -> Vec<u8> {
    code.to_le_bytes().to_vec()
}

fn decode_hex(input: &str) -> Vec<u8> {
    let input = input.trim();
    (0..input.len())
        .step_by(2)
        .map(|index| u8::from_str_radix(&input[index..index + 2], 16).unwrap_or(0))
        .collect()
}

fn encode_hex(bytes: &[u8]) -> String {
    bytes.iter().map(|byte| format!("{:02x}", byte)).collect()
}
