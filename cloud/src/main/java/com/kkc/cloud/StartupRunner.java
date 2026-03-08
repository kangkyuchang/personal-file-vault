package com.kkc.cloud;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {

	@Autowired
	private CloudConfig cloudConfig;
	
	@Override
	public void run(String... args) throws Exception {
		File directory = new File(cloudConfig.getStorePath());
		if(!directory.exists()) {
			directory.mkdirs();
		}
		System.out.print(cloudConfig.getStorePath());
	}
}
