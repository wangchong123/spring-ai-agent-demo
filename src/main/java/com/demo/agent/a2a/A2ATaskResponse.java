package com.demo.agent.a2a;

public record A2ATaskResponse(
        String taskId,
        String state,    // RUNNING / COMPLETED / FAILED
        String result,
        String error
) {
    public static A2ATaskResponse completed(String id, String result) {
        return new A2ATaskResponse(id, "COMPLETED", result, null);
    }
    public static A2ATaskResponse failed(String id, String error) {
        return new A2ATaskResponse(id, "FAILED", null, error);
    }
}
