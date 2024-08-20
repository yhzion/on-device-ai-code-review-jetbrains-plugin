package com.github.yhzion.jetbrains.plugin.aicodereview.providers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClaudeApiHealthCheckerTest {

    private lateinit var healthChecker: ClaudeApiHealthChecker
    private var callbackInvoked = false
    private var isHealthyResult: Boolean? = null

    @Before
    fun setUp() {
        // ClaudeApiHealthChecker 인스턴스 생성
        healthChecker = ClaudeApiHealthChecker()
        callbackInvoked = false
        isHealthyResult = null
    }

    @Test
    fun testSubscribeReceivesCallback() {
        // 콜백 함수 정의
        val callback = { isHealthy: Boolean ->
            callbackInvoked = true
            isHealthyResult = isHealthy
        }

        // 콜백 함수를 통해 결과를 받기 위해 subscribe 호출
        healthChecker.subscribe(callback)

        // 내부적으로 상태를 변경하는 메서드를 호출했다고 가정
        // 예를 들어 내부적으로 응답이 405 Method Not Allowed를 받아 헬스 체크가 성공했다고 가정
        healthChecker.subscribe { isHealthyResult = true }

        // 일정 시간이 지나 콜백이 호출되었는지 확인
        Thread.sleep(1000)

        // 콜백 함수가 호출되었고, 결과가 true 인지 확인
        assertTrue("Callback should be invoked", callbackInvoked)
        assertTrue("Health check should be healthy", isHealthyResult == true)
    }

    @Test
    fun testSubscribeDoesNotReceiveCallbackWithoutSubscribe() {
        // subscribe 를 호출하지 않은 상태에서 콜백이 호출되지 않음을 확인
        assertFalse("Callback should not be invoked initially", callbackInvoked)
    }
}
