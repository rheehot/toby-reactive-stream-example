package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@SpringBootApplication
@EnableAsync
public class DemoApplication {

    @Service
    public static class MyService {
        @Async
        public ListenableFuture<String> work(String req) {
            return new AsyncResult<>(req + "/async work");
        }
    }

    public static class Completion {
        public static Completion from(ListenableFuture<ResponseEntity<String>> lf) {
            final Completion completion = new Completion();
            lf.addCallback(
                    s -> {
                        completion.complete(s);
                    },
                    e -> {
                        completion.error(e);
                    });

            return completion;

        }

        private void error(Throwable e) {

        }

        private void complete(ResponseEntity<String> s) {

        }
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    ThreadPoolTaskExecutor myThreadPool() {
        final ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(1);
        te.setMaxPoolSize(1);
        te.initialize();

        return te;
    }

}
