package com.nazran.chat;

import com.nazran.chat.utils.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatApplication {

	public static void main(String[] args) {
        DotenvLoader.loadDotenv(); // Load .env file
        SpringApplication.run(ChatApplication.class, args);
	}

}
