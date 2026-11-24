use std::collections::HashMap;

use crate::types::{MethodParamType, MethodType, Parameter};
use crate::{CryptoengError, Result};

pub const MAX_REQUEST_SIZE: usize = 0x5800;

const HEADER_SIZE: usize = 12;

#[derive(Debug, Clone, Copy)]
pub struct Native;

#[derive(Debug, Clone, Copy)]
pub struct Standard;

pub trait CryptoengRequest: std::fmt::Debug {
    fn method_type(&self) -> MethodType;
    fn get_param(&self, param_type: MethodParamType) -> Result<Parameter>;
}

#[derive(Debug, Clone)]
pub struct Request<T> {
    method_type: MethodType,
    params: HashMap<MethodParamType, Vec<u8>>,
    _marker: std::marker::PhantomData<T>,
}

impl CryptoengRequest for Request<Native> {
    fn method_type(&self) -> MethodType {
        self.method_type
    }

    fn get_param(&self, param_type: MethodParamType) -> Result<Parameter> {
        Err(CryptoengError::MissingParameter(param_type))
    }
}

impl CryptoengRequest for Request<Standard> {
    fn method_type(&self) -> MethodType {
        self.method_type
    }

    fn get_param(&self, param_type: MethodParamType) -> Result<Parameter> {
        let bytes = self
            .params
            .get(&param_type)
            .filter(|bytes| !bytes.is_empty())
            .ok_or(CryptoengError::MissingParameter(param_type))?;
        Parameter::new(param_type, bytes.clone())
    }
}

pub fn parse(data: &[u8]) -> Result<Box<dyn CryptoengRequest>> {
    if data.len() > MAX_REQUEST_SIZE {
        return Err(CryptoengError::ParseError(
            "request exceeds backend buffer limit".into(),
        ));
    }
    if data.len() < 4 {
        return Err(CryptoengError::BufferTooShort {
            got: data.len(),
            min: 4,
        });
    }

    let method_type = detect_method_type(data);
    if method_type.code() < 0 {
        return Err(CryptoengError::ParseError("negative command code".into()));
    }
    if method_type.is_native() {
        Ok(Box::new(Request::<Native>::parse(data)?))
    } else {
        Ok(Box::new(Request::<Standard>::parse(data)?))
    }
}

fn detect_method_type(data: &[u8]) -> MethodType {
    let little_endian = i32::from_le_bytes([data[0], data[1], data[2], data[3]]);
    if (0..=100).contains(&little_endian) {
        MethodType::from(little_endian)
    } else {
        MethodType::from(i32::from_be_bytes([data[0], data[1], data[2], data[3]]))
    }
}

impl Request<Native> {
    fn parse(data: &[u8]) -> Result<Self> {
        Ok(Self {
            method_type: detect_method_type(data),
            params: HashMap::new(),
            _marker: std::marker::PhantomData,
        })
    }
}

impl Request<Standard> {
    fn parse(buffer: &[u8]) -> Result<Self> {
        if buffer.len() < HEADER_SIZE {
            return Err(CryptoengError::BufferTooShort {
                got: buffer.len(),
                min: HEADER_SIZE,
            });
        }

        let method_type = MethodType::from(i32::from_be_bytes([
            buffer[0], buffer[1], buffer[2], buffer[3],
        ]));
        if (4000..=4012).contains(&method_type.code()) && buffer.len() > 0x1000 {
            return Err(CryptoengError::ParseError(
                "PMS command exceeds TA limit".into(),
            ));
        }

        let param_count = i32::from_be_bytes([buffer[8], buffer[9], buffer[10], buffer[11]]);
        if param_count < 0 {
            return Err(CryptoengError::ParseError(
                "negative parameter count".into(),
            ));
        }
        let param_count = param_count as usize;
        if param_count > (buffer.len() - HEADER_SIZE) / 8 {
            return Err(CryptoengError::ParseError(
                "parameter count exceeds buffer".into(),
            ));
        }

        let mut params = HashMap::with_capacity(param_count);
        let mut remaining = &buffer[HEADER_SIZE..];
        for _ in 0..param_count {
            if remaining.len() < 8 {
                return Err(CryptoengError::BufferTooShort {
                    got: remaining.len(),
                    min: 8,
                });
            }
            let param_type = MethodParamType::from(i32::from_be_bytes([
                remaining[0],
                remaining[1],
                remaining[2],
                remaining[3],
            ]));
            let param_len =
                i32::from_be_bytes([remaining[4], remaining[5], remaining[6], remaining[7]]);
            if param_len < 0 {
                return Err(CryptoengError::ParseError(
                    "negative parameter length".into(),
                ));
            }
            let param_len = param_len as usize;
            remaining = &remaining[8..];
            if param_len > remaining.len() {
                return Err(CryptoengError::BufferTooShort {
                    got: remaining.len(),
                    min: param_len,
                });
            }
            if !matches!(param_type, MethodParamType::Unknown(_)) {
                params
                    .entry(param_type)
                    .or_insert_with(|| remaining[..param_len].to_vec());
            }
            remaining = &remaining[param_len..];
        }

        Ok(Self {
            method_type,
            params,
            _marker: std::marker::PhantomData,
        })
    }
}
