package com.github.lianjiatech.retrofit.spring.boot.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.lianjiatech.retrofit.spring.boot.test.entity.Person;
import com.github.lianjiatech.retrofit.spring.boot.test.entity.Result;
import com.github.lianjiatech.retrofit.spring.boot.test.http.HttpApi;
import com.github.lianjiatech.retrofit.spring.boot.test.http.HttpApi2;
import com.github.lianjiatech.retrofit.spring.boot.test.http.HttpApi3;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author 陈添明
 */
@SpringBootTest(classes = RetrofitTestApplication.class)
@RunWith(SpringRunner.class)
@Slf4j
public class RetrofitStarterTest {

    @Autowired
    private HttpApi httpApi;

    @Autowired
    private HttpApi2 httpApi2;

    @Autowired
    private HttpApi3 httpApi3;

    private static final ObjectMapper objectMapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private MockWebServer server;

    @Before
    public void before() throws IOException {
        System.out.println("=========开启MockWebServer===========");
        server = new MockWebServer();
        server.start(8080);

    }

    @After
    public void after() throws IOException {
        System.out.println("=========关闭MockWebServer===========");
        server.close();
    }

    @Test
    public void testGetPersonBody() throws Exception {

        // mock
        Person mockPerson = new Person().setId(1L)
                .setName("test")
                .setAge(10);
        Result mockResult = new Result<>()
                .setCode(0)
                .setMsg("ok")
                .setData(mockPerson);
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(mockResult));
        server.enqueue(response);

        Person person = new Person();
        person.setId(100L)
                .setAge(10)
                .setName("xx");
        Result<Person> personBody = httpApi2.getPersonBody(person);
        System.out.println(personBody);
    }

    @Test
    public void testAggregateLoggingInterceptor() {
        IntStream.range(1, 1000)
                .parallel()
                .forEach(i -> {
                    // mock
                    Person mockPerson = new Person().setId(1L)
                            .setName("test")
                            .setAge(10);
                    Result mockResult = new Result<>()
                            .setCode(0)
                            .setMsg("ok")
                            .setData(mockPerson);
                    MockResponse response = null;
                    try {
                        response = new MockResponse()
                                .setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8")
                                .addHeader("Cache-Control", "no-cache")
                                .setBody(objectMapper.writeValueAsString(mockResult));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    server.enqueue(response);

                    Person person = new Person();
                    person.setId(100L)
                            .setAge(10)
                            .setName("xx");
                    Result<Person> personBody = httpApi2.getPersonBody(person);
                    System.out.println(personBody);
                });
    }

    @Test
    public void testRetrofitConfigRef() throws IOException {

        // mock
        Person mockPerson = new Person().setId(1L)
                .setName("test")
                .setAge(10);
        Result mockResult = new Result<>()
                .setCode(0)
                .setMsg("ok")
                .setData(mockPerson);
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(mockResult));
        server.enqueue(response);
        server.enqueue(response);

        // http check
        Result<Person> person = httpApi.getPerson(1L);
        Person data = person.getData();
        Assert.assertNotNull(data);
        Assert.assertEquals("test", data.getName());
        Assert.assertEquals(10, data.getAge().intValue());
        Result<Person> person2 = httpApi2.getPerson(1L);
        Person data2 = person2.getData();
        Assert.assertNotNull(data2);
        Assert.assertEquals("test", data2.getName());
        Assert.assertEquals(10, data2.getAge().intValue());
    }

    @Test
    public void testRetCall() throws InterruptedException, IOException {
        // mock
        Person mockPerson = new Person().setId(2L)
                .setName("test")
                .setAge(10);
        Result mockResult = new Result<>()
                .setCode(0)
                .setMsg("ok")
                .setData(mockPerson);
        MockResponse response = new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(mockResult));
        server.enqueue(response);

        Call<Result<Person>> resultCall = httpApi.getPersonCall(2L);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        // 异步回调处理
        resultCall.enqueue(new Callback<Result<Person>>() {
            @Override
            public void onResponse(Call<Result<Person>> call, Response<Result<Person>> response) {
                try {
                    if (response.isSuccessful()) {
                        Result<Person> personResult = response.body();
                        Assert.assertEquals(0, personResult.getCode());
                        Assert.assertNotNull(personResult.getData());
                        Person data = personResult.getData();
                        Assert.assertEquals(10, data.getAge().longValue());
                        Assert.assertEquals("test", data.getName());
                    } else {
                        ResponseBody errorBody = response.errorBody();
                        System.out.println(errorBody);
                    }

                } finally {
                    countDownLatch.countDown();
                }
            }

            @Override
            public void onFailure(Call<Result<Person>> call, Throwable t) {
                Request request = call.request();
                log.error("请求执行失败! request = {}", request, t);
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
    }

    @Test
    public void testFuture() throws ExecutionException, InterruptedException, IOException {

        // mock
        Person mockPerson = new Person().setId(1L)
                .setName("test")
                .setAge(10);
        Result mockResult = new Result<>()
                .setCode(0)
                .setMsg("ok")
                .setData(mockPerson);
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(mockResult));
        server.enqueue(response);

        CompletableFuture<Result<Person>> resultCompletableFuture = httpApi.getPersonCompletableFuture(1L);
        // CompletableFuture处理
        Result<Person> personResult = resultCompletableFuture.get();
        Assert.assertEquals(0, personResult.getCode());
        Assert.assertNotNull(personResult.getData());
        Person data = personResult.getData();
        Assert.assertEquals(10, data.getAge().longValue());
        Assert.assertEquals("test", data.getName());
    }

    @Test
    public void testResponse() throws IOException {
        // mock
        Person mockPerson = new Person().setId(1L)
                .setName("test")
                .setAge(10);
        Result mockResult = new Result<>()
                .setCode(0)
                .setMsg("ok")
                .setData(mockPerson);
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(mockResult));
        server.enqueue(response);

        Response<Result<Person>> resultResponse = httpApi.getPersonResponse(1L);
        Assert.assertTrue(resultResponse.isSuccessful());
        Result<Person> personResult = resultResponse.body();
        // 直接取值
        Assert.assertEquals(0, personResult.getCode());
        Assert.assertNotNull(personResult.getData());
        Person data = personResult.getData();
        Assert.assertEquals(10, data.getAge().longValue());
        Assert.assertEquals("test", data.getName());
    }

    @Test
    public void testApi2() throws IOException {
        // mock
        Person mockPerson = new Person().setId(1L)
                .setName("test")
                .setAge(10);
        Result mockResult = new Result<>()
                .setCode(0)
                .setMsg("ok")
                .setData(mockPerson);
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(mockResult));
        server.enqueue(response);

        Result<Person> person = httpApi2.getPerson(1L);
        Person data = person.getData();
        Assert.assertNotNull(data);
        Assert.assertEquals("test", data.getName());
        Assert.assertEquals(10, data.getAge().intValue());
    }

    @Test
    public void savePerson() throws IOException {

        // mock
        Result mockResult = new Result<>()
                .setCode(0)
                .setMsg("ok");

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(mockResult));
        server.enqueue(response);

        Person person = new Person().setId(1L).setName("test").setAge(10);
        Result<Void> voidResult = httpApi.savePerson(person);
        int code = voidResult.getCode();
        Assert.assertEquals(code, 0);
    }

    @Test
    public void savePersonVoid() throws IOException {
        // mock

        Result mockResult = new Result<>()
                .setCode(0)
                .setMsg("ok");

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(mockResult));
        server.enqueue(response);

        Person person = new Person().setId(1L).setName("test").setAge(10);
        httpApi.savePersonVoid(person);
    }

    @Test
    public void testNoBaseUrl() throws IOException {
        // mock
        Person mockPerson = new Person().setId(1L)
                .setName("test")
                .setAge(10);
        Result mockResult = new Result<>()
                .setCode(0)
                .setMsg("ok")
                .setData(mockPerson);
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(mockResult));
        server.enqueue(response);

        String url = "http://localhost:8080/api/test/person";
        Result<Person> person = httpApi3.getPerson(url, 1L);
        Person data = person.getData();
        Assert.assertNotNull(data);
        Assert.assertEquals("test", data.getName());
        Assert.assertEquals(10, data.getAge().intValue());

    }

    @Test
    public void testMap() throws IOException {

        Map<String, Map<String, String>> map = new HashMap<>(4);
        Map<String, String> test = new HashMap<>(4);
        test.put("a", "aa");
        test.put("b", "bb");
        map.put("test", test);

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(map));
        server.enqueue(response);

        Map<String, Map<String, String>> stringMapMap = httpApi2.testMap();
        System.out.println(stringMapMap);
    }

    @Test
    public void savePersonList() throws IOException {

        // mock
        Result mockResult = new Result<>()
                .setCode(0)
                .setMsg("ok");

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(mockResult));
        server.enqueue(response);

        Person person = new Person().setId(1L).setName("test").setAge(10);
        Person person2 = new Person().setId(10L).setName("test333").setAge(100);

        List<Person> list = new ArrayList<>();
        list.add(person);
        list.add(person2);

        Result<Void> voidResult = httpApi.savePersonList(list);
        int code = voidResult.getCode();
        Assert.assertEquals(code, 0);
    }

    @Test
    public void testString() {

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/text; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody("text");
        server.enqueue(response);
        Person mockPerson = new Person().setId(1L)
                .setName("testString")
                .setAge(10);
        String string = httpApi.getString(mockPerson);
        System.out.println(string);
    }

    @Test
    public void testBoolean() {

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/text; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody("false");
        server.enqueue(response);

        Person mockPerson = new Person().setId(1L)
                .setName("testBoolean")
                .setAge(10);
        Boolean apiBoolean = httpApi.getBoolean(mockPerson);
        System.out.println(apiBoolean);
    }

}
