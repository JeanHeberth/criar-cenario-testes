# Workflow Auto QA

## Etapas
1. VALIDATE_REQUEST  
2. VALIDATE_PROJECT_PATH  
3. DISCOVER_PROJECT  
4. CHECK_FRAMEWORK_CONSISTENCY  
5. SCAN_PROJECT  
6. ANALYZE_PROJECT  
7. CREATE_AUTOMATION_PLAN  
8. WAIT_FOR_PLAN_APPROVAL  
9. GENERATE_CODE  
10. REVIEW_CODE  
11. SAVE_GENERATED_FILES  
12. WAIT_FOR_APPLICATION_APPROVAL  
13. APPLY_FILES  
14. WAIT_FOR_EXECUTION_APPROVAL  
15. EXECUTE_VALIDATION  
16. EXECUTE_TEST  
17. ANALYZE_FAILURE_IF_NEEDED  
18. BUILD_FINAL_REPORT  
19. FINISH

## Endpoints principais
- `POST /api/auto-qa/project/validate`
- `POST /api/auto-qa/analyze`
- `POST /api/auto-qa/executions/{executionId}/generate`
- `POST /api/auto-qa/executions/{executionId}/apply`
- `POST /api/auto-qa/executions/{executionId}/execute`
- `POST /api/auto-qa/executions/{executionId}/discard`
