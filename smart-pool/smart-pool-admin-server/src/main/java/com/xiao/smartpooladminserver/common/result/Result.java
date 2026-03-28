package com.xiao.smartpooladminserver.common.result;


import lombok.Data;

@Data
public class Result<T> {

	private int code;
	private String msg;
	private T data;


	private Result(int code, String msg, T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}
	// 成功：带数据
	public static <T> Result<T> success(T data) {
		return new Result<>(200, "success", data);
	}

	// 成功：无数据（例如 delete/update 操作）
	public static <T> Result<T> success() {
		return new Result<>(200, "success", null);
	}

	// 失败：自定义错误码和消息
	public static <T> Result<T> failure(int code, String msg) {
		return new Result<>(code, msg, null);
	}

	// 失败：使用默认错误码（如 500）
	public static <T> Result<T> failure(String msg) {
		return new Result<>(500, msg, null);
	}


}
