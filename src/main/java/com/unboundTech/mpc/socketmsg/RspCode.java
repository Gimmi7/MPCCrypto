package com.unboundTech.mpc.socketmsg;

public enum RspCode {

    unknown_exception(601, "unknown_exception"),
    parameter_invalid(602, "parameter_invalid"),
    load_share_fail(2201, "load_share_fail"),
    init_refresh_fail(2202, "init_refresh_fail"),
    init_sign_fail(2203, "init_sign_fail"),
    init_generate_fail(2204, "init_generate_fail"),
    init_derive_fail(2205, "init_derive_fail"),
    ;

    public int errCode;
    public String errMsg;

    RspCode(int errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }
}
