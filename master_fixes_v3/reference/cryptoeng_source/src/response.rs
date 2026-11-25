use crate::types::{MethodType, Parameter};

pub fn standard(method_type: MethodType) -> Vec<u8> {
    standard_with_params(method_type, 0, &[])
}

pub fn standard_with_params(
    method_type: MethodType,
    result_code: i32,
    params: &[Parameter],
) -> Vec<u8> {
    let mut response = Vec::new();
    response.extend_from_slice(&method_type.to_be_bytes());
    response.extend_from_slice(&result_code.to_be_bytes());
    response.extend_from_slice(&(params.len() as i32).to_be_bytes());
    for param in params {
        response.extend_from_slice(&param.to_be_bytes());
    }
    response
}

pub fn native(result_code: i32) -> Vec<u8> {
    result_code.to_be_bytes().to_vec()
}

pub fn failure(method_type: MethodType, result_code: Option<i32>) -> Vec<u8> {
    match result_code.filter(|_| !method_type.is_native()) {
        Some(result_code) => standard_with_params(method_type, result_code, &[]),
        None => native(-1),
    }
}
