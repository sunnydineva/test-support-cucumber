package com.shop.testsupport.cucumber.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Stubber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringBeanMockSteps {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    @Given("Spring mock bean {string} method {string} returns:")
    public void springMockBeanMethodReturns(String beanName, String methodName, String responseBody) throws Exception {
        Object bean = mockBean(beanName);
        Method method = singleMethod(bean, methodName);
        Object returnValue = deserialize(responseBody, method.getGenericReturnType());

        Stubber stubber = Mockito.doReturn(returnValue);
        invokeForStubbing(stubber.when(bean), method, anyArguments(method));
    }

    @Given("Spring mock bean {string} method {string} returns for JSON arguments:")
    public void springMockBeanMethodReturnsForJsonArguments(String beanName, String methodName, String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode argumentsNode = required(root, "arguments");
        JsonNode returnsNode = required(root, "returns");

        Object bean = mockBean(beanName);
        Method method = singleMethod(bean, methodName, argumentsNode.size());
        Object[] arguments = exactArguments(method, argumentsNode);
        Object returnValue = deserialize(returnsNode.toString(), method.getGenericReturnType());

        Stubber stubber = Mockito.doReturn(returnValue);
        invokeForStubbing(stubber.when(bean), method, arguments);
    }

    @Given("Spring mock bean {string} method {string} throws {string} with message {string}")
    public void springMockBeanMethodThrows(String beanName, String methodName, String exceptionClassName, String message) throws Exception {
        Object bean = mockBean(beanName);
        Method method = singleMethod(bean, methodName);
        RuntimeException exception = createRuntimeException(exceptionClassName, message);

        Stubber stubber = Mockito.doThrow(exception);
        invokeForStubbing(stubber.when(bean), method, anyArguments(method));
    }

    @Then("Spring mock bean {string} method {string} should have been called")
    public void springMockBeanMethodShouldHaveBeenCalled(String beanName, String methodName) throws Exception {
        Object bean = mockBean(beanName);
        Method method = singleMethod(bean, methodName);
        invokeForVerification(Mockito.verify(bean), method, anyArguments(method));
    }

    @Then("Spring mock bean {string} method {string} should have been called {int} times")
    public void springMockBeanMethodShouldHaveBeenCalledTimes(String beanName, String methodName, int times) throws Exception {
        Object bean = mockBean(beanName);
        Method method = singleMethod(bean, methodName);
        invokeForVerification(Mockito.verify(bean, Mockito.times(times)), method, anyArguments(method));
    }

    @Then("Spring mock bean {string} method {string} should have been called with JSON arguments:")
    public void springMockBeanMethodShouldHaveBeenCalledWithJsonArguments(String beanName, String methodName, String argumentsJson) throws Exception {
        JsonNode argumentsNode = objectMapper.readTree(argumentsJson);
        if (!argumentsNode.isArray()) {
            throw new IllegalArgumentException("Expected JSON array with method arguments");
        }

        Object bean = mockBean(beanName);
        Method method = singleMethod(bean, methodName, argumentsNode.size());
        invokeForVerification(Mockito.verify(bean), method, exactArguments(method, argumentsNode));
    }

    private Object mockBean(String beanName) {
        requireSpringContext();
        Object bean = applicationContext.getBean(beanName);
        assertThat(Mockito.mockingDetails(bean).isMock())
                .as("Spring bean '%s' must be a Mockito mock. Declare it as @MockBean in the service test.", beanName)
                .isTrue();
        return bean;
    }

    private void requireSpringContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("Spring ApplicationContext is not available. Use cucumber-spring with @CucumberContextConfiguration and @SpringBootTest.");
        }
    }

    private Method singleMethod(Object bean, String methodName) {
        List<Method> methods = methodsNamed(bean, methodName);
        if (methods.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one method named '%s' on bean '%s', found %d. Use the JSON arguments step for overloaded methods."
                    .formatted(methodName, bean.getClass().getName(), methods.size()));
        }
        return methods.get(0);
    }

    private Method singleMethod(Object bean, String methodName, int argumentCount) {
        List<Method> matches = methodsNamed(bean, methodName).stream()
                .filter(method -> method.getParameterCount() == argumentCount)
                .toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one method named '%s' with %d arguments on bean '%s', found %d."
                    .formatted(methodName, argumentCount, bean.getClass().getName(), matches.size()));
        }
        return matches.get(0);
    }

    private List<Method> methodsNamed(Object bean, String methodName) {
        List<Method> methods = new ArrayList<>();
        for (Method method : Mockito.mockingDetails(bean).getMockCreationSettings().getTypeToMock().getMethods()) {
            if (method.getName().equals(methodName)) {
                method.setAccessible(true);
                methods.add(method);
            }
        }
        return methods;
    }

    private Object[] anyArguments(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];
        for (int index = 0; index < parameterTypes.length; index++) {
            arguments[index] = anyFor(parameterTypes[index]);
        }
        return arguments;
    }

    private Object anyFor(Class<?> type) {
        if (!type.isPrimitive()) {
            return ArgumentMatchers.any(type);
        }
        if (type == boolean.class) {
            return ArgumentMatchers.anyBoolean();
        }
        if (type == byte.class) {
            return ArgumentMatchers.anyByte();
        }
        if (type == short.class) {
            return ArgumentMatchers.anyShort();
        }
        if (type == int.class) {
            return ArgumentMatchers.anyInt();
        }
        if (type == long.class) {
            return ArgumentMatchers.anyLong();
        }
        if (type == float.class) {
            return ArgumentMatchers.anyFloat();
        }
        if (type == double.class) {
            return ArgumentMatchers.anyDouble();
        }
        if (type == char.class) {
            return ArgumentMatchers.anyChar();
        }
        throw new IllegalArgumentException("Unsupported primitive type: " + type);
    }

    private Object[] exactArguments(Method method, JsonNode argumentsNode) throws Exception {
        if (!argumentsNode.isArray()) {
            throw new IllegalArgumentException("Expected JSON array with method arguments");
        }
        if (argumentsNode.size() != method.getParameterCount()) {
            throw new IllegalArgumentException("Method '%s' expects %d arguments, but JSON contains %d."
                    .formatted(method.getName(), method.getParameterCount(), argumentsNode.size()));
        }

        Object[] arguments = new Object[argumentsNode.size()];
        Type[] parameterTypes = method.getGenericParameterTypes();
        for (int index = 0; index < argumentsNode.size(); index++) {
            arguments[index] = deserialize(argumentsNode.get(index).toString(), parameterTypes[index]);
        }
        return arguments;
    }

    private Object deserialize(String json, Type type) throws Exception {
        JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        if (javaType.getRawClass() == Void.TYPE || javaType.getRawClass() == Void.class) {
            return null;
        }
        if (javaType.getRawClass() == String.class && !looksLikeJsonStringLiteral(json)) {
            return json;
        }
        return objectMapper.readValue(json, javaType);
    }

    private boolean looksLikeJsonStringLiteral(String value) {
        return value != null && value.trim().startsWith("\"") && value.trim().endsWith("\"");
    }

    private JsonNode required(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        if (value == null) {
            throw new IllegalArgumentException("Expected JSON field: " + fieldName);
        }
        return value;
    }

    private RuntimeException createRuntimeException(String className, String message) throws Exception {
        Class<?> type = Class.forName(className);
        if (!RuntimeException.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Only RuntimeException subclasses are supported: " + className);
        }
        return (RuntimeException) type.getConstructor(String.class).newInstance(message);
    }

    private void invokeForStubbing(Object mock, Method method, Object[] arguments) throws Exception {
        method.invoke(mock, arguments);
    }

    private void invokeForVerification(Object mock, Method method, Object[] arguments) throws Exception {
        method.invoke(mock, arguments);
    }
}
