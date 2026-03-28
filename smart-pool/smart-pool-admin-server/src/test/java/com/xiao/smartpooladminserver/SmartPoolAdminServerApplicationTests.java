package com.xiao.smartpooladminserver;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class SmartPoolAdminServerApplicationTests {

	@Autowired
	public ThreadPoolRegistry registry;



}
