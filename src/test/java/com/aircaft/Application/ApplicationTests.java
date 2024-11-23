package com.aircaft.Application;

import com.aircaft.Application.mapper.UserMapper;
import com.aircaft.Application.pojo.entity.Player;
import com.aircaft.Application.pojo.vo.BackPackPropsVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

//@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class ApplicationTests {
    @Autowired
    UserMapper userMapper;

    @Test
    void contextLoads() {
        log.info("list : {}", userMapper.getPlayerChatMessage(1, 2));

    }

}
