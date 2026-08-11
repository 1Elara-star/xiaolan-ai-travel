package com.lanyu.xiaolanaitravel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 不连接数据库的基础工程测试。 */
class XiaolanAiTravelApplicationTests {
    @Test
    void applicationMainClassExists() {
        assertThat(XiaolanAiTravelApplication.class).isNotNull();
    }
}
