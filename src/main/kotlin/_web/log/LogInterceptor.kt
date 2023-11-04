package com.wafflestudio.seminar.spring2023._web.log

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.lang.Exception

/**
 * 1. preHandle을 수정하여 logRequest
 * 2. preHandle, afterCompletion을 수정하여 logSlowResponse
 */
@Component
class LogInterceptor(
    private val logRequest: LogRequest,
    private val logSlowResponse: AlertSlowResponse,
) : HandlerInterceptor {

    private val startTime = ThreadLocal<Long>()
    private val requestPath = ThreadLocal<String>()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        startTime.set(System.currentTimeMillis())
        requestPath.set(request.servletPath)
        logRequest.invoke(Request(request.method, request.servletPath))
        // FIXME
        return super.preHandle(request, response, handler)
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val duration = System.currentTimeMillis() - startTime.get()
        val path = requestPath.get()
        if (duration >= 3000L) {
            logSlowResponse.invoke(SlowResponse(
                method = request.method, path = path, duration = duration
            ))
        }
        // FIXME
        super.afterCompletion(request, response, handler, ex)
    }
}
