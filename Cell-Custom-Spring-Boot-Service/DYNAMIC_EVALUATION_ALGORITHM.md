# Dynamic Kyverno Policy Evaluation Algorithm

## Overview

The dynamic evaluation algorithm evaluates users against Kyverno ABAC policies by executing kyverno-cli for each user and parsing the results. Only users who pass the evaluation (no "fail" results) are added to the ACL.

## Algorithm Flow

### Step 1: Discover User Files
```
Scan: resources/physical-access-control/pip-users/*.yml
Each file contains a Kyverno userinfo definition with:
- username
- groups (e.g., employee-group, training-vde-available-group)
```

### Step 2: Execute Kyverno CLI for Each User
```bash
For each user file:
  Execute: kyverno apply <policy> \
    --resource <resource> \
    --userinfo <user> \
    --set "request.operation=<operation>,request.time.admissionTime=<time>" \
    -t --policy-report --output-format json
```

**Example Command:**
```bash
kyverno apply pip-abac-policy1.yml \
  --resource pip-resources/pip-resource-room.yml \
  --userinfo "pip-users/pip-userinfo-ben-carter.yml" \
  --set "request.operation=UPDATE,request.time.admissionTime=2025-10-20T08:30:00Z" \
  -t --policy-report --output-format json
```

### Step 3: Parse ClusterReport Output
```json
{
  "kind": "ClusterReport",
  "apiVersion": "openreports.io/v1alpha1",
  "summary": {"pass": 0, "fail": 1, "warn": 0, "error": 0, "skip": 0},
  "results": [
    {
      "policy": "abac-enroll-restriction-time-bound",
      "rule": "deny-enroll-without-training-vde",
      "result": "fail",  // <-- Check this field
      "message": "ATTENTION: The 'UPDATE' operation is only allowed..."
    }
  ]
}
```

### Step 4: Evaluate Results
```java
if (results.result != "fail") {
    // User passed evaluation
    qualifiedUsers.add(userInfo);
}
```

### Step 5: Generate ACL Entries
```
For each qualified user:
  For each operation:
    Create AclEntry:
      - Principal: <userId>
      - Action: operation (ENTER, EXIT, etc.)
      - Resource: from policy (Facility [location='production-room'])
      - Condition: evaluation details
```

## Implementation Components

### 1. KyvernoUserInfo Model
Represents a user from pip-users/ directory:
```java
- apiVersion, kind
- requestInfo.userInfo.username
- requestInfo.userInfo.groups[]
```

### 2. KyvernoClusterReport Model
Represents kyverno-cli output:
```java
- summary: {pass, fail, warn, error, skip}
- results[]: {policy, rule, result, message}
- hasFailed(): boolean
- hasPassed(): boolean
```

### 3. KyvernoCliExecutor Service
Executes kyverno commands:
```java
evaluate(policyPath, resourcePath, userInfoPath, operation, admissionTime)
  → builds command
  → executes process
  → parses JSON output
  → returns KyvernoClusterReport
```

### 4. UserEvaluationService
Orchestrates user evaluation:
```java
evaluateUsersForAccess(policyPath, resourcePath, operation, time)
  → scans pip-users/ directory
  → for each user:
      - calls KyvernoCliExecutor
      - checks if passed
      - collects qualified users
  → returns List<KyvernoUserInfo>
```

### 5. AclGenerator (Enhanced)
New method: `generateAclWithDynamicEvaluation()`
```java
- Calls UserEvaluationService for each operation
- Builds ACL entries for qualified users
- Returns formatted ACL list
```

## Usage

### Via REST API

**Dynamic Evaluation (NEW):**
```bash
# JSON output
GET http://localhost:8080/api/acl/generate/dynamic

# Formatted text
GET http://localhost:8080/api/acl/generate/dynamic/formatted
```

**Static Evaluation (Original):**
```bash
# Uses pip-info-employee-vde-trainings.yml
GET http://localhost:8080/api/acl/generate
GET http://localhost:8080/api/acl/generate/formatted
```

### Programmatically

```java
@Autowired
private PolicyToAclStrategy policyToAclStrategy;

// Dynamic evaluation
List<AclEntry> acl = policyToAclStrategy.convertPolicyToAclWithDynamicEvaluation();

// Or with custom parameters
List<AclEntry> acl = policyToAclStrategy.convertPolicyToAclWithDynamicEvaluation(
    "physical-access-control/pip-abac-policy1.yml",
    "physical-access-control/pip-resources/pip-resource-room.yml",
    "2025-10-20T08:30:00Z"
);
```

## Required Directory Structure

```
src/main/resources/
└── physical-access-control/
    ├── pip-abac-policy1.yml           # Kyverno policy
    ├── pip-resources/
    │   └── pip-resource-room.yml      # Resource definition
    └── pip-users/                      # User definitions
        ├── pip-userinfo-anya-sharma.yml
        ├── pip-userinfo-ben-carter.yml
        ├── pip-userinfo-chloe-davis.yml
        ├── pip-userinfo-david-lee.yml
        └── pip-userinfo-eve-rodriguez.yml
```

## Example User File Format

```yaml
apiVersion: v1
kind: UserInfo
requestInfo:
  roles: []
  clusterRoles: []
  userInfo:
    username: ben-carter-bc-3115
    groups:
      - employee-group
      # Note: training-vde-available-group is missing
```

## Comparison: Static vs Dynamic

| Aspect | Static (Original) | Dynamic (NEW) |
|--------|------------------|---------------|
| **Data Source** | Single YAML file (pip-info-employee-vde-trainings.yml) | Multiple files in pip-users/ directory |
| **Evaluation** | Java code filters by group membership | kyverno-cli evaluates each user |
| **Policy Logic** | Hardcoded in Java (group checks) | Evaluated by Kyverno engine |
| **Flexibility** | Limited to group-based rules | Supports full Kyverno policy features |
| **Performance** | Fast (in-memory filtering) | Slower (spawns process per user) |
| **Dependencies** | None | Requires kyverno-cli installed |
| **Accuracy** | May diverge from Kyverno behavior | 100% accurate to Kyverno policy |

## Advantages of Dynamic Evaluation

1. **Policy Fidelity**: Evaluation matches exactly what Kyverno would do
2. **Completeness**: Supports all Kyverno policy features (temporal, complex conditions, etc.)
3. **Maintainability**: No need to replicate Kyverno logic in Java
4. **Extensibility**: Works with any Kyverno policy without code changes
5. **Debugging**: Get exact failure messages from Kyverno

## Prerequisites

**Install kyverno-cli:**

Windows:
```bash
# Using scoop
scoop install kyverno

# Or download from: https://github.com/kyverno/kyverno/releases
```

Linux/Mac:
```bash
# Using Homebrew
brew install kyverno

# Or using go
go install github.com/kyverno/kyverno/cmd/kyverno@latest
```

Verify installation:
```bash
kyverno version
```

## Algorithm Complexity

- **Time Complexity**: O(n × m) where:
  - n = number of users in pip-users/ directory
  - m = number of operations in policy

- **Space Complexity**: O(n × m) for storing ACL entries

- **Process Overhead**: Each evaluation spawns a kyverno-cli process
  - For large user bases, consider batching or caching

## Error Handling

The algorithm continues processing remaining users if one fails:

```java
try {
    KyvernoClusterReport report = cliExecutor.evaluate(...);
    if (report.hasPassed()) {
        qualifiedUsers.add(userInfo);
    }
} catch (Exception e) {
    log.error("Error evaluating user: {}", userFile, e);
    // Continue with next user
}
```

## Testing

Create test with mock users and kyverno-cli:

```java
@Test
void testDynamicEvaluation() throws IOException {
    List<AclEntry> acl = policyToAclStrategy
        .convertPolicyToAclWithDynamicEvaluation();

    assertNotNull(acl);
    // Verify only users with training-vde-available-group are included
}
```

## Future Enhancements

1. **Caching**: Cache kyverno evaluation results
2. **Parallel Execution**: Evaluate multiple users concurrently
3. **Batch Mode**: Process multiple users in single kyverno call
4. **Metrics**: Track evaluation times and success rates
5. **Audit Log**: Record all evaluations for compliance
