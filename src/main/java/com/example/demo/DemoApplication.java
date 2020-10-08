package com.example.demo;

import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import javax.print.DocFlavor;
import java.util.function.Consumer;
import java.util.function.Function;

@SpringBootApplication
@EnableAsync
public class DemoApplication {


    @RestController
    public static class MyController {

        //        private WebClient client = WebClient.create();
        // todo: better for WebClient example
        private AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        static final String URL1 = "http://localhost:8081/service?req={req}";
        static final String URL2 = "http://localhost:8081/service2?req={req}";


        @GetMapping("/rest")

        public DeferredResult<String> rest(int idx) {
            final DeferredResult<String> dr = new DeferredResult<>();
            Completion.from(rt.getForEntity(URL1, String.class, "hello" + idx))
                    .andApply(s -> rt.getForEntity(URL2, String.class, s.getBody()))
                    .andAccept(s -> dr.setResult(s.getBody()));

            return dr;

        }
    }

    @Service
    public static class MyService {
        @Async
        public ListenableFuture<String> work(String req) {
            return new AsyncResult<>(req + "/async work");
        }
    }

    public static class Completion {
        private Completion next;
        private Consumer<ResponseEntity<String>> con;
        private Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn;

        public Completion() {
        }

        public Completion(Consumer<ResponseEntity<String>> con) {
            this.con = con;
        }

        public Completion(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn) {
            this.fn = fn;
        }


        public void andAccept(Consumer<ResponseEntity<String>> con) {
            Completion c = new Completion(con);
            this.next = c;
        }
        public Completion andApply(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn) {
            Completion c = new Completion(fn);
            this.next = c;

            return c;
        }

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
            if (next != null) next.run(s);
        }

        private void run(ResponseEntity<String> value) {
            if (con != null) con.accept(value);
            else if (fn != null) {
                final ListenableFuture<ResponseEntity<String>> lf = fn.apply(value);
                lf.addCallback(s -> complete(s), e -> error(e));
            }

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
