use std::os::raw::c_char;

extern "C" {
    pub fn __android_log_print(prio: i32, tag: *const c_char, fmt: *const c_char, ...) -> i32;
}

#[macro_export]
macro_rules! logi {
    ($($arg:tt)*) => {
        unsafe {
            let msg = format!($($arg)*);
            let c_msg = std::ffi::CString::new(msg.replace('\0', " ")).unwrap();
            $crate::log::__android_log_print(3, c"CRYPTOENG".as_ptr(), c_msg.as_ptr());
        }
    };
}

#[macro_export]
macro_rules! logw {
    ($($arg:tt)*) => {
        unsafe {
            let msg = format!($($arg)*);
            let c_msg = std::ffi::CString::new(msg.replace('\0', " ")).unwrap();
            $crate::log::__android_log_print(4, c"CRYPTOENG".as_ptr(), c_msg.as_ptr());
        }
    };
}

#[macro_export]
macro_rules! loge {
    ($($arg:tt)*) => {
        unsafe {
            let msg = format!($($arg)*);
            let c_msg = std::ffi::CString::new(msg.replace('\0', " ")).unwrap();
            $crate::log::__android_log_print(6, c"CRYPTOENG".as_ptr(), c_msg.as_ptr());
        }
    };
}
