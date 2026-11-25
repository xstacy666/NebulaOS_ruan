# cryptoeng HAL

A Rust implementation of the `vendor.oplus.hardware.cryptoeng.ICryptoeng` AIDL service for oplus ports. It implements the privacy password (PMS) commands that App Lock and the privacy password screen call, and keeps the remaining native commands as compatibility stubs.

## Status

Implemented:

- PMS privacy password: enroll, verify, modify, delete, get info, reset all data.
- AES-256-GCM encrypted persistence for privacy credentials.
- Native command stubs so callers that probe the HAL get a response.

## AIDL surface

```aidl
package vendor.oplus.hardware.cryptoeng;

@VintfStability
interface ICryptoeng {
    byte[] cryptoeng_invoke_command(in byte[] request);
}
```

| Item | Value |
|---|---|
| Service name | `vendor.oplus.hardware.cryptoeng.ICryptoeng/default` |
| Interface version | `1` |
| Interface hash | `765136eb397eb5b85ee1e089d8fec72c15e266b3` |
| Binder stability | `Local` by default, `Vintf` with the `vintf_stability` feature |

The AIDL file declares `@VintfStability`, but the default build registers a local stability service so it can run from `/data/local/tmp` without a VINTF manifest. A VINTF build needs a manifest entry and a matching SELinux policy. Registering a service as vendor stability does not work for system-context clients; they fail with a stability context error.

## Message framing

Every request and response is an opaque byte buffer. The first four bytes decide the format.

### Native commands

If the little-endian value of the first four bytes is between 0 and 100, the buffer is a native command. The rest of the buffer is command-specific. Most native commands in this implementation ignore it.

```
36 00 00 00        command 54, little-endian
```

Responses are raw bytes. There is no header and no result code.

## Structure

| Path | Functions |
|---|---|
| `aidl/vendor/oplus/hardware/cryptoeng/ICryptoeng.aidl` | Interface definition |
| `src/main.rs` | Server startup, self test, raw client |
| `src/service.rs` | Binder service implementation |
| `src/hal.rs` | Command dispatch, native stubs, PMS wiring |
| `src/request.rs` | Native and standard request parser |
| `src/response.rs` | Standard and native response builders |
| `src/pms.rs` | Enroll, verify, modify, delete, get info, reset |
| `src/storage.rs` | Encrypted store, retry rules, boot clock |
| `src/error.rs` | Error types and client result codes |
| `src/log.rs` | `__android_log_print` macros |
| `src/types/method.rs` | Command ids |
| `src/types/parameter.rs` | Parameter ids and typed values |
| `src/gen/vendor/oplus/hardware/cryptoeng/ICryptoeng.rs` | Hand-written binder bindings |
| `vendor/binder_ndk`, `vendor/binder_ndk_sys` | Patched binder crates |

### Standard commands

Anything else is a standard command. The header is 12 bytes, big-endian:

| Offset | Size | Field |
|---|---|---|
| 0 | 4 | Method id |
| 4 | 4 | Protocol marker. Clients send `10000`. The parser accepts any value. |
| 8 | 4 | Parameter count |

Each parameter follows the header:

| Offset | Size | Field |
|---|---|---|
| 0 | 4 | Parameter type code |
| 4 | 4 | Payload length |
| 8 | N | Payload |

Parser limits and rules:

| Rule | Behavior |
|---|---|
| Maximum request | `0x5800` bytes |
| Minimum standard request | 12 bytes |
| PMS request limit | Requests above `0x1000` bytes for method codes 4000 to 4012 are rejected |
| Negative lengths or counts | Rejected as a parse error |
| Parameter count | Must not exceed the remaining buffer divided by 8 |
| Unknown parameter types | Skipped |
| Duplicate parameter types | First value wins |
| Zero-length parameter value | Treated as missing by `get_param` |

A request rejected by this parser gets an empty response and a `parse error` log line. The `10014` result code is used later, when a handler rejects a parameter value.

### Standard response

| Offset | Size | Field |
|---|---|---|
| 0 | 4 | Method id |
| 4 | 4 | Result code |
| 8 | 4 | Parameter count |

Parameters use the same type and length encoding as requests.

When an error has a mapped result code, the response is a standard header with that code and zero parameters. When an error has no mapped code, for example not enrolled, clock unavailable, or a storage failure, the response is the four-byte native failure value `ff ff ff ff`. Native commands always receive that four-byte value, even for errors that have a standard result code.

## Command dispatch

### PMS commands

| Code | Name | Handler |
|---|---|---|
| 4001 | `CeCmdPmsEnroll` | Create a privacy password record |
| 4002 | `CeCmdPmsVerify` | Check a credential and issue a challenge |
| 4003 | `CeCmdPmsModify` | Change password, security question, or email |
| 4004 | `CeCmdPmsDelete` | Remove the record |
| 4005 | `CeCmdPmsGetInfo` | Report enrollment state, type, handler, retries, lock time |
| 4006 | `CeCmdPmsRequestEncryptData` | Returns unsupported |
| 4007 | `CeCmdPmsResetAllData` | Removes both system type records |

### Native command stubs

`hal.rs` holds the exact response for each code. The groups:

| Codes | Behavior |
|---|---|
| 3, 4, 24, 25, 26, 30, 51, 52, 90 | Empty response |
| 8 | Empty response. Secrets are not persisted. |
| 18, 53 | Single zero byte |
| 54, 59, 60, 2018, 2021, 10009, 13003, 13005, 14009 | Single byte `1` |
| 2014, 10001 to 10008, 10010 to 10020, 13002 | Empty response |
| 2015, 2019, 2020, 13001, 13004, 14001 to 14007 | Single zero byte |
| 2022, 14008 | 16 zero bytes |
| 5011 | Single zero byte |
| 5012, 6015, 6016 | Empty response |
| 6001 | `01 00 00 00` |
| 6002 | Single byte `1` |
| 6003 | 16 zero bytes |

## PMS parameters

| Code | Name | Format |
|---|---|---|
| 201 | `PmsTypeT` | 4-byte integer. `1` privacy password, `2` screen lock |
| 202 | `PmsPwdInfoT` | 32-byte password hash |
| 203 | `PmsSecureInfoBufferT` | One or more 40-byte recovery records |
| 204 | `PmsVerifyTypeT` | 4-byte integer. `1` password, `2` security question |
| 205 | `PmsVerifyBufferT` | 32-byte proof |
| 206 | `PmsChallengeT` | 64-byte challenge |
| 207 | `PmsInfoTypeT` | 4-byte integer. `1` password, `2` security question, `4` email |
| 208 | `PmsInfoBufferT` | New value for a modify request |
| 209 | `PmsHandlerT` | 8-byte handler |
| 210 | `PmsPwdTypeT` | 4-byte password quality |
| 211 | `PmsPwdRetryCountLeftT` | 4-byte retry count |
| 212 | `PmsPwdLockTimeLeftT` | 8 bytes. Seconds in the first 4 bytes, zero padding after. |
| 217 | `PmsHideEmailT` | Request flag, or masked email in a response |
| 219 | `PmsSecureQuestionRetryCountLeftT` | 4-byte retry count |
| 220 | `PmsSecureQuestionLockTimeLeftT` | 8 bytes, same layout as 212 |
| 221 | `PmsLatestSecureRulesT` | 4-byte marker. `10000` selects the newer rule set. |

Password quality values: `0` unspecified, `1` four-digit PIN, `2` six-digit PIN, `3` numeric complex, `4` complex, `5` pattern.

A recovery record is 40 bytes:

| Offset | Size | Field |
|---|---|---|
| 0 | 4 | Info type. `2` security question, `4` email, `10` email continuation. |
| 4 | 4 | Declared payload length. Must be 32 or less. |
| 8 | 32 | Payload, zero padded |

The full parameter enum is in `src/types/parameter.rs`.

## Enroll (4001)

Required parameters: `PmsTypeT`, `PmsPwdTypeT`, `PmsPwdInfoT`, `PmsSecureInfoBufferT`.

Validation:

1. System type must be `1`.
2. Password quality must not be `0`.
3. Password hash must be exactly 32 bytes.
4. Recovery buffer must contain 1 to 6 records, include at least one security question, have no type `0` entry, and have no zero declared length.
5. An existing enrolled record is rejected with result code `10031`.

On success the service stores the password type, hash, and recovery records, generates a random 8-byte handler, resets retries, saves the store, and returns `PmsHandlerT`.

## GetInfo (4005)

Required parameter: `PmsTypeT`.

When no record is enrolled, the response contains only `PmsPwdTypeT` with value `0`.

When enrolled, the response contains:

1. `PmsPwdTypeT`
2. `PmsHandlerT`
3. `PmsHideEmailT` with the email, masked unless the request asks for the full value, if the records contain an email
4. `PmsPwdLockTimeLeftT` when the caller does not use the latest rules, or when a password lock is active. With the legacy rules and no active lock, the value is zero.
5. `PmsSecureQuestionLockTimeLeftT` when the latest rules are used and a question lock is active
6. `PmsPwdRetryCountLeftT`
7. `PmsSecureQuestionRetryCountLeftT` when the latest rules are used

The caller selects the newer rule set by sending `PmsLatestSecureRulesT` with value `10000`. Otherwise the legacy rule set applies.

Email behavior: the service hides the email unless the request includes `PmsHideEmailT` with value `0`. The masking keeps the domain and replaces characters from `local_length / 4 + 1` up to the `@` with `*`. A one-character local part produces an empty address. An invalid address produces result code `10001`.

GetInfo does not write to storage. Retry counts and lock times are computed from the stored failure history.

## Verify (4002)

Required parameters: `PmsTypeT`, `PmsVerifyTypeT`, `PmsVerifyBufferT`.

Supported verification types are password (`1`) and security question (`2`). The proof must be exactly 32 bytes.

For a password, the client sends:

```
SHA-256(password_hash || handler)
```

`password_hash` is the same 32-byte value supplied at enrollment. `handler` is the 8-byte value returned by enroll or by a password modify. Both parts are concatenated as raw bytes before hashing.

For a security question, the proof is compared against every stored security question record using constant-time equality. The record payload is 32 bytes, zero padded.

On success the service generates a 64-byte challenge, resets both retry counters, stores the challenge with its issue time, saves the store, and returns `PmsChallengeT`. Delete uses the same challenge and removes it with the record. Modify does not clear the challenge, so it remains valid until it expires.

On failure the service increments the retry counter, saves the store, and returns result code `10017`.

If no record is enrolled, the response is the four-byte native failure value.

## Modify (4003)

Required parameters: `PmsTypeT`, `PmsChallengeT`, `PmsPwdTypeT`, `PmsInfoTypeT`, `PmsInfoBufferT`.

The challenge must be exactly 64 bytes and must match the challenge from the last successful verify. It is valid for 300 seconds. A reboot also invalidates it because expiry is measured with the boot clock and boot id.

`PmsPwdTypeT` is required for every modify type and must not be `0`.

| Info type | Requirement | Effect |
|---|---|---|
| `1` password | Exactly 32 bytes | Replaces the hash, generates a new 8-byte handler, returns it as `PmsHandlerT` |
| `2` security question | 1 to 32 bytes | Replaces or adds the security question record |
| `4` email | 1 to 64 bytes | Replaces the email records, including the continuation record |

Every successful modify resets both retry counters and saves the store. A successful modify leaves the authorization challenge in place, so the same challenge can be reused until it expires. An email update fails with `10027` when all six recovery slots are taken and no email slot can be reused. A security question update with no free slot fails with `10014`.

## Delete (4004)

Required parameters: `PmsTypeT`, `PmsChallengeT`.

The challenge must match a challenge from a successful verify and be younger than 60 seconds. The service removes the record for the system type and saves the store.

## ResetAllData (4007)

No parameters are required. The service removes the privacy password and screen lock records and saves the store. It does not check a challenge.

## Result codes

| Code | Meaning |
|---|---|
| 10001 | Invalid email |
| 10012 | Unsupported operation |
| 10014 | Malformed parameter value |
| 10017 | Verification failed |
| 10027 | Recovery slots full |
| 10031 | Already enrolled |
| 10033 | Challenge mismatch |
| 10034 | Challenge expired |
| 10100 + parameter code | Missing parameter, for example `10301` for a missing `PmsTypeT` |
| none | Response is `ff ff ff ff` |

## Storage

The service stores data in `/data/vendor_de/0/cryptoeng`.

| File | Contents |
|---|---|
| `cryptoeng.key` | 32 random bytes, mode `0600` |
| `cryptoeng.dat` | Encrypted JSON store, mode `0600` |

The data file layout:

| Offset | Size | Field |
|---|---|---|
| 0 | 8 | Header `CENG\0\0\0\1` |
| 8 | 12 | AES-GCM nonce |
| 20 | N | Ciphertext |
| 20 + N | 16 | Authentication tag |

The key file is created once with an exclusive create. The directory is created with mode `0770` if missing. Saves write a temporary file, call `fsync`, and rename it over the data file.

The JSON store maps a system type to a record:

- password type
- password hash
- recovery records
- email
- handler
- retry counters and recent failure timestamps
- pending challenge and its issue time

The service never receives or stores the plaintext password. It receives the client's hash.

The key sits in the same directory as the data file. Encryption protects the data file when it is copied without the key file, but the access boundary is the file permission and SELinux, not hardware-backed key storage.

Save points: enroll, verify success, verify failure, modify, delete, and reset all data. GetInfo never writes.

### Retry and lock rules

The newer rule set, selected with `PmsLatestSecureRulesT = 10000`:

| Failures | Retries left | Lock after the failure |
|---|---|---|
| 0 to 4 | 5 minus failures | None |
| 5 | 5 when unlocked, 0 when locked | 60 seconds |
| 6 to 9 | 10 minus failures | None |
| 10 | 5 when unlocked, 0 when locked | 600 seconds |
| 11 to 14 | `failures XOR 15` | None |
| 15 | 5 when unlocked, 0 when locked | 1800 seconds |
| 16 to 19 | 20 minus failures | None |
| 20 and above | 0 | 3600 seconds |

The legacy rule set uses `max(0, 5 - failures)` retries and a 30 second lock after every fifth failure.

A lock time is reported as the remaining milliseconds divided by 1000, plus one.

### Clock

The service reads `/proc/sys/kernel/random/boot_id` and uses `CLOCK_BOOTTIME` for timestamps. Challenge expiry compares the boot id first. A different boot id makes the challenge invalid. A failure timestamp from a previous boot yields no elapsed time, so any lock time is measured from boot and counts down as uptime increases.

### Concurrency

One `Mutex` guards the store and runtime clock. The binder thread pool has four threads, so PMS requests are serialized. Start a single service instance; the store has no cross-process lock.

## Build

Prerequisites:

- Rust with the `aarch64-linux-android` target.
- Android NDK r29. `.cargo/config` points to `/opt/android-sdk/ndk/29.0.14206865`. Change those paths if the NDK is installed elsewhere.
- The NDK clang wrapper for API 35.

```sh
export NDK_HOME=/opt/android-sdk/ndk/29.0.14206865
export ANDROID_NDK_HOME="$NDK_HOME"
export PATH="$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH"

cargo build --release
```

Breakdown:

| Bytes | Meaning |
|---|---|
| `00000fa5` | Method 4005 |
| `00002710` | Protocol marker 10000 |
| `00000002` | Two parameters |
| `000000c9 00000004 00000001` | `PmsTypeT` = 1 |
| `000000dd 00000004 0000002710` | `PmsLatestSecureRulesT` = 10000 |

Response when nothing is enrolled:

```
00000fa5 00000000 00000001 000000d2 00000004 00000000
```

Method 4005, result 0, one parameter, `PmsPwdTypeT` = 0.

## Troubleshooting

| Symptom | Check |
|---|---|
| `service not found` in the client | Service process is running, and the client uses the same stability context. |
| `PMS storage unavailable` in logcat | Process is not uid 1000, or `/data/vendor_de/0/cryptoeng` is missing or not writable. |
| `VINTF declaration error` at startup | VINTF build without a manifest entry. Use the default local build or install the manifest. |
| `Cannot do a user transaction on a vendor stability binder` | Service was registered as vendor stability. System apps cannot call it. Use local or system stability. |
| Empty response from a command | The method has no handler, or the header parser rejected the request. Check logcat for an `unhandled method` or `parse error` line. |
| Result code `10033` | The challenge does not match. Run verify again. |
| Result code `10034` | The challenge is older than 300 seconds for modify or 60 seconds for delete, or the device rebooted. |
| GetInfo returns `PmsPwdTypeT = 0` after a service change | The new service is using a different store. Re-enroll the credential. |



## License

Apache-2.0
